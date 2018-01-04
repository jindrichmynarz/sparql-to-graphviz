(ns sparql-to-graphviz.endpoint
  (:require [sparclj.core :refer [init-endpoint]]
            [mount.core :as mount :refer [defstate]]))

(defstate endpoint
  :start (init-endpoint (mount/args)))
