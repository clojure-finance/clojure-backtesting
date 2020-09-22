;; Namespace: define the namespace and import of all necessary libraries 
(ns clojure-backtesting.core
     (:require [clojure.data.csv :as csv] 
               [clojure.java.io :as io] 
               [clojure.set :as set]              
               [clojure.pprint :as pprint] 
               [clj-time.core :as t]
               [clojure.string :as str]
     )) 


;; Function 1: Row-based csv->map function
(defn csv->map
  [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map keyword) 
            repeat)      
       (rest csv-data))) 
; zipmap the header and the remaining rows to vector of map. 
; each map has header as keyword and row value as value.
; the repeat is used for zipmap function to reproduce the previous vector.


;; Function 2: Column based csv->map function
(defn csv->map_col
  [csv-data]
  (zipmap
    (->> (first csv-data)
      (map keyword))
    (apply map vector (rest csv-data))))


;; Function 3: Key update function
(defn update-by-keys
  [m keys f]
  (reduce (fn [m k] (update m k f)) m keys)) 
; update values in a map (m) by applying function (f) on key 


;; Function 4: Parse the string into corresponding datatype
(defn parse-str
  [str]
  (try (Integer/parseInt str) 
       (catch Exception e (try (Float/parseFloat str)
                               (catch Exception e (if (= str "") nil str))))))
; parse numbers(including integeres and floats) from string. May return nil.
; if the string is NA, just replace by nil, otherwise it is the original string

;; Function 4.1
(defn parse-date
  [str]
  (use 'clj-time.format)
  (parse (formatter "dd/MM/YYYY") str))


;; Function 5: Read CSV data into memory
(defn slurp-csv
  [filename]
  (with-open [reader (io/reader filename)] ;; create a reader buffer to read each line
    (->> (csv/read-csv reader) ;; read the csv line by line using the buffer
         csv->map           
         doall)))
;; doall: just show the whole map, no use


;; Function 6: Read the csv file into row-based
(defn read-csv
  [filename]
  (->> (slurp-csv filename)
       (map #(update-by-keys % keywords-map parse-str)))) 
; change the data type to numbers/string/nil respectively


;; Function 7: Read csv file into memeory in column-based
(defn slurp-csv-col
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map_col         
         doall)))
; change the csv to a map with the csv->map f


;; Function 8: Slurp the csv file into col-based
(defn read-csv-col
  [filename]
  (->> (slurp-csv-col filename)
       (map #(update-by-keys % keywords-map parse-str)))) 


;; Function 9: Parse the seq like ({}{}{}) to {: [] : []}
(defn row->col
  [row-based]
    (-> (keys (first row-based))
    (zipmap (apply map vector (map vals (rest row-based))))))


;; Function 10: To filter the data
(defn data-filter
  [sec year month day]
   (->> newdata
  (filter #(and (= (:tic %) sec) (= (t/before? (:datadate %) (t/date-time year month day))true))))) 



(def csvmap (nth (slurp-csv "data-testing-merged.csv") 0))
(def mapkey (keys csvmap))
(def keywords-map (reduce conj [] mapkey)) ;; used to get a vector of all the keywords

(def data (read-csv "data-testing-merged.csv"))
(def newdata (map #(update-by-keys % [:datadate] parse-date) data))
(def aapl (data-filter "AAPL" 1982 01 01))
(def aapl-data (row->col aapl))


;; Function for initialize record variables
(defn initialize
  []
  (def pos [])
  (def trade [])
  (def tradetime []))

(defn order
  [time qty]
  (def pos (conj pos (+ (reduce + trade) qty))) 
  (def trade (conj trade qty))
  (def tradetime (conj tradetime time)))

;; Function to get the items you want
(defn get-col-data
  [item dataset]
  ((keyword item) dataset))

;; Function for moving average
(defn average 
  [lst] 
  (/ (reduce + lst) (count lst)))

(defn moving-average
  [window vec]
  (def lst (apply list vec))
  (into [] (concat (repeat (- window 1) 0) (map average (partition window 1 lst)))))

(def short-ma-5 (moving-average 5 price-data))
(def long-ma-20  (moving-average 20 price-data))

(for [x (range 21 (count price-data))]
  (if (and (> (get short-ma-5 x) (get long-ma-20 x)) (< (get short-ma-5 (- x 1)) (get long-ma-20 (- x 1)))) (order (get date-data x) 1) 
      (if (and (< (get short-ma-5 x) (get long-ma-20 x)) (> (get short-ma-5 (- x 1)) (get long-ma-20 (- x 1)))) (order (get date-data x) -1))))



