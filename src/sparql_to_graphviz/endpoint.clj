(ns sparql-to-graphviz.endpoint
  (:require [sparql-to-graphviz.util :as util]
            [sparql-to-graphviz.spec :as spec]
            [mount.core :as mount :refer [defstate]]
            [clj-http.client :as client]
            [clojure.string :as string]
            [slingshot.slingshot :refer [try+ throw+]]))

(defn init-endpoint
  "Ping endpoint to test if it is up."
  [{::spec/keys [endpoint graph min-support sleep]}]
  (try+ (client/head endpoint {:throw-entire-message? true})
        {:graph graph
         :min-support min-support
         :sleep sleep
         :sparql-endpoint endpoint}
        (catch [:status 404] _
          (throw+ {:type ::util/endpoint-not-found}))))

(defstate endpoint
  :start (init-endpoint (mount/args)))
