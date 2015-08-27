(ns blast.solr
  (require
    [flux.http :as http]
    [flux.cloud :as cloud]
    [flux.core :as flux]))

;; See dev/user.clj for some examples you can run in your REPL


(defn create-connection [{:keys [zk-connect url collection]}]
  (print zk-connect url collection)
  (if zk-connect
    (if collection
      (cloud/create zk-connect collection)
      (cloud/create zk-connect))
    (http/create url collection)))

(defn query "execute a query in Solr.
  Assumes that the system map already contains a connection that has been started."
    [system & args] (flux/with-connection (:conn system) (apply flux/query args)))


;; This is the configuration for our application. We'll use this to connect to Solr,
;; so change it to reflect the connection parameters of your Solr server.
;; If you want to use an embedded Solr instead, change this and the "connect" function
;; above to use embedded/create-core and embedded/create...see the flux README for an example.
(def config { :zk-connect "localhost:9983"
              :url "http://localhost:8983/solr"
              :collection "flux-tests"
              })
