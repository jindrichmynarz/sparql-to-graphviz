(defproject sparql-to-graphviz "0.1.0-SNAPSHOT"
  :description "Generate class diagram representing empirical schema of data in a SPARQL endpoint"
  :url "http://github.com/jindrichmynarz/sparql-to-graphviz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/data.xml "0.0.8"]
                 [stencil "0.5.0"]
                 [clj-http "3.4.1"]
                 [cheshire "5.7.0"]
                 [slingshot "0.12.2"]
                 [mount "0.1.11"]
                 [commons-validator/commons-validator "1.5.1"]
                 [hiccup "1.0.5"]
                 [dorothy "0.0.6"]
                 [org.apache.jena/jena-arq "3.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.24"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main sparql-to-graphviz.cli
  :profiles {:dev {:plugins [[lein-binplus "0.4.2"]]}
             :uberjar {:aot :all
                       :uberjar-name "sparql_to_graphviz.jar"}}
  :bin {:name "sparql_to_graphviz"})
