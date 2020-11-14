(ns clojure-backtesting.user
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            ;;[clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            ;;[clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            ))

;;testing purpose
; (comment 
; (def filex "/home/kony/Documents/GitHub/clojure-backtesting/resources/Compustat-extract.csv")

; (def a (read-csv-row filex))
; )        


(defn get-set
    "return a set of maps of tickers and datadate" ;;{:tic "AAPL", :datadate "1981/3/31"}
    [file2]
    (loop [remaining file2
            result-set []]
        (if (empty? remaining)
            (into #{} result-set)
            (let [first-line (first remaining)
                next-remaining (rest remaining)
                ;;next-result-set (conj result-set (get first-line :datadate)
                next-result-set (conj result-set {:tic (get first-line :TICKER) :datadate (get first-line :datadate)})
                ]
            (recur next-remaining next-result-set)
            )
        )
    )
)


(defn last-quar
    "return the last quarter date of a given row"
    [row]
    (let [date (get row :date)]
        (let [[year month day] (map parse-int (str/split date #"-"))]
            (cond
            (or (and (= month 12) (= day 31))(= month 1) (= month 2) (and (= month 3) (<= day 30))) (str (- year 1) "-" 12 "-" 31)
            (or (and (= month 3) (= day 31)) (= month 4) (= month 5) (and (= month 6) (<= day 29))) (str year "-3-" 31)
            (or (and (= month 6) (= day 30)) (= month 7) (= month 8) (and (= month 9) (<= day 30))) (str year "-6-" 30)
            (or (and (= month 9) (= day 31)) (= month 10) (= month 11) (and (= month 12) (<= day 30))) (str year "-" 9 "-" 30))
        )
    )
)


(defn insert-col
  "insert the date col from COMPUSTAT into CRSP"
  [file1 set] ;; key: before the insert col; file1: insert into this file; set: col to be inserted
    ;;add a thing in each map using key and value
    ;;run loop, length of the loop = length of the list
    (for [row file1] (concat row {:datadate (condp contains? (last-quar row) set (last-quar row) "")}))
)

(defn merge-data-row
    "merge 2 csv files "
    [file1 file2]
    
    (def f1 (read-csv-row file1)) ;;file 1 is CRSP
    (def f2 (read-csv-row file2)) ;;file 2 Is COMPUSTAT
    
    ;(def f0 (insert-col f1 (get-set f2)))) ;;insert datadate to file 1
    ;(left-join f0 f2 {:datadate :datadate :tic :TICKER})

    ;;(def file0 (insert-col file1 set))
    ;; need to parse-int later
)

(defn merge-data-col
    "merge 2 csv files based on the column model"
    [file1 file2]
    (row->col (merge-data-row file1 file2))
)

; main function for reading dataset
(defn main-read-data
    [& args]
    (println args)
    (reset! data-set (read-csv-row (first args)))
    (pprint/print-table (deref data-set))
)

(defn main-order
    "Main function for placing orders"
      [& args] ; pass ./resources/CRSP-extract.csv as arg
      (println args)
      (reset! data-set (read-csv-row (first args)))
      (print(search_in_order "1980-12-17" "AAPL"))
      (print(look_ahead_i_days "1980-12-16" 2))
      (order_internal "1980-12-16" "AAPL" 10)
      (order_internal "1980-12-17" "AAPL" 10 true)
      (order_internal "1980-12-14" "AAPL" 10)
      (order_internal [["1980-12-19" "AAPL" 10]["1980-12-18" "AAPL" 10 true]])
      (pprint/print-table (deref order_record))
)

(defn -main
  "Write your code here"
    [& args] ; pass ./resources/CRSP-extract.csv as arg
    ;(println args)
    (reset! data-set (add_aprc (read-csv-row (first args))))
    ; (println (take 20 (deref data-set)))
    ; (init_portfolio "1980-12-16" 500)
    ; (order_internal "1980-12-16" "AAPL" 10)
    ; (order_internal "1980-12-16" "IBM" 10)
    ; (order_internal "1980-12-17" "IBM" -10)
    ; (println (deref portfolio))
    
    ;(println (search_in_order "1962-07-02" "IBM")) ; 22812
    (println (nth (deref data-set) 27440))
    ;(println (nth (deref data-set) 22810))
    ;(println (nth (deref data-set) 22811))
)
;;sample activation command:
;;lein run "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv"
