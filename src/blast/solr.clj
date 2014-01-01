(ns blast.solr
  (require
    [clojure.java.io :as io]
    [flux.client :as client]
    [flux.embedded :as embedded]
    [flux.http :as http]
    [flux.core :as flux]))


;; Run the following lines in your REPL ("lein repl" in the root of this project)
(comment
  (require
    '[clojure.java.io :as io]
    '[flux.client :as client]
    '[flux.embedded :as embedded]
    '[flux.http :as http]
    '[flux.core :as flux])

  ;; Connect to a running Solr server, like the one in the exmaples/ directory of the Solr distribution.
  ;; e.g. download Solr from http://lucene.apache.org/solr/, unzip it, go into the examples directory, and run
  ;; java -jar start.jar
  (def conn (http/create "http://localhost:8983/solr" :collection1))

  ;; this will hold our counter...just a convenience for assigning unique IDs.
  (def id-counter (atom 0))

  ;; equivalent to ++i. We increment, then return the incremented value. Again, we could keep this as a local var inside
  ;; the function where we load things, if we prefer.
  (defn- id "Get the next sequential ID" [] (swap! id-counter inc))

  ;; Flux also lets you start an embedded Solr server:
  ;(def core (embedded/create-core "solr-home" "path/to/solr.xml"))
  ;(def conn (embedded/create core :collection1))

  ;; now, let's load some data.
  ;; We'll read a file line-by-line and load it into Solr
  (flux/with-connection conn
                        (with-open [rdr (io/reader "sonnets.txt")]
                          (doseq [line (line-seq rdr)]
                            (println line)
                            (flux/add {:id (id) :line_t line})) ; use the *_t convention to indicate that the field should be treated as text.
                          (flux/commit)
                          (flux/query "*:*"))) ; one example query.

  ;; relies on the hardcoded "conn" var to hold the connection. Bad form in a real system. Convenient from a REPL.
  (defn q [& args] (flux/with-connection conn (apply flux/query args)))

  (q "line_t:desire")
  (q "line_t:desire" {:deftype "edismax"}) ; send query args if you want
  (q "line_t:desire" {:deftype :edismax}) ; flux  will convert keywords into strings, so use them at will.
  (q "{!edismax qf=\"line_t\"}desire") ; version with local variables


  )

