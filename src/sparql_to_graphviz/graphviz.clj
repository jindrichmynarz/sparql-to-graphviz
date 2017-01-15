(ns sparql-to-graphviz.graphviz
  (:require [sparql-to-graphviz.util :as util]
            [sparql-to-graphviz.spec :as spec]
            [hiccup.core :refer [html]]
            [dorothy.core :refer :all])
  (:import (java.awt GraphicsEnvironment)))

(def ^:private font-available?
  "Predicate testing what fonts are available on the given system."
  (set (.getAvailableFontFamilyNames (GraphicsEnvironment/getLocalGraphicsEnvironment))))

(def ^:private monospace-font
  (or (some font-available? ["Monaco"]) "Monospace"))

(def ^:private sans-serif-font
  (or (some font-available? ["Open Sans"]) "Sans-Serif"))

(def ^:private small-font
  {:point-size 10})

(def ^:private large-font
  {:point-size 14})

(def ^:private to-key
  (comp keyword util/sha1))

(defn- property-selector
  [property-class]
  (comp (partial = property-class) :type))

(def ^:private datatype-property?
  (property-selector "owl:DatatypeProperty"))

(def ^:private object-property?
  (property-selector "owl:ObjectProperty"))

(defn datatype-property-node
  "Format a datatype property."
  [{{::spec/keys [min-cardinality max-cardinality]} :cardinality
    :keys [datatype property]}]
  (let [datatype-cardinality (format "%s (%d..%d)" datatype min-cardinality max-cardinality)]
    [:tr [:td {:align "left"} [:font small-font property]]
         [:td {:align "right"} [:font (assoc small-font :color "dimgrey") datatype-cardinality]]]))

(defn class-node
  "Format a node for class."
  [{class-iri :class
    :keys [properties]
    :as node}]
  (let [class-key (to-key class-iri)
        datatype-properties (filter datatype-property? properties)]
    [class-key
     {:label (html (into [:table {:border 1 :cellborder 0 :port class-key}
                          [:tr [:td {:colspan 2} [:font large-font class-iri]]]
                          [:hr]]
                         (if (seq datatype-properties)
                           (map datatype-property-node datatype-properties)
                           ; Placeholder for classes with no datatype properties.
                           ; Without the placeholder, the class is not displayed.
                           [[:tr [:td {:colspan 2} " "]]])))
      :fontname monospace-font}]))

(defn edges
  "Format edges representing object properties of a class.
  `internal?` is a predicate that is true if provided with a class in the diagram."
  [internal?
   {class-iri :class
    :keys [properties]}]
  (letfn [(format-ranges [{:keys [property ranges]}]
            (for [{{::spec/keys [min-cardinality max-cardinality]} :cardinality
                   range-iri :range} (sort-by (comp - :frequency) ranges)
                  :when (internal? range-iri)]
              {range-iri [(format "%s (%d..%d)" property min-cardinality max-cardinality)]}))
          (format-label [label]
            [:tr [:td {:align "left"} [:font small-font label]]])
          (format-edge [[range-iri labels]]
            [(to-key class-iri)
             (to-key range-iri)
             {:label (html (into [:table {:border 0 :cellborder 0}]
                                 (mapv format-label labels)))
              :arrowhead "open"
              :fontname monospace-font}])]
    (->> properties
      (filter object-property?)
      (mapcat format-ranges)
      (reduce (partial merge-with concat))
      (map format-edge))))

(defn namespace-prefixes
  "Make a box with namespace prefixes used in a diagram."
  [ns-prefix-map]
  (html (into [:table {:border 1 :cellborder 0}
               [:tr [:td {:colspan 2} [:font (assoc large-font :face sans-serif-font) "Namespace prefixes"]]]
               [:hr]]
              (for [[ns-iri prefix] (sort-by second ns-prefix-map)]
                [:tr [:td {:align "right"} [:font small-font
                                             (if (pos? (count prefix)) prefix " ")]]
                     [:td {:align "left"} [:font small-font ns-iri]]]))))

(defn class-diagram
  "Make a class diagram representing empirical `schema`.
  `ns-prefix-map` is used as a legend explaining namespace prefixes."
  [schema ns-prefix-map]
  (let [internal? (into #{} (map :class schema))]
    (dot (digraph (concat [{:overlap "false"
                            :splines "true"}
                           (node-attrs {:shape :none
                                        :margin 0})]
                          (map class-node schema)
                          (mapcat (partial edges internal?) schema)
                          [(subgraph :legend [{:rank :sink}
                                              [:prefixes {:label (namespace-prefixes ns-prefix-map)
                                                          :fontname monospace-font}]])])))))
