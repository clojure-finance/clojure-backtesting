;; gorilla-repl.fileformat = 1

;; **
;;; ## Simple Trading Strategy Example
;; **

;; @@
(ns clojure-backtesting.examples.simple-strategy
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
;;; 
;;; The bundled sample dataset has three securities over the first quarter of 1990: 10001 (AAA, pays a dividend on 1990-02-15), 10002 (BBB, splits 2-for-1 on 1990-03-01) and 10003 (CCC, no prices from 1990-02-05 to 1990-02-09). Point `load-dataset` at your own preprocessed data to use that instead.
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
;;; ### Initialise portfolio
;; **

;; @@
(init-portfolio "1990-01-02" 10000)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1990-01-02 Cash: $10000&quot;</span>","value":"\"Date: 1990-01-02 Cash: $10000\""}
;; <=

;; **
;;; ### Write a strategy
;;; 
;;; In a timespan of 10 trading days:
;;; - buy 50 shares of AAA on the first day
;;; - sell 10 shares of AAA on every other day
;;; 
;;; Orders fill at the close of the next trading day.
;; **

;; @@
(def num-of-days (atom 10))

(while (pos? @num-of-days)
  (when (= 10 @num-of-days)
    (order "10001" 50 :print true)) ; buy 50 shares
  (when (odd? @num-of-days)
    (order "10001" -10 :print true)) ; sell 10 shares
  (update-eval-report) ; update the evaluation metrics every day
  (println (get-date))
  (next-date) ; move on to the next trading day
  (swap! num-of-days dec))
;; @@
;; ->
;;; 1990-01-02
;;; Order: 1990-01-03 | 10001 | 50.000000.
;;; 1990-01-03
;;; Order: 1990-01-04 | 10001 | -10.000000.
;;; 1990-01-04
;;; 1990-01-05
;;; Order: 1990-01-08 | 10001 | -10.000000.
;;; 1990-01-08
;;; 1990-01-09
;;; Order: 1990-01-10 | 10001 | -10.000000.
;;; 1990-01-10
;;; 1990-01-11
;;; Order: 1990-01-12 | 10001 | -10.000000.
;;; 1990-01-12
;;; 1990-01-15
;;; Order: 1990-01-16 | 10001 | -10.000000.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Check order record
;; **

;; @@
(print-order-record)
;; @@
;; ->
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1990-01-03 |   10001 |  50.81 | 50.81 |      50.0 |
;;; | 1990-01-04 |   10001 |  50.22 | 50.22 |     -10.0 |
;;; | 1990-01-08 |   10001 |   50.4 | 50.40 |     -10.0 |
;;; | 1990-01-10 |   10001 |  49.28 | 49.28 |     -10.0 |
;;; | 1990-01-12 |   10001 |  49.29 | 49.29 |     -10.0 |
;;; | 1990-01-16 |   10001 |  51.04 | 51.04 |     -10.0 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Check portfolio record
;; **

;; @@
(print-portfolio)
;; @@
;; ->
;;; 
;;; | :asset | :price | :aprc | :quantity | :tot-val |
;;; |--------+--------+-------+-----------+----------|
;;; |   cash |    N/A |   N/A |       N/A |  9961.80 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(print-portfolio-record -1)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :short | :leverage |  :margin |
;;; |------------+------------+------------+----------+-------+--------+-----------+----------|
;;; | 1990-01-02 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 |  100.00% |
;;; | 1990-01-03 |  $10000.00 |      0.00% |    0.00% | $0.00 |  $0.00 |      0.00 |  393.62% |
;;; | 1990-01-04 |   $9970.50 |      0.00% |   -0.30% | $0.00 |  $0.00 |      0.00 |  496.34% |
;;; | 1990-01-05 |   $9976.50 |      0.06% |   -0.24% | $0.00 |  $0.00 |      0.00 |  495.16% |
;;; | 1990-01-08 |   $9977.70 |      0.00% |   -0.22% | $0.00 |  $0.00 |      0.00 |  659.90% |
;;; | 1990-01-09 |   $9963.00 |     -0.15% |   -0.37% | $0.00 |  $0.00 |      0.00 |  665.40% |
;;; | 1990-01-10 |   $9944.10 |      0.00% |   -0.56% | $0.00 |  $0.00 |      0.00 | 1008.94% |
;;; | 1990-01-11 |   $9951.70 |      0.08% |   -0.48% | $0.00 |  $0.00 |      0.00 | 1001.98% |
;;; | 1990-01-12 |   $9944.30 |      0.00% |   -0.56% | $0.00 |  $0.00 |      0.00 | 2017.51% |
;;; | 1990-01-15 |   $9960.40 |      0.16% |   -0.40% | $0.00 |  $0.00 |      0.00 | 1956.86% |
;;; | 1990-01-16 |   $9961.80 |      0.00% |   -0.38% | $0.00 |  $0.00 |      0.00 |  100.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Generate evaluation report
;; **

;; @@
(print-eval-report)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value |    :vol |  :r-vol | :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+---------+-----------+---------+---------------|
;;; | 1990-01-03 |     $10000 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |      $0 |        0.0000 |
;;; | 1990-01-04 |      $9970 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |    $-14 |        0.2950 |
;;; | 1990-01-05 |      $9976 | 0.0301% | 0.0301% |  7.9373 |    7.9373 |    $-11 |        0.2950 |
;;; | 1990-01-08 |      $9977 | 0.0269% | 0.0269% |  7.0993 |    7.0993 |     $-7 |        0.2950 |
;;; | 1990-01-09 |      $9963 | 0.0694% | 0.0694% | -3.3269 |   -3.3269 |    $-12 |        0.3700 |
;;; | 1990-01-10 |      $9944 | 0.0636% | 0.0636% | -3.1121 |   -3.1121 |    $-13 |        0.5590 |
;;; | 1990-01-11 |      $9951 | 0.0667% | 0.0667% | -0.3235 |   -0.3235 |    $-12 |        0.5590 |
;;; | 1990-01-12 |      $9944 | 0.0624% | 0.0624% | -0.3074 |   -0.3074 |    $-11 |        0.5590 |
;;; | 1990-01-15 |      $9960 | 0.0782% | 0.0782% |  3.0617 |    3.0617 |     $-7 |        0.5590 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Plot variables
;;; 
;;; Below are example codes that show how to plot different variables in the portfolio record and the evaluation record. Each chart opens in the browser.
;; **

;; **
;;; #### 1. Portfolio daily return
;; **

;; @@
(def data-to-plot (map #(assoc % :plot "portfolio") (deref portfolio-value)))
(plot data-to-plot :plot :date :daily-ret true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; **
;;; #### 2. Volatility
;; **

;; @@
(def data-to-plot (map #(assoc % :plot "volatility") (deref eval-record)))
(plot data-to-plot :plot :date :vol true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; **
;;; #### 3. Sharpe ratio
;; **

;; @@
(def data-to-plot (map #(assoc % :plot "sharpe ratio") (deref eval-record)))
(plot data-to-plot :plot :date :sharpe true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; **
;;; #### 4. Fill prices
;; **

;; @@
(def data-to-plot (map #(assoc % :plot "price") (deref order-record)))
(plot data-to-plot :plot :date :price true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=
