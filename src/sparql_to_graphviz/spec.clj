(ns sparql-to-graphviz.spec
  (:require [sparql-to-graphviz.prefix :as prefix]
            [sparclj.core :as sparql]
            [clojure.spec.alpha :as s])
  (:import (java.io File Writer)
           (org.apache.jena.iri IRIFactory)))

(def curie?
  (partial re-matches #"^[^:]*:[^:]+$"))

(def non-negative?
  (complement neg?))

(def urn?
  (partial re-matches #"(?i)^urn:[a-z0-9][a-z0-9-]{0,31}:[a-z0-9()+,\-.:=@;$_!*'%/?#]+$"))

(def valid-iri?
  "Test if `iri` is valid."
  (let [iri-factory (IRIFactory/iriImplementation)]
    (fn [iri]
      (try (do (.construct iri-factory iri) true)
           (catch Exception _ false)))))

(s/def ::file (partial instance? File))

(s/def ::curie (s/and string? curie?))

(s/def ::iri (s/and string? valid-iri?))

(s/def ::non-negative-int (s/and int? non-negative?))

(s/def ::non-negative-number (s/and number? non-negative?))

(s/def ::urn (s/and string? urn?))

(s/def ::class ::iri)

(s/def ::frequency (s/and int? pos?))

(s/def ::graph (s/or :iri ::iri
                     :urn ::urn))

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

(s/def ::config (s/keys :req [::sparql/url ::output]
                        :opt [::graph ::help? ::sparql/max-retries ::sparql/sleep]))
