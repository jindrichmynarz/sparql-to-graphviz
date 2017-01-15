(ns sparql-to-graphviz.prefix
  (:require [clojure.java.io :as io]
            [clojure.set :refer [map-invert]]
            [cheshire.core :as json]))

; ----- Namespace prefixes -----

(defn- prefix
  "Builds a function for compact IRIs in the namespace `iri`."
  [iri]
  (partial str iri))

(def owl
  (prefix "http://www.w3.org/2002/07/owl#"))

(def xsd
  (prefix "http://www.w3.org/2001/XMLSchema#"))

; ----- Public functions -----

(defn split-local-name
  "Split `iri` into namespace and local name."
  [iri]
  (let [[_ namespace-iri local-name] (re-matches #"^(.+[#/])(.+)$" iri)]
    [namespace-iri local-name]))

(defn prefix-conventions
  "Get conventional prefixes for namespaces from Prefix.cc."
  []
  (-> "prefix_cc.json"
      io/resource
      io/reader
      json/parse-stream
      map-invert))
