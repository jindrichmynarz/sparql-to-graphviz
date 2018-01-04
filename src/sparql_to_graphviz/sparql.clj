(ns sparql-to-graphviz.sparql
  (:require [sparql-to-graphviz.endpoint :refer [endpoint]]
            [sparclj.core :as sparql]
            [stencil.core :refer [render-string]]
            [clojure.java.io :as io]))

; ----- Private functions -----

(defn- render-template
  "Render Mustache `template` file using `data`."
  [template & {:keys [data]}]
  {:pre [(io/resource template)]}
  (let [{:keys [graph]} endpoint]
    (render-string (slurp (io/resource template))
                   (cond-> (or data {})
                     graph (assoc :graph graph)))))

; ----- Public functions -----

(defn ask-template
  "Execute SPARQL ASK query rendered from Mustache `template` file using `data`."
  [template & {:keys [data]}]
  (let [query (render-template template :data data)]
    (sparql/ask-query endpoint query)))

(defn select-template
  "Execute SPARQL SELECT query rendered from Mustache `template` file using `data`."
  [template & {:keys [data]}]
  (let [query (render-template template :data data)]
    (println query)
    (sparql/select-query endpoint query)))
