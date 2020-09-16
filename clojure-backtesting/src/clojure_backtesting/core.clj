 (ns clojure-backtesting.core
     (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
               [clojure.java.io :as io]  ;; For input-output handling
     [clojure.set :as set]                ;;    
     [clojure.pprint :as pprint] 
    [clj-time.core :as t]
     )) 

(defn csv->map
  "Convert parsed CSV vectors into maps with headers as keys"
  [csv-data]
  (map zipmap ;; make the first row as headers and the following rows as values in a map structure e.g. {:tic AAPL} 
       (->> (first csv-data) ;; take the first row of the csv-data
            (map keyword) ;; make the header be the "key" in the map 
            repeat)      ;; repeat the process for all the headers
       (rest csv-data))) ;; use the rest rows as values of the map

(defn update-by-keys
  "Update values in a map (m) by applying function (f) on keys"
  [m keys f]
  (reduce (fn [m k] (update m k f)) m keys))  

(defn parse-num
  "Parse numbers(including integeres and floats) from string. May return nil."
  [str]
  (try (Integer/parseInt str) 
       (catch Exception e (try (Float/parseFloat str)
                               (catch Exception e (if (= str "") nil str))))));;if the string is NA, just replace by nil, otherwise it is the original string

(defn slurp-csv
  "Read CSV data into memory"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map           ;; change the csv to a map with the csv->map fn
         doall)))

(def csvmap (nth (slurp-csv "data-testing-merged.csv") 0))
(def mapkey (keys csvmap))
(def keywords-map (reduce conj [] mapkey)) ;; used to get a vector of all the keywords

(defn read-csv
  [filename]
  (->> (slurp-csv filename)
       (map #(update-by-keys % keywords-map parse-num)))) ;; change the data type to numbers/string/nil respectively


(defn data-filter
  [security]
  (->> data
  (filter #(= (:tic %) security))));;filter by security ticker

(def data (read-csv "data-testing-merged.csv"))
(def newdata (data-filter "AAPL")) ;; filter out all AAPL data for now

(defn calc-ma
  [window input_data]
  (->> input_data
       ))

;; Below are the code for reference only


(defn average [lst] (/ (reduce + lst) (count lst)))

(defn moving-average [window lst] (map average (partition window 1 lst)))

(->> flights
     (map #(assoc % :speed (/ (:distance %) (/ (:air_time %) 60))
                  :delay (+ (:arr_delay %) (:dep_delay %))))
     (take 6)
     pprint/print-table)

(defn partialsums [start lst]
  (lazy-seq
    (if-let [lst (seq lst)] 
          (cons start (partialsums (+ start (first lst)) (rest lst)))
          (list start))))

(defn sliding-window-moving-average [window lst]
  (map #(/ % window)
       (let [start   (apply + (take window lst))
             diffseq (map - (drop window lst) lst)]
         (partialsums start diffseq))))


