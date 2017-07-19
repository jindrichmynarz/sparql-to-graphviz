(ns sparql-to-graphviz.spec-test
  (:require [sparql-to-graphviz.spec :as spec]
            [clojure.test :refer :all]))

(deftest urn?
  (are [urn] (is (spec/urn? urn))
       "urn:x-arq:UnionGraph"
       "URN:foo:a123,456"
       "urn:foo:a123%2C456"))

(deftest valid-iri?
  (testing "Valid IRIs"
    (are [iri] (is (spec/valid-iri? iri))
         "https://ruian.linked.opendata.cz/zdroj/datová-sada/rúian"
         "http://localhost/"))
  (testing "Invalid IRIs"
    (are [iri] (is (not (spec/valid-iri? iri)))
         "http://example.com/ https://example.org"
         "bork mork")))
