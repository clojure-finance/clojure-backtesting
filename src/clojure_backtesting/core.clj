(ns clojure-backtesting.core
  (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
            [clojure.java.io :as io] 
            [clojure.set :as set]      ;;    
            [clojure.pprint :as pprint] )  ;; For input-output handling
     ) 

(defn csv->map
  "Convert parsed CSV vectors into maps with headers as keys"
  [csv-data]
  (map zipmap ;; make the first row as headers and the following rows as values in a map structure e.g. {:tic AAPL} 
       (->> (first csv-data) ;; take the first row of the csv-data
            (map keyword) ;; make the header be the "key" in the map 
            repeat)      ;; repeat the process for all the headers
       (rest csv-data))) ;; use the rest rows as values of the map

(defn csv->map_col
  [csv-data]
  (zipmap
    (->> (first csv-data)
      (map keyword))
    (apply map vector (rest csv-data))))

(defn update-by-keys
  "Update values in a map (m) by applying function (f) on keys"
  [m keys f]
  (reduce (fn [m k] (update m k f)) m keys))  

(defn parse-int
  "Parse integer from string. May return nil."
  [str]
  (try (Integer/parseInt str) 
       (catch Exception e nil)))

(defn read-csv-row
  "Read CSV data into memory"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map           ;; change the csv to a map with the csv->map fn
         doall)))

(defn read-csv-col
  "Read CSV data into memory"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map_col          ;; change the csv to a map with the csv->map fn
         doall)))

;;Here, I want to define a function that can convert between the column based
;;dataset and the row based data set
(defn row->col
  [row-based]
  "This function can parse the seq like ({}{}{}) to {: [] : []}"
    (-> (keys (first row-based))
    (zipmap (apply map vector (map vals (rest row-based))))))


;;file 1 and 2 address store for testing purpose
;;(def file1 "../resources/CRSP-extract.csv")

;;(def file2 "../../resources/Compustat-extract.csv")

;;(def a (slurp-csv file1))
;;(def b (slurp-csv file2))

