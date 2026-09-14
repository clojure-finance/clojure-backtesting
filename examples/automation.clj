;; gorilla-repl.fileformat = 1

;; **
;;; ## Automation Example
;;; 
;;; An automation pairs a condition with an action. At the end of every trading day each condition is checked and, when it holds, the action runs. `:max-dispatch` limits how often, and `:expiration` (in trading days) how long an automation stays active. The `condition` and `action` macros keep the source form so that `print-automation-list` can show it.
;; **

;; @@
(ns clojure-backtesting.examples.automation
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
            [clojure-backtesting.direct :refer :all]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(load-dataset "resources/sample-data/main" "main" add-aprc)
(init-portfolio "1990-01-02" 10000)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1990-01-02 Cash: $10000&quot;</span>","value":"\"Date: 1990-01-02 Cash: $10000\""}
;; <=

;; **
;;; ### Stop and limit orders
;;; 
;;; Buy 100 shares of AAA, then protect the position: a stop-sell if the price falls under $49, and a limit-sell to take profit above $52. Each fires at most once.
;; **

;; @@
(order "10001" 100 :print true)

(defn stop-sell
  "Sell qty shares once the price falls below prc."
  [permno prc qty]
  (set-automation
   (condition #(when-let [price (get-permno-price permno)] (< price prc)))
   (action #(order permno (- qty) :print true))
   :max-dispatch 1))

(defn limit-sell
  "Sell qty shares once the price rises above prc."
  [permno prc qty]
  (set-automation
   (condition #(when-let [price (get-permno-price permno)] (> price prc)))
   (action #(order permno (- qty) :print true))
   :max-dispatch 1))

(stop-sell "10001" 49 100)
(limit-sell "10001" 52 100)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-long'>2</span>","value":"2"}
;; <=

;; @@
(print-automation-list)
;; @@
;; ->
;;; 
;;; | :id |                                                          :condition |                                     :action | :max-dispatch | :expire-date |
;;; |-----+---------------------------------------------------------------------+---------------------------------------------+---------------+--------------|
;;; |   1 | (fn* [] (when-let [price (get-permno-price permno)] (< price prc))) | (fn* [] (order permno (- qty) :print true)) |             1 |        Never |
;;; |   2 | (fn* [] (when-let [price (get-permno-price permno)] (> price prc))) | (fn* [] (order permno (- qty) :print true)) |             1 |        Never |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Run
;; **

;; @@
(dotimes [_ 30]
  (next-date))
(get-date)
;; @@
;; ->
;;; Order: 1990-01-03 | 10001 | 100.000000.
;;; Automation 2 dispatched.
;;; Order: 1990-01-19 | 10001 | -100.000000.
;;; Automation 1 dispatched.
;;; Order: 1990-01-30 | 10001 | -100.000000.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;1990-02-13&quot;</span>","value":"\"1990-02-13\""}
;; <=

;; @@
(print-automation-history)
;; @@
;; ->
;;; 
;;; |      :date | :automation |
;;; |------------+-------------|
;;; | 1990-01-18 |           2 |
;;; | 1990-01-29 |           1 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(print-order-record)
;; @@
;; ->
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1990-01-03 |   10001 |  50.81 | 50.81 |     100.0 |
;;; | 1990-01-19 |   10001 |  51.85 | 51.85 |    -100.0 |
;;; | 1990-01-30 |   10001 |  47.77 | 47.77 |    -100.0 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(print-portfolio)
;; @@
;; ->
;;; 
;;; | :asset | :price |   :aprc | :quantity | :tot-val |
;;; |--------+--------+---------+-----------+----------|
;;; |   cash |    N/A |     N/A |       N/A | 14881.00 |
;;; |  10001 |  48.01 | 48.0100 |    -100.0 | -4801.00 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=
