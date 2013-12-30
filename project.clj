(defproject clojr "0.0.1-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.codesignals/flux "0.2.0" :exclusions [[log4j] [org.slf4j/slf4j-log4j12]]]
                 [org.apache.solr/solr-solrj "4.6.0" :exclusions [[log4j] [org.slf4j/slf4j-log4j12]]]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [commons-logging "1.1.3"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}})