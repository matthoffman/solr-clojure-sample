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

(defn pq "Convenience method to send a query using the system in the #'system atom and pretty-print the results"
  [& args] (pprint (apply q args)))

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

  ;; pass in query options as a map
  (flux/with-connection (conn) (flux/query "*:*" {:sort "title_s desc"}))

  (flux/with-connection (conn) (flux/query "*:*" {:facet true :facet.field "author_s"}))

  ;; could also use keywords for things like facet.field:
  (flux/with-connection (conn) (flux/query "*:*" {:facet true :facet.field :author_s}))

  (flux/with-connection (conn) (flux/query "title_s:\"A document\"~2"))

  (flux/with-connection (conn) (flux/query "title_s:\"document A\"~2"))

  ;; conditionals supported in the default query parser:
  (flux/with-connection (conn) (flux/query "title_s:test AND author_s:matt"))

  (flux/with-connection (conn) (flux/query "title_s:test OR author_s:matt"))

  ;; they can be arbitrarily nested, of course:
  (flux/with-connection (conn) (flux/query "(title_s:test OR author_s:\"matt\") AND tags_ss:tag2"))

  ;; negations work, too:
  (flux/with-connection (conn) (flux/query "title_s:test AND -author_s:matt"))

  ;; if we had numbers, we could do range queries like so:
  (flux/with-connection (conn) (flux/query "internal_i:[* TO 5]"))

  (flux/with-connection (conn) (flux/query "internal_i:[15 TO *]" {:rows 100}))

  ;; there's also an experimental feature (yanked from another fork) to programmatically build up queries, like so:
  (flux/with-connection (conn)
                        (flux/query "*:*"
                                    (-> (c/with-filter (c/is :author_s "matt"))
                                        (c/with-facets (c/fields :author_s))
                                        (c/with-options {:page 2}))))

  ;; now, let's load some data.
  ;; We'll read a file line-by-line and load it into Solr
  (flux/with-connection (conn)
                        (flux/delete-by-query "*:*")
                        (flux/commit))

  (load-test-data)

  (q "line_t:love")                                         ;; note what ranks highly

  (q "line_t:love" {:deftype "edismax"}) ; send query args if you want

  (q "line_t:love" {:deftype :edismax}) ; flux  will convert keywords into strings, so use them at will.

  (q "line_t:love" {:facet true :facet.field "sonnet_i" :facet.limit 10 :facet.mincount 1})

  (q "{!edismax qf=\"line_t\"}desire") ; version with local variables. Anything that's valid in a Solr query is valid here.


  (flux/with-connection (conn)
                   (flux/delete-by-id [(:id to-delete)])
                   (flux/commit))
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