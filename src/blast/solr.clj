(ns blast.solr
  (require
    [clojure.java.io :as io]
    [flux.client :as client]
    [flux.embedded :as embedded]
    [flux.http :as http]
    [flux.core :as flux]))

;; See dev/user.clj for some examples you can run in your REPL


(defn create-connection [{:keys [url collection]}]
  (http/create url collection))

(defn query "execute a query in Solr.
  Assumes that the system map already contains a connection that has been started."
    [system & args] (flux/with-connection (:conn system) (apply flux/query args)))


;; This is the configuration for our application. We'll use this to connect to Solr,
;; so change it to reflect the connection parameters of your Solr server.
;; If you want to use an embedded Solr instead, change this and the "connect" function
;; above to use embedded/create-core and embedded/create...see the flux README for an example.
(def config {
              :url "http://localhost:8983/solr"
              :collection :collection1
              })
