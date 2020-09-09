(ns clojure-backtesting.core
  (:gen-class)
  (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
            [clojure.java.io :as io])) ;; For input-output handling

(defn load-csv
  "Loads a specified CSV file from the resources folder
  in project directory"
  [filename]
  (with-open [file (io/reader (str "resources/" filename))]
    (doall
     (csv/read-csv file))))

(defn get-keys
  "Converts the first row in a vector of vectors to
  mappable keywords:"
  [head & tail]
  (map keyword head))
