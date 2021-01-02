(ns clojure-backtesting.data-management
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.specs :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure.string :as str]
            [clj-time.core :as clj-t]
            [java-time :as t]
            ))

(defn- get-set
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

(defn get-tickers 
  "return a set of tickers"
  [file]
  (let [tickers-map (get-set file)]
  )
)

(defn- last-quar
    "return the last quarter date of a given row"
    [row]
    (let [date (get row :date) tic (get row :TICKER)]
        (let [[year month day] (map parse-int (str/split date #"-"))]
            {:tic tic :datadate (cond
            (or (and (= month 12) (= day 31))(= month 1) (= month 2) (and (= month 3) (<= day 30))) (str (- year 1) "-" 12 "-" 31)
            (or (and (= month 3) (= day 31)) (= month 4) (= month 5) (and (= month 6) (<= day 29))) (str year "-3-" 31)
            (or (and (= month 6) (= day 30)) (= month 7) (= month 8) (and (= month 9) (<= day 30))) (str year "-6-" 30)
            (or (and (= month 9) (= day 31)) (= month 10) (= month 11) (and (= month 12) (<= day 30))) (str year "-" 9 "-" 30))}
        )
    )
)


(defn insert-col
  "insert the date col from COMPUSTAT into CRSP"
    [file1 set] 
    ;; key: before the insert col; file1: insert into this file; set: col to be inserted
    ;;add a thing in each map using key and value
    ;;run loop, length of the loop = length of the list
    (for [row file1] (assoc row :datadate (condp contains? (last-quar row) set (get (last-quar row) :datadate) "")))
)

(defn merge-data-row
    "merge 2 csv files "
    [file1 file2]
    
    (def f1 (read-csv-row file1)) ;;file 1 is CRSP
    (def f2 (read-csv-row file2)) ;;file 2 Is COMPUSTAT
    
    (def f0 (insert-col f1 (get-set f2))) ;;insert datadate to file 1

    ;(left-join f0 f2 {:datadate :datadate :tic :TICKER})

    ;;(def file0 (insert-col file1 set))
    ;; need to parse-int later
)

(defn merge-data-col
    "merge 2 csv files based on the column model"
    [file1 file2]
    (row->col (merge-data-row file1 file2))
)

(defn look-n-days-ago
  "This function is the opposite of look-ahead-n-days"
  [date n]
  (let [[year month day] (map parse-int (str/split date #"-"))]
    (t/format "yyyy-MM-dd" (t/minus (t/local-date year month day) (t/days n)))))

(defn get-with-date-key-tic
  "This function returns the content"
  [date key tic data-set]
  ;The code below is mostly copying from search-date
  (loop [count 0 remaining data-set] ;(original line)
  ;(loop [count 0 remaining testfile1] 				;testing line, change the data-set to CRSP
    (if (empty? remaining)
      NOMATCH
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (if (and (= (get first-line :date) date) ;;amend later if the merge data-set has different keys (using the keys in CRSP now)
                 (= (get first-line :TICKER) tic) ;;amend later if the merge data-set has different keys(using the keys in CRSP now)
                 )
          (get first-line key)
          (recur (inc count) next-remaining)))))
  )

(defn get-pre-date-and-content
  "This function should return the previous date and content for the specific date and tic"
  [key date tic data-set]
  ;1. Find the previous date
  (loop [i 1]
    (if (<= i MAXLOOKAHEAD)
            ;(println (look-ahead-i-days (get-date) i))
      (let [prev-date (look-n-days-ago date i)]
        (let [content (get-with-date-key-tic prev-date key tic data-set)]
          (if (= content NOMATCH)
            (recur (inc i))
            [prev-date content])))
      [NOMATCH NOMATCH])))

(defn get-prev-n-days
  "This function returns a vector that contains data of a given key for the previous n days"
  ;key of keyword type
  ;n   number of counting ahead
  ;tic name of the stock
  [key n tic & [pre reference]]
  (let [date (atom (look-ahead-i-days (get-date) 1)) [count-tmp result-tmp] (if pre
                                                                              (if (<= (count pre) n)
                                                                                (if (< (count pre) n)
                                                                                  [(- n 2) pre]
                                                                                  [(- n 2) (rest pre)])
                                                                                [0 []])
                                                                              [0 []])]
    (loop [count count-tmp result result-tmp]
      (let [[prev-date content] (get-pre-date-and-content key (deref date) tic (or reference (deref data-set)))]
        (if (and (not (= prev-date NOMATCH)) (< count n)) ; has date, has content
          (do
            (reset! date prev-date)
            (recur (+ count 1) (conj result {:date prev-date key content})))
          result))))
  )

(defn available-tics
  "This function returns the available tickers and also the optimizor on a date"
  [date]
  ;;date e.g. "DD/MM?YYYY"
  ;;tic e.g. "AAPL"
  ;;return [false 0 0] if no match
  ;;return [true price reference] otherwise
  
  (loop [remaining (deref data-set) result []] ;(original line)
  ;(loop [count 0 remaining testfile1] 				;testing line, change the data-set to CRSP
    (if (empty? remaining)
      result
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (if  (= (get first-line :date) date) ;;amend later if the merge data-set has different keys (using the keys in CRSP now)
          (recur next-remaining (conj result [(get first-line :TICKER) remaining]))
          (recur next-remaining result)))))
)

(defn moving-average
  [key list]
   (average (map (fn [_] (Double/parseDouble (get _ key))) list)))

;; (defn moving-average
;;   [key days]
;;   ())