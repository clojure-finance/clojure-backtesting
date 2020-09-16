(ns clojure-backtesting.application
  (:require [clojure.test :refer :all]
            [clojure-backtesting.core :refer :all]
            [clojure.string :as str]))

(defn merge
  "merge 2 csv files "
  [file1 file2]
  (def file1 ;;file 1 CRSP

    )
  
  (def file2
    )
  


  ;; get a set of datadate from compustat
  (defn get_set)
  (def file0 insert-col file1 set )

 )

(defn insert-col
  "insert the date col from COMPUSTAT into CRSP"
  [file1 set] ;; key: before the insert col; file1: insert into this file; set: col to be inserted
  ;;add a thing in each map using key and value
  ;;run loop, length of the loop = length of the list, 
  ;;read the value of the key "date", compare with "leo think of the algo"
  (for [row file1] (concat row {:datadate (last_quar row)}))

  )

  (defn last_quar
    "return the last quarter date of a given row"
    [row]
    (let [date (get row :date)]
      (let [[year month day] (map parse-int (str/split date #"-"))]
        if(...))
    )
  )
