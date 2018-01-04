(ns sparql-to-graphviz.cli
  (:gen-class)
  (:require [sparql-to-graphviz.core :as core]
            [sparql-to-graphviz.spec :as spec]
            [sparql-to-graphviz.endpoint :as endpoint]
            [sparql-to-graphviz.util :as util]
            [sparql-to-graphviz.graphviz :as graphviz]
            [sparclj.core :as sparql]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [slingshot.slingshot :refer [try+]]
            [mount.core :as mount]))

; ----- Private functions -----

(defn- usage
  [summary]
  (util/join-lines ["Generate empirical schema of data in a SPARQL endpoint"
                    ""
                    "Usage: sparql_to_graphviz [options]"
                    ""
                    "Options:"
                    summary]))

(defn- error-msg
  [errors]
  (util/join-lines (cons "The following errors occurred while parsing your command:\n" errors)))

(defn- validate-params
  [params]
  (when-not (s/valid? ::spec/config params)
    (util/die (str "The provided arguments are invalid.\n\n"
                   (s/explain-str ::spec/config params)))))

(defn class-diagram
  "Generate class diagram of a dataset."
  [min-support]
  (let [schema (core/empirical-schema min-support)
        ns-prefix-map (core/namespace-prefix-map (core/schema-terms schema))
        compact-schema (core/compact-schema-iris ns-prefix-map schema)]
    (graphviz/class-diagram compact-schema ns-prefix-map)))

(defn save-diagram
  "Save DOT `diagram` to `output`."
  [output diagram]
  (if (= output *out*)
    (do (.write output diagram) (flush))
    (with-open [writer (io/writer output)]
      (.write writer diagram))))

(defn- main
  [{::spec/keys [endpoint min-support output]
    :as params}]
  (validate-params params)
  (try+ (mount/start-with-args params)
        (catch [:type ::sparql/endpoint-not-found] _
          (util/die (format "SPARQL endpoint <%s> was not found." endpoint))))
  (try+ (save-diagram output (class-diagram min-support))
        (catch [:type ::util/no-classes] _
          (util/die "Cannot generate a diagram of a dataset without classes."))
        (catch [:type ::sparql/endpoint-not-found] _
          (util/die (format "SPARQL endpoint <%s> is hiding." endpoint)))))

; ----- Private vars -----

(def ^:private cli-options
  [["-e" "--endpoint ENDPOINT" "SPARQL endpoint's URL"
    :id ::sparql/url
    :validate [(partial s/valid? ::sparql/url) "The endpoint must be a valid absolute HTTP(S) URL."]]
   ["-g" "--graph GRAPH" "Restrict data to a named graph"
    :id ::spec/graph
    :validate [(partial s/valid? ::spec/graph) "The graph must be either a valid absolute IRI or URN."]]
   ["-o" "--output OUTPUT" "Path to the output file"
    :id ::spec/output
    :parse-fn io/as-file
    :default *out*
    :default-desc "STDOUT"]
   [nil "--min-support MIN_SUPPORT" "Filter classes and properties by minimum frequency in percent"
    :id ::spec/min-support
    :parse-fn util/->integer
    :validate [(partial s/valid? ::spec/min-support) "Percentage must be a number between 0 and 100."]
    :default 0]
   [nil "--sleep SLEEP" "Number of miliseconds to pause between SPARQL requests"
    :id ::sparql/sleep
    :parse-fn util/->integer
    :validate [spec/non-negative? "Pause duration must be a non-negative number."]
    :default 0]
   [nil "--max-retries MAX_RETRIES" "Number of attempts to retry a failed request"
    :id ::sparql/max-retries
    :parse-fn util/->integer
    :validate [spec/non-negative? "Number of retries must be a non-negative number."]
    :default 3]
   ["-h" "--help" "Display help information"
    :id ::spec/help?]])

; ----- Public functions -----

(defn -main
  [& args]
  (let [{{::spec/keys [help?]
          :as params} :options
         :keys [errors summary]} (parse-opts args cli-options)]
    (cond help? (util/info (usage summary))
          errors (util/die (error-msg errors))
          :else (main params))))
