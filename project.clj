(defproject sparql-to-graphviz "0.1.0-SNAPSHOT"
  :description "Generate class diagram representing empirical schema of data in a SPARQL endpoint"
  :url "http://github.com/jindrichmynarz/sparql-to-graphviz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [stencil "0.5.0"]
                 [cheshire "5.8.0"]
                 [slingshot "0.12.2"]
                 [mount "0.1.11"]
                 [hiccup "1.0.5"]
                 [dorothy "0.0.6"]
                 [org.apache.jena/jena-arq "3.6.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [sparclj "0.2.1"]]
  :main sparql-to-graphviz.cli
  :profiles {:dev {:plugins [[lein-binplus "0.4.2"]]}
             :uberjar {:aot :all
                       :uberjar-name "sparql_to_graphviz.jar"}}
  :bin {:name "sparql_to_graphviz"})
