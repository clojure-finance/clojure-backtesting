(ns clojure-backtesting.application
  (:require [clojure.test :refer :all]
            [clojure-backtesting.core :refer :all]
            [clojure.string :as str]))

(defn merge-data
  "merge 2 csv files "
  [file1 file2]
  ((def file1 (slurp-csv file1)) ;;file 1 is CRSP
  
  (def file2 (slurp-csv file2)) ;;file 2 Is COMPUSTAT
  
  (def set 
    (doseq temp (slurp-csv file2))
    (set (get data :datadate))) 

  (def file0 (insert-col file1 set)))
  
  ;; need to parse-int later
)

(defn insert-col
  "insert the date col from COMPUSTAT into CRSP"
  [file1 set] ;; key: before the insert col; file1: insert into this file; set: col to be inserted
  ;;add a thing in each map using key and value
  ;;run loop, length of the loop = length of the list, 
  ;;read the value of the key "date", compare with "leo think of the algo"
  (for [row file1] (concat row {:datadate (condp contains? (last-quar row) set (last-quar row) "")}))
)

  (defn last-quar
    "return the last quarter date of a given row"
    [row]
    (let [date (get row :date)]
      (let [[year month day] (map parse-int (str/split date #"/"))]
        (cond
        (or (and (= month 12) (= day 31))(= month 1) (= month 2) (and (= month 3) (<= day 30))) (str (- year 1) "/" 12 "/" 31)
        (or (and (= month 3) (= day 31)) (= month 4) (= month 5) (and (= month 6) (<= day 29))) (str year "/3/" 31)
        (or (and (= month 6) (= day 30)) (= month 7) (= month 8) (and (= month 9) (<= day 30))) (str year "/6/" 30)
        (or (and (= month 9) (= day 31)) (= month 10) (= month 11) (and (= month 12) (<= day 30))) (str year "/" 9 "/" 31))
      )
    )
  )


