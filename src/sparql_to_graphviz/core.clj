(ns sparql-to-graphviz.core
  "Summarizes data from a SPARQL endpoint into a list of classes with their properties."
  (:require [sparql-to-graphviz.sparql :as sparql]
            [sparql-to-graphviz.spec :as spec]
            [sparql-to-graphviz.util :as util]
            [sparql-to-graphviz.prefix :as prefix]
            [sparql-to-graphviz.endpoint :refer [endpoint]]
            [clojure.spec :as s]
            [clojure.string :as string]))

(s/fdef count-instances
        :ret ::spec/non-negative-int)
(defn count-instances
  "Count distinct instances in the dataset."
  []
  (-> "count_instances.mustache"
      sparql/select-template
      first
      :count))

(s/fdef count-class-instances
        :args (s/cat :class-iri ::spec/iri)
        :ret ::spec/non-negative-int)
(defn count-class-instances
  "Count instances a `class-iri`."
  [class-iri]
  (-> "count_class_instances.mustache"
      (sparql/select-template :data {:class class-iri})
      first
      :count))

(s/fdef classes
        :ret (s/coll-of (s/keys :req [::spec/class ::spec/frequency]) :distinct true))
(defn classes
  "Get absolute IRIs of instantiated classes."
  ([min-support]
   (if (zero? min-support)
     (classes min-support nil)
     (let [instance-count (count-instances)
           min-instances (int (* instance-count (/ min-support 100)))]
       (classes min-support min-instances))))
  ([_ min-instances]
   (map (fn [{:keys [frequency]
              class-iri :class}]
          {::spec/class class-iri
           ::spec/frequency frequency})
        (sparql/select-template "classes.mustache" :data {:min-instances min-instances}))))

(s/fdef class-properties
        :args (s/cat :class-iri ::spec/iri :min-support ::spec/min-support)
        :ret (s/coll-of ::spec/iri :distinct true))
(defn class-properties
  "Get properties used with instances of `class-iri`."
  ([class-iri min-support]
   (if (zero? min-support)
     (class-properties class-iri min-support nil)
     (let [instance-count (count-class-instances class-iri)
           min-instances (int (* (/ min-support 100) instance-count))]
       (class-properties class-iri min-support min-instances))))
  ([class-iri _ min-instances]
   (map :property 
        (sparql/select-template "class_properties.mustache" :data {:class class-iri
                                                                   :min-instances min-instances}))))

(s/fdef class-property-cardinality
        :args (s/cat :class-iri ::spec/iri :property ::spec/iri)
        :ret ::spec/cardinality
        :fn #(>= (::spec/max-cardinality (:ret %)) (::spec/min-cardinality (:ret %))))
(defn class-property-cardinality
  "Get minimum and maximum cardinality of the `property` as used with instances of `class-iri`."
  [class-iri property]
  (let [data {:class class-iri
              :property property}
        [{:keys [minCardinality maxCardinality]}
         & _] (sparql/select-template "class_property_cardinality.mustache" :data data)]
    (when minCardinality
      {::spec/min-cardinality minCardinality
       ::spec/max-cardinality maxCardinality})))

(s/fdef class-property-type
        :args (s/cat :class-iri ::spec/iri :property ::spec/iri)
        :ret ::spec/property-type)
(defn class-property-type
  [class-iri property]
  (let [data {:class class-iri
              :property property}]
    (-> "class_property_type.mustache"
        (sparql/select-template :data data)
        first
        :propertyType)))

(s/fdef class-property-datatype
        :args (s/cat :class-iri ::spec/iri :property ::spec/iri)
        :ret ::spec/iri)
(defn class-property-datatype
  [class-iri property]
  (let [data {:class class-iri
              :property property}]
    (-> "class_property_datatype.mustache"
        (sparql/select-template :data data)
        first
        :datatype)))

(s/fdef class-property-ranges
        :args (s/cat :class-iri ::spec/iri :property ::spec/iri)
        :ret (s/coll-of (s/keys :req [::spec/range ::spec/frequency])))
(defn class-property-ranges
  [class-iri property]
  (let [data {:class class-iri
              :property property}]
    (map (fn [{:keys [frequency]
               range-iri :range}]
           {::spec/frequency frequency
            ::spec/range range-iri})
         (sparql/select-template "class_property_ranges.mustache" :data data))))

(s/fdef class-property-range-cardinality
        :args (s/cat :class-iri ::spec/iri :property ::spec/iri :range-iri ::spec/iri)
        :ret ::spec/cardinality)
(defn class-property-range-cardinality
  [class-iri property range-iri]
  (let [data {:class class-iri
              :property property
              :range range-iri}
        [{:keys [minCardinality maxCardinality]}
         & _] (sparql/select-template "class_property_range_cardinality.mustache" :data data)]
    (when minCardinality
      {::spec/min-cardinality minCardinality
       ::spec/max-cardinality maxCardinality})))

(s/fdef ranges-with-cardinalities
        :args (s/cat :class-iri ::spec/iri :property ::spec/iri :frequency ::spec/frequency))
(defn ranges-with-cardinalities
  [class-iri property total-frequency]
  (into []
        (for [{::spec/keys [frequency]
               r ::spec/range} (class-property-ranges class-iri property)]
          {:range r
           :cardinality (class-property-range-cardinality class-iri property r)
           :frequency (double (/ frequency total-frequency))})))

(defn empirical-schema
  [min-support]
  (for [{::spec/keys [frequency]
         class-iri ::spec/class} (classes min-support)]
    {:class class-iri
     :properties (for [property (class-properties class-iri min-support)
                       :let [property-type (class-property-type class-iri property)
                             datatype-property? (= property-type (prefix/owl "DatatypeProperty"))
                             description {:property property
                                          :type property-type}]]
                   (if datatype-property?
                     (assoc description
                            :datatype (class-property-datatype class-iri property)
                            :cardinality (class-property-cardinality class-iri property))
                     (let [ranges (ranges-with-cardinalities class-iri property frequency)]
                       (if (seq ranges)
                         (assoc description :ranges ranges)
                         description))))}))

(s/fdef schema-terms
        :ret (s/coll-of ::spec/iri))
(defn schema-terms
  "Collect all terms in `schema`."
  [schema]
  (reduce (fn [acc {class-iri :class
                    properties :properties}]
            (into acc (conj (remove nil? (mapcat (juxt :property :datatype) properties)) class-iri)))
          []
          schema))

(s/fdef namespaces
        :args (s/cat :schema-terms (s/coll-of ::spec/iri))
        :ret (s/coll-of ::spec/iri))
(defn namespaces
  "Get distinct namespaces from `schema-terms`."
  [schema-terms]
  (distinct (remove nil? (map (comp first prefix/split-local-name) schema-terms))))

(s/fdef most-frequent-namespace
        :args (s/cat :schema-terms (s/coll-of ::spec/iri))
        :ret ::spec/iri)
(defn most-frequent-namespace
  "Get the most frequent namespace from `schema-terms`."
  [schema-terms]
  (->> schema-terms
       (map (comp first prefix/split-local-name))
       (remove nil?)
       frequencies
       (sort-by (comp - second))
       (map first)
       (filter (partial not= (prefix/xsd))) ; Skip XML Schema
       first))

(s/fdef namespace-prefix-map
        :args (s/cat :schema-terms (s/coll-of ::spec/iri))
        :ret (s/coll-of (s/cat ::spec/iri string?)))
(defn namespace-prefix-map
  "Create a namespace-prefix map from `schema-terms`."
  [schema-terms]
  (let [top-namespace (most-frequent-namespace schema-terms)
        conventions (prefix/prefix-conventions)
        namespace->prefix (fn [[prefix-map index] ns-iri]
                            (cond ; Top namespace has empty prefix
                                  (= ns-iri top-namespace)
                                    [(assoc prefix-map ns-iri "") index]
                                  ; Get conventional prefix
                                  (contains? conventions ns-iri)
                                    [(assoc prefix-map ns-iri (conventions ns-iri)) index]
                                  ; Mint new prefix
                                  :else
                                    [(assoc prefix-map ns-iri (str "ns" index)) (inc index)]))
        longer (fn [a b] (> (count a) (count b)))
        init-map {(prefix/owl) "owl"}]
    (->> schema-terms
         namespaces
         (reduce namespace->prefix [init-map 0])
         first
         (map vec)
         (sort-by first longer)))) ; Longer namespaces first

(s/fdef compact-iri
        :args (s/cat :ns-prefix-map (s/map-of ::spec/iri string?) :iri ::spec/iri)
        :ret ::spec/curie)
(defn compact-iri
  "Compact an `iri` using namespace-prefix mappings from `ns-prefix-map`."
  [ns-prefix-map iri]
  (if-let [[ns-iri prefix] (first (filter (comp (partial string/starts-with? iri) first) ns-prefix-map))]
    (str prefix \: (subs iri (count ns-iri)))
    iri))

(s/fdef compact-schema-iris
        :args (s/cat :ns-prefix-map (s/map-of ::spec/iri string?) :schema seq?))
(defn compact-schema-iris
  "Compact IRIs in `schema` using `ns-prefix-map` that maps namespaces to prefixes."
  [ns-prefix-map schema]
  (let [compact-iri' (partial compact-iri ns-prefix-map)]
    (for [{class-iri :class
           :keys [properties]} schema
          :let [compact-ranges (fn [ranges]
                                 (reduce (fn [acc r] (conj acc (update r :range compact-iri')))
                                         []
                                         ranges))]]
      {:class (compact-iri' class-iri)
       :properties (for [{:keys [ranges]
                          :as property} properties]
                     (cond-> (util/update-keys property [:datatype :property :type] compact-iri')
                       ranges (update :ranges compact-ranges)))})))
