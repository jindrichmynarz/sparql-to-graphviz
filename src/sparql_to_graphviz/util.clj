(ns sparql-to-graphviz.util
  (:require [clojure.string :as string])
  (:import (java.security MessageDigest)))

(defn ->integer
  [s]
  (Integer/parseInt s))

(defn- exit
  "Exit with `status` and message `msg`.
  `status` 0 is OK, `status` 1 indicates error."
  [^Integer status
   ^String msg]
  {:pre [(#{0 1} status)]}
  (binding [*out* (if (zero? status) *out* *err*)]
    (println msg))
  (System/exit status))

(def die
  (partial exit 1))

(def info
  (partial exit 0))

(def join-lines
  (partial string/join \newline))

(defn sha1
  "Compute SHA1 hash from string `s`."
  [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA1") (.getBytes s))]
    ;; Stolen from <https://gist.github.com/kisom/1698245#file-sha256-clj-L19>
    (string/join (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn update-keys
  "Update keys `ks` in map `m` by applying function `f` to their values."
  [m ks f]
  (reduce (fn [m k]
            (if (contains? m k)
              (update m k f)
              m))
          m
          ks))
