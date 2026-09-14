;; gorilla-repl.fileformat = 1

;; **
;;; ## Rate of Change example
;; **

;; @@
(ns clojure-backtesting.examples.roc
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

;; **
;;; ### Import dataset
;; **

;; @@
(load-dataset "resources/sample-data/main" "main" add-aprc)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 1990-01-02 ~ 1990-03-30&quot;</span>","value":"\"Date range: 1990-01-02 ~ 1990-03-30\""}
;; <=

;; **
;;; ### Initialise portfolio (go back here every time you want to restart)
;; **

;; @@
(init-portfolio "1990-01-02" 10000)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1990-01-02 Cash: $10000&quot;</span>","value":"\"Date: 1990-01-02 Cash: $10000\""}
;; <=

;; **
;;; ### Rate of Change (ROC) strategy
;;; 
;;; ROC = (closing price - closing price n periods ago) / closing price n periods ago
;;; 
;;; We trade according to the following rules, with n = 10 days:
;;; - **Buy signal**: ROC drops below the lower threshold (-3%)
;;; - **Sell signal**: ROC rises above the upper threshold (+3%)
;;; 
;;; `ROC` returns nil until n earlier closes exist.
;; **

;; @@
(def lower-threshold -0.03)
(def upper-threshold 0.03)

(while (< (compare (get-date) "1990-03-29") 0)
  (when-let [roc (ROC "10001" 10)]
    (when (< roc lower-threshold)
      (order "10001" 10 :print false)) ; buy signal
    (when (> roc upper-threshold)
      (order "10001" -10 :print false))) ; sell signal
  (update-eval-report)
  (next-date))
(end-order)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-boolean'>true</span>","value":"true"}
;; <=

;; **
;;; ### Check portfolio record
;; **

;; @@
(print-order-record 10)
;; @@
;; ->
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1990-01-19 |   10001 |  51.85 | 51.85 |     -10.0 |
;;; | 1990-01-30 |   10001 |  47.77 | 47.77 |      10.0 |
;;; | 1990-01-31 |   10001 |  48.05 | 48.05 |      10.0 |
;;; | 1990-02-01 |   10001 |  47.65 | 47.65 |      10.0 |
;;; | 1990-02-02 |   10001 |  47.64 | 47.64 |      10.0 |
;;; | 1990-02-05 |   10001 |  48.12 | 48.12 |      10.0 |
;;; | 1990-02-06 |   10001 |  48.91 | 48.91 |      10.0 |
;;; | 1990-02-08 |   10001 |   47.5 | 47.50 |      10.0 |
;;; | 1990-02-09 |   10001 |  48.29 | 48.29 |      10.0 |
;;; | 1990-02-21 |   10001 |  48.82 | 49.32 |      10.0 |
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
;;; | :asset | :price | :aprc | :quantity | :tot-val |
;;; |--------+--------+-------+-----------+----------|
;;; |   cash |    N/A |   N/A |       N/A |  9694.78 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(print-portfolio-record 10)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :short | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+--------+-----------+---------|
;;; | 1990-01-02 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-03 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-04 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-05 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-08 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-09 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-10 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-11 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-12 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; | 1990-01-15 |  $10000.00 |      0.00% |    0.00% | $0.00 | $-0.00 |      0.00 | 100.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Generate evaluation report
;; **

;; @@
(print-eval-report 10)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value |    :vol |  :r-vol | :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+---------+-----------+---------+---------------|
;;; | 1990-01-19 |     $10000 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |      $0 |        0.0000 |
;;; | 1990-01-22 |     $10019 | 0.0500% | 0.0500% |  4.0988 |    4.0988 |     $19 |        0.0000 |
;;; | 1990-01-23 |     $10017 | 0.0491% | 0.0491% |  3.4941 |    3.4941 |     $17 |        0.0210 |
;;; | 1990-01-24 |     $10018 | 0.0475% | 0.0475% |  3.7089 |    3.7089 |     $18 |        0.0210 |
;;; | 1990-01-25 |     $10019 | 0.0461% | 0.0461% |  3.7822 |    3.7822 |     $19 |        0.0210 |
;;; | 1990-01-26 |     $10022 | 0.0449% | 0.0449% |  4.1244 |    4.1244 |     $22 |        0.0210 |
;;; | 1990-01-29 |     $10031 | 0.0474% | 0.0474% |  5.2810 |    5.2810 |     $31 |        0.0210 |
;;; | 1990-01-30 |     $10040 | 0.0463% | 0.0463% |  5.1459 |    5.1459 |     $20 |        0.0210 |
;;; | 1990-01-31 |     $10040 | 0.0453% | 0.0453% |  5.0207 |    5.0207 |     $13 |        0.0210 |
;;; | 1990-02-01 |     $10036 | 0.0444% | 0.0444% |  4.9043 |    4.9043 |      $9 |        0.0398 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Plot variables
;; **

;; @@
(def data-to-plot (map #(assoc % :plot "portfolio") (deref portfolio-value)))
(plot data-to-plot :plot :date :daily-ret true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=
