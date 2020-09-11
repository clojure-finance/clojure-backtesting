(ns clojure-backtesting.core
  (:gen-class)
  (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.pprint :as pprint])) ;; For input-output 

(defn csv->map
  "Convert parsed CSV vectors into maps with headers as keys"
  [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map keyword)
            repeat)
       (rest csv-data)))

(defn update-by-keys
  "Update values in a map (m) by applying function (f) on keys"
  [m keys f]
  (reduce (fn [m k] (update m k f)) m keys))

(defn parse-int
  "Parse integer from string. May return nil."
  [str]
  (try (Integer/parseInt str)
       (catch Exception e nil)))

(defn slurp-csv
  "Read CSV data into memory"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map doall)))


