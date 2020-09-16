(ns clojure-backtesting.application
  (:require [clojure.test :refer :all]
            [clojure-backtesting.core :refer :all]))

(defn merge
  "merge 2 csv files "
  [file1 file2]
  (def file1 (slurp-csv file1)) ;;file 1 is CRSP
  
  (def file2 (slurp-csv file2)) ;;file 2 is COMPUSTAT
  
  (def set 
    (doseq temp (slurp-csv file2))
    (set (get data :datadate))) 

  (def file0 (insert-col file1 set))
  
  ;; need to parse-int later
  ()


)





(defn insert-col
  "insert the date col from COMPUSTAT into CRSP"
  [file1 set] ;; key: before the insert col; file1: insert into this file; set: col to be inserted
  ;;add a thing in each map using key and value
  ;;run loop, length of the loop = length of the list, 
  ;;read the value of the key "date", compare with "leo think of the algo"



  )


