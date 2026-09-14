;; gorilla-repl.fileformat = 1

;; **
;;; ## Bollinger Bands
;; **

;; @@
(ns clojure-backtesting.examples.bollinger
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
;;; ### Write a strategy
;;; 
;;; The Bollinger Bands indicator measures how high or low the price is relative to previous trades. It consists of a middle band with two outer bands:
;;; 
;;; - Middle band = 20-day simple moving average (SMA)
;;; - Upper band = 20-day SMA + 2 x 20-day standard deviation of price
;;; - Lower band = 20-day SMA - 2 x 20-day standard deviation of price
;;; 
;;; Buy signal: price goes below the lower band. Sell signal: price goes above the upper band.
;;; 
;;; The strategy does nothing while fewer than 20 closes are available (`moving-avg` and `moving-sd` return nil), which also covers CCC's gap in February.
;; **

;; @@
(defn bollinger-bands
  "The strategy"
  [permno]
  (let [price (get-permno-price permno)
        sma (moving-avg permno 20)
        sd20 (moving-sd permno 20)]
    (when (and price sma sd20)
      (let [upper (+ sma (* 2 sd20))
            lower (- sma (* 2 sd20))]
        (when (< price lower)
          (order permno 10 :print false))
        (when (> price upper)
          (order permno 0 :remaining true))))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#'clojure-backtesting.examples.bollinger/bollinger-bands</span>","value":"#'clojure-backtesting.examples.bollinger/bollinger-bands"}
;; <=

;; @@
(while (< (compare (get-date) "1990-03-29") 0)
  (bollinger-bands "10001")
  (bollinger-bands "10003")
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
(print-order-record)
;; @@
;; ->
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1990-01-31 |   10001 |  48.05 | 48.05 |      10.0 |
;;; | 1990-03-20 |   10001 |  44.69 | 45.15 |      10.0 |
;;; | 1990-03-21 |   10001 |  44.85 | 45.31 |      10.0 |
;;; | 1990-03-22 |   10001 |  44.91 | 45.37 |      10.0 |
;;; | 1990-03-29 |   10001 |  43.87 | 44.32 |  -40.1025 |
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
;;; |   cash |    N/A |   N/A |       N/A |  9934.30 |
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
;;; | 1990-01-31 |     $10000 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |      $0 |        0.0000 |
;;; | 1990-02-01 |      $9996 | 0.0083% | 0.0083% | -3.3101 |   -3.3101 |     $-4 |        0.0400 |
;;; | 1990-02-02 |      $9995 | 0.0082% | 0.0082% | -3.3240 |   -3.3240 |     $-4 |        0.0410 |
;;; | 1990-02-05 |     $10000 | 0.0128% | 0.0128% |  0.3485 |    0.3485 |      $0 |        0.0410 |
;;; | 1990-02-06 |     $10008 | 0.0199% | 0.0199% |  2.6432 |    2.6432 |      $8 |        0.0410 |
;;; | 1990-02-07 |     $10000 | 0.0258% | 0.0258% |  0.0228 |    0.0228 |      $0 |        0.0849 |
;;; | 1990-02-08 |      $9994 | 0.0275% | 0.0275% | -1.1350 |   -1.1350 |     $-5 |        0.1409 |
;;; | 1990-02-09 |     $10002 | 0.0309% | 0.0309% |  0.4252 |    0.4252 |      $2 |        0.1409 |
;;; | 1990-02-12 |      $9998 | 0.0313% | 0.0313% | -0.2709 |   -0.2709 |     $-1 |        0.1409 |
;;; | 1990-02-13 |      $9999 | 0.0308% | 0.0313% | -0.0665 |   -0.0675 |      $0 |        0.1409 |
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
