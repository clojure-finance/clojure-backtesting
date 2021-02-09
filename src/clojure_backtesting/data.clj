(ns clojure-backtesting.data
  (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
            [clojure.java.io :as io]
            [clojure.set :as set]      ;;
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.pprint :as pprint] )  ;; For input-output handling
     )

;; This file is to construct the basic data structure for backtesting 

(def data-set (atom [])) ;; main dataset (to be changed by the user)

(defn csv->map
  "Convert parsed CSV vectors into maps with headers as keys, by row"
  [csv-data]
  (map zipmap ;; make the first row as headers and the following rows as values in a map structure e.g. {:tic AAPL} 
       (->> (first csv-data) ;; take the first row of the csv-data
            (map keyword) ;; make the header be the "key" in the map 
            repeat)      ;; repeat the process for all the headers
       (rest csv-data))) ;; use the rest rows as values of the map

(defn- csv->map-col
  "Convert parsed CSV vectors into maps with headers as keys, by column"
  [csv-data]
  (zipmap
   (->> (first csv-data)
        (map keyword))
   (apply map vector (rest csv-data))))

(defn update-by-keys
  "Update values in a map by applying function (f) on keys"
  [map keys f]
  (reduce (fn [map k] (update map k f)) map keys))

(defn parse-int
  "Parse integer from string. May return nil."
  [str]
  (try (Integer/parseInt str)
       (catch Exception e nil)))

; Returns a list of maps, e.g. ({:col1 1, :col2 2} {:col1 3, :col2 4})
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
         csv->map-col
         doall)))

;; Here, I want to define a function that can convert between the column based
;; dataset and the row based dataset
(defn row->col
  "This function can parse the seq like ({}{}{}) to {: [] : []}"
  [row-based]
    (-> (keys (first row-based))
    (zipmap (apply map vector (map vals (rest row-based))))))

;; filter the data by security and date
; (defn data-filter
;   ([sec data]
;     (->> data
;     (filter #(= (:tic %) sec))))
;   ([sec year month day data]
;    (->> data
;   (filter #(and (= (:tic %) sec) (= (t/before? (:datadate %) (t/date-time year month day))true))))))

(defn- count-days
  [row-data]
  (count row-data))

(defn average
 "This function returns the average value of a vector."
  [vec]
  (/ (reduce + vec) (count vec)))

;; (defn- moving-average
;;  "This function returns the moving average of len(window) days. 
;;  The first len(window) days are recorded as 0."
;;   [window vec]
;;   (concat (repeat (- window 1) 0) (map average (partition window 1 vec))))

(defn left-join
  "When passed 2 rels, returns the rel corresponding to the natural
  left-join. When passed an additional keymap, joins on the corresponding
  keys. (Deprecated: should use join instead)"
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

;; helper function, natural logarithm
(defn log-10 [n]
  (/ (Math/log n) (Math/log 10)))

;; extract: sorted by ticker, then by date
;; for each security:
;; add col 'cum-ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum-ret)
(defn add-aprc 
  "This function adds the adjusted price column to the dataset (CRSP-extract)."
  [data]
  ; get price on 1st day
  (def initial-price 0)
  (def cum-ret 0)
  (def curr-ticker "DEFAULT")
 ; traverse row by row in dataset
  (map (fn [line]
        (let [;line-new (select-keys line [:date :TICKER :PRC :RET])
              price (Double/parseDouble (get line :PRC))
              ret (Double/parseDouble (get line :RET))
              ticker (get line :TICKER)]
          (if (not= curr-ticker ticker)
              (do
                ;(println ticker)
                (def curr-ticker ticker)
                (def initial-price price)
                (def cum-ret 0)
              )
          )
          ;(def log-ret (Math/log (+ 1 ret))) ; natural log
          (def log-ret (log-10 (+ 1 ret))) ; log base 10
          (def cum-ret (+ cum-ret log-ret))
          (def aprc (* initial-price (Math/pow Math/E cum-ret)))
          (assoc line :INIT-PRICE initial-price :APRC aprc :LOG-RET log-ret :CUM-RET cum-ret)
           ;testing
          ; (swap! data-set-adj conj (assoc line-new "APRC" aprc "LOG-RET" log-ret "CUM-RET" cum-ret))
        )
      )
    data
  )
)

;; extract: sorted by date, then by ticker
;; for each security:
;; add col 'cum-ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum-ret)
(defn add-aprc-by-date
  "This function adds the adjusted price column to the dataset (data-CRSP-sorted-cleaned)."
  [data]
  ; get price on 1st day for each ticker
  (def initial-price (atom {}))
  ; record cumulative return for each ticker
  (def cum-ret (atom {}))
 ; traverse row by row in dataset
  (map (fn [line]
        (let [date (get line :date)
              price (Double/parseDouble (get line :PRC))
              ret (Double/parseDouble (get line :RET))
              ticker (get line :TICKER)]
          ;; check whether the initial-price map already has the ticker
          (if-not (contains? (deref initial-price) ticker)
            (do ;; ticker appears the first time 
              (swap! initial-price (fn [ticker-map] (conj ticker-map [ticker {:price price}])))
              (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret ret}])))
            )
            ;; ticker does not appear the first time
            (let [prev-cumret (get-in (deref cum-ret) [ticker :cumret])
                  log-ret (log-10 (+ 1 ret)) ; log base 10 
                 ] 
              (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret (+ log-ret prev-cumret)}])))
            )
          )
          
          (def ticker-initial-price (get-in (deref initial-price) [ticker :price]))
          (def curr-cumret (get-in (deref cum-ret) [ticker :cumret]))
          (def aprc (* ticker-initial-price (Math/pow Math/E curr-cumret)))

          (assoc line :INIT-PRICE ticker-initial-price :APRC aprc :CUM-RET curr-cumret)
        )
      )
    data
  )
)