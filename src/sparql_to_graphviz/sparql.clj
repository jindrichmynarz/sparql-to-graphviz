(ns sparql-to-graphviz.sparql
  (:require [sparql-to-graphviz.spec :as spec]
            [sparql-to-graphviz.endpoint :refer [endpoint]]
            [sparql-to-graphviz.util :as util]
            [sparql-to-graphviz.prefix :as prefix]
            [sparql-to-graphviz.xml-schema :as xsd]
            [clj-http.client :as client]
            [stencil.core :refer [render-string]]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.java.io :as io]
            [clojure.string :as string]))

; ----- Private functions -----

(defn xml-schema->data-type
  "Coerce a XML Schema `data-type`."
  [^String data-type]
  (if (string/starts-with? data-type (prefix/xsd))
    (keyword "sparql-to-graphviz.xml-schema" (string/replace data-type (prefix/xsd) ""))
    data-type))

(defmulti format-binding
  "Format a SPARQL result binding"
  (fn [data-type _] (xml-schema->data-type data-type)))

(defmethod format-binding ::xsd/boolean
  [_ content]
  (Boolean/parseBoolean content))

(defmethod format-binding ::xsd/double
  [_ content]
  (Double/parseDouble content))

(defmethod format-binding ::xsd/float
  [_ content]
  (Float/parseFloat content))

(defmethod format-binding ::xsd/integer
  [_ content]
  (util/->integer content))

(defmethod format-binding ::xsd/long
  [_ content]
  (Long/parseLong content))

(defmethod format-binding :default
  [_ content]
  content)

(defn- get-binding
  "Get binding from `result`."
  [result]
  (let [{{:keys [datatype]} :attrs
         [content & _] :content
         :keys [tag]} (zip-xml/xml1-> result zip/down zip/node)]
    (if (and (= tag :literal) datatype)
      (format-binding datatype content)
      content)))

(def ^:private variable-binding-pair
  "Returns a pair of variable name and its binding."
  (juxt (comp keyword (zip-xml/attr :name)) get-binding))

(defn- execute-query
  "Execute SPARQL `query`."
  [query]
  (let [{:keys [sleep sparql-endpoint]} endpoint
        params {:query-params {"query" query}
                :throw-entire-message? true}]
    (when-not (zero? sleep) (Thread/sleep sleep)) 
    (try+ (:body (client/get sparql-endpoint params))
          (catch [:status 404] _
            (throw+ {:type ::util/endpoint-not-found})))))

; ----- Public functions -----

(defn select-query
  "Execute SPARQL SELECT query in `sparql-string`
  Returns an empty sequence when the query has no results."
  [sparql-string]
  (doall (for [result (-> sparql-string
                          execute-query
                          xml/parse-str
                          :content
                          second
                          :content)
               :let [zipper (zip/xml-zip result)]]
           (->> (zip-xml/xml-> zipper :binding variable-binding-pair)
                (partition 2)
                (map vec)
                (into {})))))

(defn select-template
  "Execute SPARQL SELECT query rendered from Mustache `template` file using `data`."
  [template & {:keys [data]}]
  {:pre [(io/resource template)]}
  (let [{:keys [graph]} endpoint
        query (render-string (slurp (io/resource template))
                             (cond-> (or data {})
                               graph (assoc :graph graph)))]
    (select-query query)))
