(ns clojure-backtesting.data-management
  (:require [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.core.matrix.stats :as stat] ;; For the standard deviation formula
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer :all])
  (:import [java.util PriorityQueue]))

;; cache management
(defn- cache-pop
  "reset information of the oldest date"
  []
  (let [date (.poll cache-queue)]
    (reset! (nth (get data-cache date) 0) nil)
    (reset! (nth (get data-cache date) 1) nil))
  )

(defn- cache-add-info
  [date info]
  (while (>= (.size cache-queue) CACHE-SIZE) (cache-pop))
  (.add cache-queue date)
  (reset! (nth (get data-cache date) 0) info)
  )

(defn- cache-add-map
  [date info]
  ;; (when (>= (.size data-cache) CACHE-SIZE) (cache-pop))
  (reset! (nth (get data-cache date) 1) info))

(defn- last-quar
  "Returns the last quarter date of a given row."
  [row]
  (let [date (get row :date) tic (get row TICKER-KEY)]
    (let [[year month day] (map parse-int (str/split date #"-"))]
      {:tic tic :datadate (cond
                            (or (and (= month 12) (= day 31)) (= month 1) (= month 2) (and (= month 3) (<= day 30))) (str (- year 1) "-" 12 "-" 31)
                            (or (and (= month 3) (= day 31)) (= month 4) (= month 5) (and (= month 6) (<= day 29))) (str year "-3-" 31)
                            (or (and (= month 6) (= day 30)) (= month 7) (= month 8) (and (= month 9) (<= day 30))) (str year "-6-" 30)
                            (or (and (= month 9) (= day 31)) (= month 10) (= month 11) (and (= month 12) (<= day 30))) (str year "-" 9 "-" 30))})))

(defn- get-info-by-date
  "Get the full tics info.\n
   Returns a vector of maps."
  ;; to do join the compustat
  [date]
  (if-let [file (get data-files date)]
    (if-let [ret (deref (nth (get data-cache date) 0))]
      ;; cache hit
      ret
      ;; cache miss
      (let [data (line-seq (io/reader file))
            data (map read-string data)]
        (cache-add-info date (map zipmap (repeat headers) data))))
    nil))

(defn- get-info-map-by-date
  [date]
  (if-let [ret (deref (nth (get data-cache date) 1))]
    ;; cache hit
    ret
    ;; cache miss
    (if-let [info (get-info-by-date date)]
      (cache-add-map date (zipmap (map TICKER-KEY info) info))
      nil)))

(defn get-info
  "Returns the whole information for the all the tics today.\n
   A sequence of maps."
  []
  ;; (if-let [info (deref tics-today)]
  ;;   info
  ;;   (let []
  ;;     (reset! tics-today (get-info-by-date (get-date)))))
  (get-info-by-date (get-date))
  )

(defn get-info-map
  "Returns the whole information for the all the tics today.\n
   A map of tic:info."
  [& [info]]

  (if info
    (zipmap (map TICKER-KEY info) info)
    ;; (if-let [tmp (deref tics-map-today)]
    ;;   tmp
    ;;   (reset! tics-map-today (zipmap (map TICKER-KEY (get-info)) (get-info))))
    (get-info-map-by-date (get-date))
    ))

;; (defn get-info-tomorrow
;;   "Returns the whole information for the all the tics today.\n
;;    A sequence of maps."
;;   []
;;   (if-let [info (deref tics-tomorrow)]
;;     info
;;     (let [info (get-info-by-date (get-next-date))]
;;       (reset! tics-tomorrow info)
;;       info))
;;   )

(defn get-tic-info
  "Returns the information for the specified tic today.\n
   A map if any, otherwise nil."
  ([tic]
  ;; (doseq [row (get-info)]
  ;;   (if (= tic (TICKER-KEY row))
  ;;     row))
  ;; ;; method 1
  ;; (loop [info (or info (get-info))]
  ;;   (let [row (first info)
  ;;         info (rest info)]
  ;;     (if (= row nil)
  ;;       nil
  ;;       (if (= tic (TICKER-KEY row))
  ;;         row
  ;;         (recur info)))))
  ;; method 2
   (get (get-info-map) tic))
  ([date tic]
  ;;  (get (get-info-map (get-info-by-date date)) tic)
   (get (get-info-map-by-date date) tic)
   ))

(defn get-tic-price
  "Returns the price of a given ticker today, otherwise nil."
  ([tic]
   (:PRC (get-tic-info tic)))
  ([date tic]
   (:PRC (get-tic-info date tic))))

(defn get-tic-by-key
  "Returns the value of the key of a given ticker today, otherwise nil."
  ([tic key]
   (get (get-tic-info tic) key))
  ([date tic key]
   (get (get-tic-info date tic) key)))

(defn get-prev-n-days
  "Returns a sequence of sequence of maps that contains data of the previous n days (not including today).\n
   Date in descending order, ie from the most recent to the oldest.\n
   If no n, return a lazy sequence of all prev days.
   "
  ;n  number of counting ahead
  ([]
   (let [date (get-date)
         dates (map first (rsubseq data-files < date))]
     (map get-info-by-date dates)))
  ([n]
   (let [date (get-date)
         dates (take n (map first (rsubseq data-files < date)))]
     (map get-info-by-date dates))))

(defn get-tic-prev-n-days
  "This function returns a sequence of vector of the previous n records of a specific ticker (not including today).\n
   Date in descending order, ie from the most recent to the oldest.\n
   Note that the returned length may be smaller than n, if the ticker is missing on some days.\n
   @tic: name of the stock\n
   @n: number of counting ahead"
  [tic n]
  (let []
    (loop [res (transient []) data (get-prev-n-days n)]
      (if (or (= (count data) 0) (>= (count res) n))
        (persistent! res)
        (let [curr (first data)
              data (rest data)]
          ;; (doseq [row curr]
          ;;   (if (= tic (get row TICKER-KEY))
          ;;     (recur (conj res row) data)))
          (let [
                index (.indexOf (mapv TICKER-KEY curr) tic)
                ;; row (loop [daily-data curr]
                ;;       (if (= 0 (count daily-data))
                ;;         nil
                ;;         (let [row (first daily-data)
                ;;               remain (rest daily-data)]
                ;;           (if (= tic (TICKER-KEY row))
                ;;             row
                ;;             (recur remain)))))
                ]
            (if (not= index -1)
            ;; (if (not= row nil)
              (recur (conj! res (nth curr index)) data)
              ;; (recur (conj res row) data)
              (recur res data))))))
    )
  )

;; ================ Deprecated =================

;; (defn- get-set
;;     "return a set of maps of tickers and datadate" ;;{:tic "AAPL", :datadate "1981/3/31"}
;;     [file2]
;;     (loop [remaining file2
;;             result-set []]
;;         (if (empty? remaining)
;;             (into #{} result-set)
;;             (let [first-line (first remaining)
;;                 next-remaining (rest remaining)
;;                 ;;next-result-set (conj result-set (get first-line :datadate)
;;                 next-result-set (conj result-set {:tic (get first-line TICKER-KEY) :datadate (get first-line :date)})
;;                 ]
;;             (recur next-remaining next-result-set)
;;             )
;;         )
;;     )
;; )

;; (defn get-tickers 
;;   "return a set of tickers"
;;   [file]
;;   (get-set file)
;; )

;; (defn insert-col
;;   "insert the date col from COMPUSTAT into CRSP"
;;     [file1 set] 
;;     ;; key: before the insert col; file1: insert into this file; set: col to be inserted
;;     ;;add a thing in each map using key and value
;;     ;;run loop, length of the loop = length of the list
;;     (for [row file1] (assoc row :datadate (condp contains? (last-quar row) set (get (last-quar row) :datadate) "")))
;; )

;; (defn merge-data-row
;;     "merge 2 csv files "
;;     [file1 file2]

;;     (def f1 (read-csv-row file1)) ;;file 1 is CRSP
;;     (def f2 (read-csv-row file2)) ;;file 2 Is COMPUSTAT

;;     (def f0 (insert-col f1 (get-set f2))) ;;insert datadate to file 1

;;     ;(left-join f0 f2 {:datadate :datadate :tic TICKER-KEY})

;;     ;;(def file0 (insert-col file1 set))
;;     ;; need to parse-int later
;; )

;; (defn merge-data-col
;;     "merge 2 csv files based on the column model"
;;     [file1 file2]
;;     (row->col (merge-data-row file1 file2))
;; )

;; ========== deprecated codes ===========

;; (defn- get-with-date-key-tic
;;   "This function returns the content"
;;   [date key tic data-set]
;;   ;The code below is mostly copying from search-date
;;   (loop [count 0 remaining data-set] ;(original line)
;;     (if (empty? remaining)
;;       NOMATCH
;;       (let [first-line (first remaining)
;;             next-remaining (rest remaining)]
;;         (if (and (= (get first-line :date) date) ;;amend later if the merge data-set has different keys (using the keys in CRSP now)
;;                  (= (get first-line TICKER-KEY) tic) ;;amend later if the merge data-set has different keys(using the keys in CRSP now)
;;                  )
;;           (get first-line key)
;;           (recur (inc count) next-remaining)))))
;;   )

;; (defn- get-pre-date-and-content
;;   "This function should return the previous date and content for the specific date and tic"
;;   [key date tic data-set]
;;   ;1. Find the previous date
;;   (loop [i 1]
;;     (if (<= i MAXLOOKAHEAD)
;;             ;(println (look-ahead-i-days (get-date) i))
;;       (let [prev-date (look-i-days-ago date i)]
;;         (let [content (get-with-date-key-tic prev-date key tic data-set)]
;;           (if (= content NOMATCH)
;;             (recur (inc i))
;;             [prev-date content])))
;;       [NOMATCH NOMATCH])))

;; (defn get-prev-n-days
;;   "This function returns a vector that contains data of a given key for the previous n days"
;;   ;key of keyword type
;;   ;n   number of counting ahead
;;   ;tic name of the stock
;;   [key n tic & [pre reference]]
;;   (let [date (atom (look-ahead-i-days (get-date) 1)) [count-tmp result-tmp] (if pre
;;                                                                               (if (<= (count pre) n)
;;                                                                                 (if (< (count pre) n)
;;                                                                                   [(- n 1) (into [] pre)]
;;                                                                                   [(- n 1) (into [] (rest pre))])
;;                                                                                 [0 '()])
;;                                                                               [0 '()])
;;         ref (or reference (deref data-set))]
;;     (if (or (= (get (deref available-tics) tic) nil) (not= ref (deref data-set)))
;;       (loop [count count-tmp result result-tmp]
;;         (if (< count n)
;;           (let [[prev-date content] (get-pre-date-and-content key (deref date) tic ref)]
;;             (if (not (= prev-date NOMATCH)) ; has date, has content
;;               (do
;;                 (reset! date prev-date)
;;                 (recur (+ count 1) (conj result {:date prev-date key content})))
;;               result))
;;           result))
;;       (let [line-num (get (get (deref available-tics) tic) :num)]
;;        (if (= line-num nil)
;;          (let [line (get (get (deref available-tics) tic) :reference)] (conj result-tmp {:date (get line :date) key (get line key)}))
;;          (loop [count count-tmp result result-tmp num line-num]
;;            (if (and (< count n) (>= num 0))
;;              (let [line (nth ref num) [date content ticker] [(get line :date) (get line key) (get line TICKER-KEY)]]
;;                (if (= ticker tic) ; has date, has content
;;                  (do
;;                    (recur (+ count 1) (conj result {:date date key content}) (- num 1)))
;;                  result))
;;              result))))))
;;   )

;; (defn moving-average
;;   [key list]
;;   (if (<= (count list) 0)
;;     0
;;     (average (map (fn [_] (Double/parseDouble (get _ key))) list))))

;; ;; Two functions designed for example / Bollinger Bands
;; (defn moving-sd
;;   [key vec]
;;   "This function returns the s.d. of the vec[key]"
;;   (stat/sd (map (fn [_] (Double/parseDouble (get _ key))) vec)))

;; (defn get-price
;;   "Returns the price of a ticker today"
;;   ([tic mode]
;;     (if (= "lazy" mode)
;;       ;(deref lazy-mode)
;;         (Double/parseDouble (get (get (get (deref available-tics) tic) :reference) :PRC))
;;         (get (first (get (get (deref available-tics) tic) :reference)) :PRC)
;;       ))
;;   ([tic key mode]
;;     (if (= "lazy" mode)
;;       (Double/parseDouble (get (get (get (deref available-tics) tic) :reference) key))
;;       (get (first (get (get (deref available-tics) tic) :reference)) key))
;;       )
;;   )

;; (defn get-by-key
;;   "Returns the [key] of a ticker today"
;;   [tic key mode]
;;     (if (= "lazy" mode)
;;       (Double/parseDouble (get (get (get (deref available-tics) tic) :reference) key))
;;       (get (first (get (get (deref available-tics) tic) :reference)) key))
;;     )
