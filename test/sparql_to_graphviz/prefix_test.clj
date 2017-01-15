(ns sparql-to-graphviz.prefix-test
  (:require [sparql-to-graphviz.prefix :as prefix]
            [clojure.test :refer :all]))

(deftest split-local-name
  (are [iri nspace local-name] (= (prefix/split-local-name iri) [nspace local-name])
       "http://purl.org/dc/terms/title" "http://purl.org/dc/terms/" "title"
       (prefix/owl "ObjectProperty") (prefix/owl) "ObjectProperty"))
