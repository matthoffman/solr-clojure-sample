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
    [flux.http :as http]
    [flux.core :as flux]))


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


;; Run (go) in your REPL to connect to a running Solr server, like the one in the exmaples/ directory of the Solr
;; distribution.
;; So, for example, download Solr from http://lucene.apache.org/solr/, unzip it, go into the examples directory, and run
;; java -jar start.jar
                                                    D
;; Try running any of the following in your REPL...
(comment

  ;; now, let's load some data.
  ;; We'll read a file line-by-line and load it into Solr
  (flux/with-connection (conn)
                        (with-open [rdr (io/reader "sonnets.edn")]
                          (doseq [line (line-seq rdr)]
                            (println line)
                            (flux/add (edn/read-string line)))
                          (flux/commit)
                          (flux/query "*:*"))) ; one example query.

  (q "line_t:desire")
  (q "line_t:desire" {:deftype "edismax"}) ; send query args if you want
  (q "line_t:desire" {:deftype :edismax}) ; flux  will convert keywords into strings, so use them at will.
  (q "{!edismax qf=\"line_t\"}desire") ; version with local variables. Anything that's valid in a Solr query is valid here.

  )