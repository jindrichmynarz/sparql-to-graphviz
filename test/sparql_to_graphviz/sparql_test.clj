(ns sparql-to-graphviz.sparql-test
  (:require [sparql-to-graphviz.sparql :as sparql]
            [sparql-to-graphviz.prefix :refer [xsd]]
            [clojure.test :refer :all]))

(deftest format-binding
  (are [datatype content result] (= (sparql/format-binding datatype content) result)
       (xsd "boolean") "true" true
       (xsd "double") "1.23" 1.23
       (xsd "float") ".1e6" 100000.0
       (xsd "integer") "5" 5
       (xsd "long") "10" 10
       (xsd "string") "foo" "foo"))
