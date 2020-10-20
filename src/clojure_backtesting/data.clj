(ns clojure-backtesting.data
  (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
            [clojure.java.io :as io]
            [clojure.set :as set]      ;;
            [clj-time.core :as t]
            [clojure.pprint :as pprint] )  ;; For input-output handling
     )

;;This file is to construct the basic data structure of the backtesting

(def data-set (atom [])) ;;this should be the main dataset(to be changed by the user)

(defn test_data
  []
  (def f0 1))

(defn csv->map
  "Convert parsed CSV vectors into maps with headers as keys, by row"
  [csv-data]
  (map zipmap ;; make the first row as headers and the following rows as values in a map structure e.g. {:tic AAPL}
       (->> (first csv-data) ;; take the first row of the csv-data
            (map keyword) ;; make the header be the "key" in the map
            repeat)      ;; repeat the process for all the headers
       (rest csv-data))) ;; use the rest rows as values of the map

(defn csv->map_col
  "Convert parsed CSV vectors into maps with headers as keys, by column"
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

(defn parse-float
  "Parse float from string. May return nil"
  [str]
  (try (Float/parseFloat str)
       (catch Exception e nil)))

(defn parse-date
  "Parse datetime object from string"
  [str]
    (f/parse (f/formatter "YYYY-MM-dd") str))

(defn read-csv-row
  "Read CSV data into memory by row"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map
         doall)))

; Returns a map, e.g. {:col1 [1 3], :col2 [2 4]}
(defn read-csv-col
  "Read CSV data into memory by column"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map_col
         doall)))

;; Here, I want to define a function that can convert between the column based
;; dataset and the row based dataset
(defn row->col
  [row-based]
  "This function can parse the seq like ({}{}{}) to {: [] : []}"
    (-> (keys (first row-based))
    (zipmap (apply map vector (map vals (rest row-based))))))

;; filter the data by security and date
(defn data-filter
  ([sec data]
    (->> data
    (filter #(= (:tic %) sec))))
  ([sec year month day data]
   (->> data
  (filter #(and (= (:tic %) sec) (= (t/before? (:datadate %) (t/date-time year month day))true))))))

(defn count-days
  [row-data]
  (count row-data))

(defn average
 "This function returns the average value of a vector"
  [vec]
  (/ (reduce + vec) (count vec)))

(defn moving-average
 "This function returns the moving average of len(window) days. The first len(window) days are recorded as 0"
  [window vec]
  (concat (repeat (- window 1) 0) (map average (partition window 1 vec))))

(defn left-join
  "When passed 2 rels, returns the rel corresponding to the natural
  left-join. When passed an additional keymap, joins on the corresponding
  keys."
  ([xrel yrel]
   (if (and (seq xrel) (seq yrel))
     (let [ks (set/intersection (set (keys (first xrel)))
                                (set (keys (first yrel))))
           idx (set/index yrel ks)]
       (reduce (fn [ret x]
                 (if-let [found (idx (select-keys x ks))]
                   (reduce #(conj %1 (merge %2 x)) ret found)
                   (conj ret x)))
               #{} xrel))
     xrel))
  ([xrel yrel km]
   (let [idx (set/index yrel (vals km))]
     (reduce (fn [ret x]
               (if-let [found (idx (set/rename-keys (select-keys x (keys km)) km))]
                 (reduce #(conj %1 (merge %2 x)) ret found)
                 (conj ret x)))
             #{} xrel))))

; (def file1 "./resources/CRSP-extract_test.csv")
; (def file2 "./resources/Compustat-extract_test.csv")

;;file 1 and 2 directories
(comment
  (def file1 "./resources/CRSP-extract.csv")
  (def file2 "./resources/Compustat-extract.csv")

  (def c (first (read-csv-row file1)))
  (def d (first (read-csv-row file2))))
