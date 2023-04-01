;; gorilla-repl.fileformat = 1

;; **
;;; ## Relative Strength Index
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

;; **
;;; ### Import dataset
;; **

;; @@
; path to dataset = "/Volumes/T7/CRSP"
; change it to the relative path to your own dataset
;
(load-dataset "./Volumes/T7/CRSP" "main" add-aprc)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 1972-01-03 ~ 2017-02-10&quot;</span>","value":"\"Date range: 1972-01-03 ~ 2017-02-10\""}
;; <=

;; **
;;; ### Initialise portfolio （Go back here everytime you want to restart.）
;; **

;; @@
;; initialise with current date and initial capital (= $10000)
(init-portfolio "1980-12-15" 1000);
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1980-12-31 Cash: $1000&quot;</span>","value":"\"Date: 1980-12-31 Cash: $1000\""}
;; <=

;; **
;;; ### Relative Strength Index (RSI) strategy
;;;
;;; <p>RSI = 100 - 100 / (1 + RS)</p>
;;; RS = Average Gain / Average Loss
;;;
;;; <p>1st Average Gain = Sum of gains over past 14 periods / 14</p>
;;; 1st Average Loss = Sum of losses over past 14 periods / 14
;;;
;;; <p>Average Gain = (Previous average gain x 13 + Current gain) / 14</p>
;;; Average Loss = (Previous average loss x 13 + Current loss) / 14
;;;
;;; We'll trade according to the following rules:
;;; - **Buy signal**: when RSI crosses the lower threshold (e.g. 30)
;;; - **Sell signal**: when RSI crosses the upper threshold (e.g. 70)
;; **

;; @@
(get-date)
;; @@

;; @@
(def n 14)

;; go to 14 days after
(for [x (range n)] 
  (next-date))

(get-date)
;; @@

;; @@
(def lower-threshold 30)
(def uppper-threshold 60)
;; calculate average gains and losses for remaining days
(while (< (compare (get-date) "1981-03-31") 0)
  (do
    (let [rsi (RSI "14593")]
      (if (< rsi lower-threshold)
        (order "14593" 1 :print false)) ; buy signal
      (if (> rs upper-threshold)
        (order "14593" -1 :print false)) ; sell signal
      (update-eval-report)
      (next-date)))
(end-order))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>true</span>","value":"true"}
;; <=

;; @@
(print-order-record 10)
;; @@
;; ->
;;; 
;;; |      :date | :permno |  :price | :aprc | :quantity |
;;; |------------+---------+---------+-------+-----------|
;;; | 1981-01-02 |   14593 |  34.625 | 28.74 |        -1 |
;;; | 1981-01-05 |   14593 | 33.8125 | 28.45 |        -1 |
;;; | 1981-10-12 |   14593 | 19.3125 | 22.30 |        -1 |
;;; | 1981-10-13 |   14593 |  19.375 | 22.34 |        -1 |
;;; | 1981-12-28 |   14593 |    21.0 | 23.13 |         4 |
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
;;; |   cash |    N/A |   N/A |       N/A |  1009.30 |
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
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-31 |   $1000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-02 |   $1000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-05 |   $1000.29 |     -0.00% |    0.01% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-06 |   $1001.41 |      0.05% |    0.06% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-07 |   $1002.45 |      0.05% |    0.11% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-08 |   $1002.93 |      0.02% |    0.13% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-09 |   $1001.69 |     -0.05% |    0.07% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-12 |   $1001.83 |      0.01% |    0.08% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-13 |   $1002.74 |      0.04% |    0.12% | $0.00 |      0.00 |   0.00% |
;;; | 1981-01-14 |   $1002.64 |     -0.00% |    0.11% | $0.00 |      0.00 |   0.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Generate evaluation report
;; **

;; @@
(print-eval-report 30)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value |    :vol |  :r-vol |             :sharpe |           :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+---------------------+---------------------+---------+---------------|
;;; | 1981-01-02 |      $1000 | 0.0000% | 0.0000% |             0.0000% |             0.0000% |      $0 |        0.0000 |
;;; | 1981-01-05 |      $1000 | 0.0000% | 0.0000% | 4599543695833.1280% | 4599543695833.1280% |      $0 |        0.0000 |
;;; | 1981-01-06 |      $1001 | 0.0241% | 0.0241% |             2.5316% |             2.5316% |      $0 |      100.0000 |
;;; | 1981-01-07 |      $1002 | 0.0256% | 0.0256% |             4.1486% |             4.1486% |      $1 |      100.0000 |
;;; | 1981-01-08 |      $1002 | 0.0229% | 0.0229% |             5.5475% |             5.5475% |      $1 |      100.0000 |
;;; | 1981-01-09 |      $1001 | 0.0346% | 0.0346% |             2.1147% |             2.1147% |      $0 |      211.9380 |
;;; | 1981-01-12 |      $1001 | 0.0321% | 0.0321% |             2.4749% |             2.4749% |      $0 |      211.9380 |
;;; | 1981-01-13 |      $1002 | 0.0317% | 0.0317% |             3.7421% |             3.7421% |      $1 |      211.9380 |
;;; | 1981-01-14 |      $1002 | 0.0303% | 0.0303% |             3.7759% |             3.7759% |      $1 |      211.9380 |
;;; | 1981-01-15 |      $1002 | 0.0305% | 0.0305% |             3.0112% |             3.0112% |      $1 |      211.9380 |
;;; | 1981-01-16 |      $1002 | 0.0291% | 0.0291% |             3.5129% |             3.5129% |      $1 |      211.9380 |
;;; | 1981-01-19 |      $1000 | 0.0337% | 0.0337% |             1.2088% |             1.2088% |      $0 |      227.2370 |
;;; | 1981-01-20 |      $1001 | 0.0334% | 0.0334% |             2.1923% |             2.1923% |      $0 |      227.2370 |
;;; | 1981-01-21 |      $1001 | 0.0329% | 0.0329% |             1.5450% |             1.5450% |      $0 |      227.2370 |
;;; | 1981-01-22 |      $1000 | 0.0320% | 0.0320% |             1.2109% |             1.2109% |      $0 |      227.2370 |
;;; | 1981-01-23 |      $1000 | 0.0310% | 0.0310% |             1.3802% |             1.3802% |      $0 |      227.2370 |
;;; | 1981-01-26 |      $1001 | 0.0303% | 0.0303% |             2.0115% |             2.0115% |      $0 |      227.2370 |
;;; | 1981-01-27 |      $1001 | 0.0295% | 0.0295% |             2.2763% |             2.2763% |      $0 |      227.2370 |
;;; | 1981-01-28 |      $1002 | 0.0296% | 0.0296% |             3.4513% |             3.4513% |      $1 |      227.2370 |
;;; | 1981-01-29 |      $1003 | 0.0297% | 0.0297% |             4.7035% |             4.7035% |      $1 |      227.2370 |
;;; | 1981-01-30 |      $1004 | 0.0307% | 0.0307% |             6.2963% |             6.2963% |      $2 |      214.0978 |
;;; | 1981-02-02 |      $1005 | 0.0319% | 0.0319% |             7.9426% |             7.9426% |      $2 |      202.4048 |
;;; | 1981-02-03 |      $1005 | 0.0326% | 0.0326% |             6.6785% |             6.6785% |      $2 |      202.4048 |
;;; | 1981-02-04 |      $1004 | 0.0331% | 0.0331% |             5.5226% |             5.5226% |      $2 |      202.4048 |
;;; | 1981-02-05 |      $1004 | 0.0325% | 0.0325% |             5.5614% |             5.5614% |      $2 |      202.4048 |
;;; | 1981-02-06 |      $1004 | 0.0319% | 0.0319% |             5.5960% |             5.5960% |      $2 |      202.4048 |
;;; | 1981-02-09 |      $1005 | 0.0324% | 0.0324% |             7.0664% |             7.0664% |      $2 |      202.4048 |
;;; | 1981-02-10 |      $1005 | 0.0318% | 0.0318% |             7.2623% |             7.2623% |      $2 |      202.4048 |
;;; | 1981-02-11 |      $1006 | 0.0316% | 0.0316% |             8.3162% |             8.3162% |      $3 |      202.4048 |
;;; | 1981-02-12 |      $1006 | 0.0310% | 0.0315% |             8.7512% |             8.6148% |      $3 |      202.4048 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Plot variables
;; **

;; @@
(def data (deref portfolio-value))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/data</span>","value":"#'clojure-backtesting.demo/data"}
;; <=

;; @@
(def data-to-plot
 (map #(assoc % :plot "portfolio")
  data))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/data-to-plot</span>","value":"#'clojure-backtesting.demo/data-to-plot"}
;; <=

;; @@
(first data-to-plot)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:date</span>","value":":date"},{"type":"html","content":"<span class='clj-string'>&quot;1980-12-31&quot;</span>","value":"\"1980-12-31\""}],"value":"[:date \"1980-12-31\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-value</span>","value":":tot-value"},{"type":"html","content":"<span class='clj-long'>1000</span>","value":"1000"}],"value":"[:tot-value 1000]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:daily-ret</span>","value":":daily-ret"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:daily-ret 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-ret</span>","value":":tot-ret"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:tot-ret 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:loan</span>","value":":loan"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:loan 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:leverage</span>","value":":leverage"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:leverage 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:margin</span>","value":":margin"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:margin 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:plot</span>","value":":plot"},{"type":"html","content":"<span class='clj-string'>&quot;portfolio&quot;</span>","value":"\"portfolio\""}],"value":"[:plot \"portfolio\"]"}],"value":"{:date \"1980-12-31\", :tot-value 1000, :daily-ret 0.0, :tot-ret 0.0, :loan 0.0, :leverage 0.0, :margin 0.0, :plot \"portfolio\"}"}
;; <=

;; @@
(plot data-to-plot :plot :date :daily-ret false)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
