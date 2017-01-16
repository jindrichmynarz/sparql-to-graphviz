(ns sparql-to-graphviz.spec-test
  (:require [sparql-to-graphviz.spec :as spec]
            [clojure.test :refer :all]))

(deftest urn?
  (are [urn] (is (spec/urn? urn))
       "urn:x-arq:UnionGraph"
       "URN:foo:a123,456"
       "urn:foo:a123%2C456"))
