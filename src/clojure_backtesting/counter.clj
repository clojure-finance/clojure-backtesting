(ns clojure-backtesting.counter
  (:require [clojure-backtesting.data :refer :all]))

(def date (atom nil))

(defn get-date
  []
  (deref date))

(defn init-date
  "Initialize the current date as input date.\n
   If the input not in dataset, will go to the closest smaller date (if any, otherwise closest larger date).\n
   Return the resultant date."
  [_date]
  (if-let [_date (first (first (rsubseq data-files <= _date)))]
    (reset! date _date)
    (if-let [_date (first (first (subseq data-files > _date)))]
      (reset! date _date)
      (throw (Exception. "Input date is wrong"))))
  (get-date))

(defn get-next-date
  "Returns the next date without actually moving the counter."
  []
  (first (first (subseq data-files > (get-date)))))

(defn get-prev-n-date
  "Returns the date of the previous n valid days.\n
   E.g. prev 1 of 1973-02-04 may be 1973-02-03 or 1973-02-02 or even 1973-02-01."
  [n]
  (assert (> n 0) "n should be larger than 0.")
  (let [tmp (take n (rsubseq data-files < (get-date)))]
    (if (= n (count tmp))
      (first (last tmp))
      nil)))
