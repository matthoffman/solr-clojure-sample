(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application.

  The system, start, stop, etc. convention is from Stuart Sierra:
  http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded"
  (:require
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.edn :as edn]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [blast.solr :as solr]
    [flux.client :as client]
    [flux.embedded :as embedded]
    [flux.cloud :as cloud]
    [flux.http :as http]
    [flux.core :as flux]
    [flux.collections :as coll]
    [flux.criteria :as c]))


(def system
  "A Var containing an object representing the application under
  development."
  (atom {}))

(defn init
  "Creates and initializes the system under development in the Var
  #'system.
  We initialize the system to the value of the solr/config map."
  []
  (reset! system solr/config))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (swap! system assoc :conn (solr/create-connection @system)))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (when-let [conn (:conn @system)]
    (swap! system assoc :conn (.shutdown conn))))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn conn "Get the connection from the system atom" [] (:conn @system))

(defn q "Convenience method to send a query using the system in the #'system atom"
    [& args] (apply solr/query @system args))

(defn wipe-test-data []
  (flux/with-connection (conn)
                        (flux/delete-by-query "*:*")
                        (flux/commit)))

(defn random-uuid []
  (.toString (java.util.UUID/randomUUID)))


(defn load-test-data
  ([] (load-test-data "sonnets.edn"))
  ([name]
   (flux/with-connection (conn)
                         (with-open [rdr (io/reader name)]
                           (doseq [line (line-seq rdr)]
                             (println line)
                             (flux/add (edn/read-string line)))
                           (flux/commit)))))

(defn commit [] (flux/with-connection (conn) (flux/commit)))

;; Try running any of the following in your REPL...
(comment

  ;; now, let's load some data.
  ;; We'll read a file line-by-line and load it into Solr

  (q "line_t:desire")
  (q "line_t:desire" {:deftype "edismax"}) ; send query args if you want
  (q "line_t:desire" {:deftype :edismax}) ; flux  will convert keywords into strings, so use them at will.
  (q "{!edismax qf=\"line_t\"}desire") ; version with local variables. Anything that's valid in a Solr query is valid here.

  ;; These are mainly pulled straight from Flux's unit tests:

  (flux/with-connection (conn)
                        (flux/add {:id         (random-uuid)
                                   :title_s    "A test document"
                                   :created_tdt (java.util.Date.)
                                   :author_s   "matt"
                                   :tags_ss    ["tag1" "tag2" "tag3"]})
                        (flux/commit))

  (flux/with-connection (conn)
                        (flux/query "*:*"))

  (flux/with-connection (conn)
                        (flux/add [{:id         (random-uuid)
                                    :title_s    "A second document"
                                    :created_tdt (java.util.Date.)
                                    :tags_ss    ["tag2" "tag4"]}
                                   {:id         (random-uuid)
                                    :title_s    "And a third document"
                                    :created_tdt (java.util.Date.)
                                    :tags_ss    ["tag3"]}
                                   {:id         (random-uuid)
                                    :title_s    "tagless"
                                    :created_tdt (java.util.Date.)}])
                        (flux/commit))

  (flux/with-connection (conn) (flux/query "*:*" {:sort "title_s desc"}))

  (flux/with-connection (conn) (flux/query "title_t:\"last least\"~2"))

  (flux/with-connection (conn) (flux/query "title_t:\"after third\"~2"))


  (flux/query "title_t:black AND author_s:\"Glen Cook\"")
  (flux/query "title_t:black OR author_s:\"Glen Cook\"")

  (flux/with-connection (conn) (flux/query "(title_t:black OR author_s:\"Glen Cook\") AND available_b:true"))

  (flux/with-connection (conn) (flux/query "title_t:weapons AND -title_t:bond AND available_b:true"))

  (flux/query "internal_i:[* TO 5]")
  (flux/with-connection (conn) (flux/query "internal_i:[15 TO *]" {:rows 100}))


  (flux/with-connection (conn)
                   (flux/delete-by-id [(:id to-delete)])
                   (flux/commit))


  (flux/query "*:*"
               (-> (c/with-filter (c/is :category "car/BMW"))
                   (c/with-facets (c/fields :build_year))
                   (c/with-options {:page 2})))
  )


(comment
  (wipe-test-data)
  (load-test-data)
  (flux/with-connection (conn) (flux/query "*:*"))

  (pprint (flux/with-connection (conn) (flux/query "line_t:art" {:facet :true :facet.field "sonnet_i" :facet_counts 5})))

  (flux/with-connection (conn) (flux/query "*:*" {:sort "title_s desc"}))
  (pprint (flux/with-connection (conn) (flux/query "line_t:\"lovely beauteous\"~5" {:facet :true :facet.field "sonnet_i"})))
  (pprint (flux/with-connection (conn) (flux/query "line_t:\"beauteous lovely\"~5" {:facet :true :facet.field "sonnet_i"})))
  (flux/with-connection (conn) (flux/query "*:*" (c/with-facets (c/fields :sonnet_i)) ))


  (pprint (flux/with-connection (conn) (flux/query "line_t:love" {:facet :true :facet.field "sonnet_i"})))
  (pprint (flux/with-connection (conn) (flux/query "line_t:love" {:facet :true :facet.field ["sonnet_i"]} )))
  (pprint (flux/with-connection (conn) (flux/query "line_t:love" {:facet :true :facet.field ["sonnet_i" "line_num_i"]} )))
  )