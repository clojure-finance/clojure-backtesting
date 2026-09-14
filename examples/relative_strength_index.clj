;; gorilla-repl.fileformat = 1

;; **
;;; ## Relative Strength Index
;; **

;; @@
(ns clojure-backtesting.examples.rsi
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
;;; ### Relative Strength Index (RSI) strategy
;;; 
;;; RSI = 100 - 100 / (1 + RS), where RS = average gain / average loss.
;;; 
;;; The first average gain and loss are simple averages over the past 14 changes. After that, Wilder's smoothing applies: average gain = (previous average gain x 13 + current gain) / 14, and likewise for the loss.
;;; 
;;; We trade according to the following rules:
;;; - **Buy signal**: RSI drops below the lower threshold (30)
;;; - **Sell signal**: RSI rises above the upper threshold (70)
;;; 
;;; `RSI` returns nil until 14 closes are available.
;; **

;; @@
(def lower-threshold 30)
(def upper-threshold 70)

(while (< (compare (get-date) "1990-03-29") 0)
  (when-let [rsi (RSI "10002")]
    (when (< rsi lower-threshold)
      (order "10002" 5 :print false)) ; buy signal
    (when (> rsi upper-threshold)
      (order "10002" -5 :print false))) ; sell signal
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
;;; |      :date | :permno | :price |  :aprc | :quantity |
;;; |------------+---------+--------+--------+-----------|
;;; | 1990-02-21 |   10002 | 135.08 | 135.08 |      -5.0 |
;;; | 1990-02-22 |   10002 | 133.11 | 133.11 |      -5.0 |
;;; | 1990-03-02 |   10002 |  66.92 | 133.84 |       5.0 |
;;; | 1990-03-05 |   10002 |  65.73 | 131.46 |       5.0 |
;;; | 1990-03-06 |   10002 |  66.15 | 132.30 |       5.0 |
;;; | 1990-03-07 |   10002 |  65.25 | 130.50 |       5.0 |
;;; | 1990-03-08 |   10002 |  65.33 | 130.66 |       5.0 |
;;; | 1990-03-09 |   10002 |  65.21 | 130.42 |       5.0 |
;;; | 1990-03-12 |   10002 |  66.15 | 132.30 |       5.0 |
;;; | 1990-03-13 |   10002 |  66.24 | 132.48 |       5.0 |
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
;;; |   cash |    N/A |   N/A |       N/A |  9905.05 |
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
;;; | 1990-01-03 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-04 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-05 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-08 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-09 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-10 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-11 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-12 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
;;; | 1990-01-15 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 | 100.00% |
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
;;; | 1990-02-21 |     $10000 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |      $0 |        0.0000 |
;;; | 1990-02-22 |     $10009 | 0.0000% | 0.0000% | -2.5752 |   -2.8983 |      $4 |        0.0000 |
;;; | 1990-02-23 |     $10006 | 0.0053% | 0.0060% | -2.5420 |   -2.8983 |      $3 |        0.0330 |
;;; | 1990-02-26 |     $10030 | 0.0385% | 0.0445% |  2.1385 |    2.4664 |     $15 |        0.0330 |
;;; | 1990-02-27 |     $10013 | 0.0466% | 0.0547% |  0.3406 |    0.3964 |      $6 |        0.1665 |
;;; | 1990-02-28 |     $10022 | 0.0479% | 0.0569% |  1.0164 |    1.1978 |     $11 |        0.1665 |
;;; | 1990-03-01 |     $10017 | 0.0482% | 0.0579% |  0.5588 |    0.6657 |      $8 |        0.1665 |
;;; | 1990-03-02 |     $10002 | 0.0476% | 0.0579% |  0.5525 |    0.6657 |      $0 |        0.2801 |
;;; | 1990-03-05 |     $10020 | 0.0471% | 0.0579% |  0.5465 |    0.6657 |      $5 |        0.2801 |
;;; | 1990-03-06 |     $10016 | 0.0465% | 0.0579% |  0.5406 |    0.6657 |      $3 |        0.2801 |
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
