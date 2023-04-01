;; gorilla-repl.fileformat = 1

;; **
;;; # Automation Example
;; **

;; @@
(ns clojure-backtesting.updated_examples.automation
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [clojure-backtesting.direct :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
  ) ;; require all libriaries from core
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(load-dataset "./CRSP" "main" add-aprc) ; place relative directory to CRSP
(init-portfolio "1986-01-09" 2000)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1986-01-09 Cash: $2000&quot;</span>","value":"\"Date: 1986-01-09 Cash: $2000\""}
;; <=

;; **
;;; ### Limit Order
;;; #### Stop Buy
;; **

;; @@
(defn stop-buy
  "This function executes a stop buy order."
  [tic prc qty]
  (set-automation
  ; check if ticker adjusted price is greater than prc
   (condition #(and (get (deref portfolio) tic) (> (get-in (deref portfolio) [tic :aprc]) prc)))
   (action #(order tic qty))
   :max-dispatch nil
   :expiration nil))

(stop-buy "30154" 32 10)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>1</span>","value":"1"}
;; <=

;; @@
(defn limit-buy
  "This function executes a limit buy order."
  [tic prc qty]
  (set-automation
  ; check if ticker adjusted price is smaller than prc
   (condition #(and (get (deref portfolio) tic) (< (get-in (deref portfolio) [tic :aprc]) prc)))
   (action #(order tic qty))
   :max-dispatch nil
   :expiration nil))

(limit-buy "30155" 35 10)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>2</span>","value":"2"}
;; <=

;; @@
(defn stop-sell
  "This function executes a stop sell order."
  [tic prc qty]
  (set-automation
  ; check if ticker adjusted price is smaller than prc
   (condition #(and (get (deref portfolio) tic) (< (get-in (deref portfolio) [tic :aprc]) prc)))
   (action #(order tic (- qty)))
   :max-dispatch nil
   :expiration nil))

(stop-sell "30154" 35 10)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>3</span>","value":"3"}
;; <=

;; @@
(defn limit-sell
  "This function executes a limit sell order."
  [tic prc qty]
  (set-automation
  ; check if ticker adjusted price is smaller than prc
   (condition #(and (get (deref portfolio) tic) (> (get-in (deref portfolio) [tic :aprc]) prc)))
   (action #(order tic (- qty)))
   :max-dispatch nil
   :expiration nil))

(limit-sell "30155" 35 10)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>4</span>","value":"4"}
;; <=

;; @@
(print-automation-list)
;; @@
;; ->
;;; 
;;; | :id |                                                                                :condition |                      :action | :max-dispatch | :expire-date |
;;; |-----+-------------------------------------------------------------------------------------------+------------------------------+---------------+--------------|
;;; |   1 | (fn* [] (and (get (deref portfolio) tic) (&gt; (get-in (deref portfolio) [tic :aprc]) prc))) |     (fn* [] (order tic qty)) |     Unlimited |        Never |
;;; |   2 | (fn* [] (and (get (deref portfolio) tic) (&lt; (get-in (deref portfolio) [tic :aprc]) prc))) |     (fn* [] (order tic qty)) |     Unlimited |        Never |
;;; |   3 | (fn* [] (and (get (deref portfolio) tic) (&lt; (get-in (deref portfolio) [tic :aprc]) prc))) | (fn* [] (order tic (- qty))) |     Unlimited |        Never |
;;; |   4 | (fn* [] (and (get (deref portfolio) tic) (&gt; (get-in (deref portfolio) [tic :aprc]) prc))) | (fn* [] (order tic (- qty))) |     Unlimited |        Never |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(next-date)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;1986-01-10&quot;</span>","value":"\"1986-01-10\""}
;; <=

;; @@
(print-order-record)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
