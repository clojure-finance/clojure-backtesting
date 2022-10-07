(ns clojure-backtesting.counter-order
    (:require   [java-time :as jt]
                [clojure.string :as str]
                [clojure-backtesting.parameters :refer :all]
                [clojure-backtesting.data :refer :all]))

(def date (atom nil))

(defn get-date
  []
  (deref date))

(defn init-date
  "Initialize the current date as input date.\n
   If the input not in dataset, will go to the closest smaller date (if any, otherwise closest larger date).\n
   Return the resultant date."
  [_date]
  ;; date input format checking
  (if-let [_date (first (first (rsubseq data-files <= _date)))]
    (reset! date _date)
    (if-let [_date (first (first (subseq data-files > _date)))]
      (reset! date _date)
      (throw (Exception. "Input date is wrong"))))
  (reset! tics-today nil)
  (reset! tics-tomorrow nil)
  (get-date))

(defn get-next-date
  "Returns the next date without actually moving the counter."
  []
  (first (first (subseq data-files > (get-date)))))
