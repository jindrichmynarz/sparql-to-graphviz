(ns sparql-to-graphviz.spec
  (:require [sparql-to-graphviz.prefix :as prefix]
            [clojure.spec :as s])
  (:import (java.io File Writer)
           (org.apache.commons.validator.routines UrlValidator)))

(def curie?
  (partial re-matches #"^[^:]*:[^:]+$"))

(def http?
  (partial re-matches #"^https?:\/\/.*$"))

(def non-negative?
  (complement neg?))

(def valid-url?
  "Test if `url` is valid."
  (let [validator (UrlValidator. UrlValidator/ALLOW_LOCAL_URLS)]
    (fn [url]
      (.isValid validator url))))

(s/def ::file (partial instance? File))

(s/def ::curie (s/and string? curie?))

(s/def ::iri valid-url?)

(s/def ::non-negative-int (s/and int? non-negative?))

(s/def ::non-negative-number (s/and number? non-negative?))

(s/def ::class ::iri)

(s/def ::endpoint (s/and string? http? ::iri))

(s/def ::frequency (s/and int? pos?))

(s/def ::graph (s/and string? ::iri))

(s/def ::help? true?)

(s/def ::min-cardinality ::non-negative-int)

(s/def ::max-cardinality ::non-negative-int)

(s/def ::cardinality (s/keys :req [::min-cardinality ::max-cardinality]))

(s/def ::max-retries (s/and int? pos?))

(s/def ::min-support (s/and ::non-negative-number (partial >= 100)))

(s/def ::output (s/or :file ::file
                      :writer (partial instance? Writer)))

(s/def ::property ::iri)

(s/def ::property-type #{(prefix/owl "DatytypeProperty") (prefix/owl "ObjectProperty")})

(s/def ::sleep ::non-negative-int)

(s/def ::config (s/keys :req [::endpoint ::output]
                        :opt [::graph ::help? ::sleep]))
