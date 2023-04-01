;; gorilla-repl.fileformat = 1

;; **
;;; ## Simple Trading Strategy Example
;; **

;; @@
; import libraries from kernel
(ns clojure-backtesting.updated_examples.simpleStrat
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
;;; ### Initialise portfolio
;; **

;; @@
;; initialise with current date and initial capital (= $10000)
(init-portfolio "1980-12-16" 10000)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1980-12-16 Cash: $10000&quot;</span>","value":"\"Date: 1980-12-16 Cash: $10000\""}
;; <=

;; **
;;; ### Write a strategy
;;; 
;;; The following code implements a simple trading strategy:
;;; 
;;; In a timespan of 10 days (inclusive of today),
;;; - Buy 50 stocks of AAPL on the first day
;;; - Sell 10 stocks of AAPL on every other day
;; **

;; **
;;; ### (1) Running the strategy
;;; - Note that you should have loaded the **extract** dataset
;; **

;; @@
;; define the "time span", i.e. to trade in the coming 10 days 
(def num-of-days (atom 10))                              

(while (pos? @num-of-days) ;; check if num-of-days is > 0
    (do 
        ;; write your trading strategy here
        (if (= 10 @num-of-days) ;; check if num-of-days == 10
            (do
                (order "14593" 50 :print true) ; buy 50 stocks
                ;; (println ((fn [date] (str "Buy 50 stocks of AAPL on " date)) (get-date)))
            )
        )
        (if (odd? @num-of-days) ;; check if num-of-days is odd
            (do
                (order "14593" -10 :print true) ; sell 10 stocks
                ;; (println ((fn [date] (str "Sell 10 stocks of AAPL on " date)) (get-date)))
            )
        )
        
        (update-eval-report) ;; update the evaluation metrics every day
        (println (get-date))
        ; move on to the next trading day
        (next-date)
        
        ; decrement counter
        (swap! num-of-days dec)
    )
)
;; @@
;; ->
;;; 1980-12-16
;;; Order: 1980-12-17 | 14593 | 50.000000.
;;; 1980-12-17
;;; Order: 1980-12-18 | 14593 | -10.000000.
;;; 1980-12-18
;;; 1980-12-19
;;; Order: 1980-12-22 | 14593 | -10.000000.
;;; 1980-12-22
;;; 1980-12-23
;;; Order: 1980-12-24 | 14593 | -10.000000.
;;; 1980-12-24
;;; 1980-12-26
;;; Order: 1980-12-29 | 14593 | -10.000000.
;;; 1980-12-29
;;; 1980-12-30
;;; Order: 1980-12-31 | 14593 | -10.000000.
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
;;; |      :date | :permno |  :price | :aprc | :quantity |
;;; |------------+---------+---------+-------+-----------|
;;; | 1980-12-17 |   14593 | 25.9375 | 25.35 |        50 |
;;; | 1980-12-18 |   14593 | 26.6875 | 25.67 |       -10 |
;;; | 1980-12-22 |   14593 | 29.6875 | 26.88 |       -10 |
;;; | 1980-12-24 |   14593 | 32.5625 | 27.98 |       -10 |
;;; | 1980-12-29 |   14593 | 36.0625 | 29.25 |       -10 |
;;; | 1980-12-31 |   14593 | 34.1875 | 28.58 |       -10 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Check portfolio record
;; **

;; @@
;; view final portfolio
(print-portfolio)
;; @@
;; ->
;;; 
;;; | :asset | :price | :aprc | :quantity | :tot-val |
;;; |--------+--------+-------+-----------+----------|
;;; |   cash |    N/A |   N/A |       N/A | 10116.11 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
;; view portfolio value and return
; pass negative value to print-portfolio-record to get all records
(print-portfolio-record -1)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-16 |  $10000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-17 |  $10000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-18 |  $10015.79 |      0.00% |    0.07% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-19 |  $10042.49 |      0.12% |    0.18% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-22 |  $10064.41 |      0.00% |    0.28% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-23 |  $10078.98 |      0.06% |    0.34% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-24 |  $10097.44 |      0.00% |    0.42% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-26 |  $10119.28 |      0.09% |    0.51% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-29 |  $10122.82 |     -0.00% |    0.53% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-30 |  $10119.71 |     -0.01% |    0.52% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-31 |  $10116.11 |      0.00% |    0.50% | $0.00 |      0.00 |   0.00% |
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
;;; |      :date | :tot-value |    :vol |  :r-vol |  :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+----------+-----------+---------+---------------|
;;; | 1980-12-17 |     $10000 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $0 |        0.0000 |
;;; | 1980-12-18 |     $10015 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $7 |        0.0000 |
;;; | 1980-12-19 |     $10042 | 0.0578% | 0.0578% |  3.1854% |   3.1854% |     $21 |      100.0000 |
;;; | 1980-12-22 |     $10064 | 0.0517% | 0.0517% |  5.3929% |   5.3929% |     $21 |      100.0000 |
;;; | 1980-12-23 |     $10078 | 0.0490% | 0.0490% |  6.9722% |   6.9722% |     $26 |      100.0000 |
;;; | 1980-12-24 |     $10097 | 0.0461% | 0.0461% |  9.1301% |   9.1301% |     $24 |      100.0000 |
;;; | 1980-12-26 |     $10119 | 0.0491% | 0.0491% | 10.4957% |  10.4957% |     $29 |      100.0000 |
;;; | 1980-12-29 |     $10122 | 0.0473% | 0.0473% | 11.2136% |  11.2136% |     $24 |      100.0000 |
;;; | 1980-12-30 |     $10119 | 0.0467% | 0.0467% | 11.0778% |  11.0778% |     $23 |      111.5205 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Plot variables
;;; Below are example codes that show how to plot different variables in the portfolio record / evaluation record.
;; **

;; **
;;; ### 1. Portfolio daily return
;; **

;; @@
(def data (deref portfolio-value))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.simpleStrat/data</span>","value":"#'clojure-backtesting.updated_examples.simpleStrat/data"}
;; <=

;; @@
; Add legend name to series
(def data-to-plot
 (map #(assoc % :plot "port-value")
  data))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.simpleStrat/data-to-plot</span>","value":"#'clojure-backtesting.updated_examples.simpleStrat/data-to-plot"}
;; <=

;; @@
(first data-to-plot)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:date</span>","value":":date"},{"type":"html","content":"<span class='clj-string'>&quot;1980-12-16&quot;</span>","value":"\"1980-12-16\""}],"value":"[:date \"1980-12-16\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-value</span>","value":":tot-value"},{"type":"html","content":"<span class='clj-long'>10000</span>","value":"10000"}],"value":"[:tot-value 10000]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:daily-ret</span>","value":":daily-ret"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:daily-ret 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-ret</span>","value":":tot-ret"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:tot-ret 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:loan</span>","value":":loan"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:loan 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:leverage</span>","value":":leverage"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:leverage 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:margin</span>","value":":margin"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:margin 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:plot</span>","value":":plot"},{"type":"html","content":"<span class='clj-string'>&quot;port-value&quot;</span>","value":"\"port-value\""}],"value":"[:plot \"port-value\"]"}],"value":"{:date \"1980-12-16\", :tot-value 10000, :daily-ret 0.0, :tot-ret 0.0, :loan 0.0, :leverage 0.0, :margin 0.0, :plot \"port-value\"}"}
;; <=

;; @@
(plot data-to-plot :plot :date :daily-ret false)
;; @@
;; ->
;;; [I 02:14:03.874 Clojupyter] oz.core:273 -- Starting up server on port 10666
;;; [I 02:14:04.159 Clojupyter] oz.server:142 -- Web server is running at `http://localhost:10666/`
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### 2. Plot volatility
;; **

;; @@
(def data (deref eval-record))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.simpleStrat/data</span>","value":"#'clojure-backtesting.updated_examples.simpleStrat/data"}
;; <=

;; @@
(first data)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:date</span>","value":":date"},{"type":"html","content":"<span class='clj-string'>&quot;1980-12-17&quot;</span>","value":"\"1980-12-17\""}],"value":"[:date \"1980-12-17\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-value</span>","value":":tot-value"},{"type":"html","content":"<span class='clj-double'>10000.0</span>","value":"10000.0"}],"value":"[:tot-value 10000.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:vol</span>","value":":vol"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:vol 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:r-vol</span>","value":":r-vol"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:r-vol 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:sharpe</span>","value":":sharpe"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:sharpe 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:r-sharpe</span>","value":":r-sharpe"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:r-sharpe 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:pnl-pt</span>","value":":pnl-pt"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:pnl-pt 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:max-drawdown</span>","value":":max-drawdown"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:max-drawdown 0.0]"}],"value":"{:date \"1980-12-17\", :tot-value 10000.0, :vol 0.0, :r-vol 0.0, :sharpe 0.0, :r-sharpe 0.0, :pnl-pt 0.0, :max-drawdown 0.0}"}
;; <=

;; @@
; Add legend name to series
(def data-to-plot
 (map #(assoc % :plot "volatility")
  data))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.simpleStrat/data-to-plot</span>","value":"#'clojure-backtesting.updated_examples.simpleStrat/data-to-plot"}
;; <=

;; @@
(print-eval-report)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value |    :vol |  :r-vol |  :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+----------+-----------+---------+---------------|
;;; | 1980-12-17 |     $10000 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $0 |        0.0000 |
;;; | 1980-12-18 |     $10015 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $7 |        0.0000 |
;;; | 1980-12-19 |     $10042 | 0.0578% | 0.0578% |  3.1854% |   3.1854% |     $21 |      100.0000 |
;;; | 1980-12-22 |     $10064 | 0.0517% | 0.0517% |  5.3929% |   5.3929% |     $21 |      100.0000 |
;;; | 1980-12-23 |     $10078 | 0.0490% | 0.0490% |  6.9722% |   6.9722% |     $26 |      100.0000 |
;;; | 1980-12-24 |     $10097 | 0.0461% | 0.0461% |  9.1301% |   9.1301% |     $24 |      100.0000 |
;;; | 1980-12-26 |     $10119 | 0.0491% | 0.0491% | 10.4957% |  10.4957% |     $29 |      100.0000 |
;;; | 1980-12-29 |     $10122 | 0.0473% | 0.0473% | 11.2136% |  11.2136% |     $24 |      100.0000 |
;;; | 1980-12-30 |     $10119 | 0.0467% | 0.0467% | 11.0778% |  11.0778% |     $23 |      111.5205 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(plot data-to-plot :plot :date :vol true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### 3. Plot sharpe ratio
;; **

;; @@
(def data-to-plot
 (map #(assoc % :plot "sharpe ratio")
  data))

(plot data-to-plot :plot :date :sharpe true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### 4. Plot stock price
;; **

;; @@
(def data (deref order-record))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.simpleStrat/data</span>","value":"#'clojure-backtesting.updated_examples.simpleStrat/data"}
;; <=

;; @@
(first data)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:date</span>","value":":date"},{"type":"html","content":"<span class='clj-string'>&quot;1980-12-17&quot;</span>","value":"\"1980-12-17\""}],"value":"[:date \"1980-12-17\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:permno</span>","value":":permno"},{"type":"html","content":"<span class='clj-string'>&quot;14593&quot;</span>","value":"\"14593\""}],"value":"[:permno \"14593\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:price</span>","value":":price"},{"type":"html","content":"<span class='clj-double'>25.9375</span>","value":"25.9375"}],"value":"[:price 25.9375]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:aprc</span>","value":":aprc"},{"type":"html","content":"<span class='clj-string'>&quot;25.35&quot;</span>","value":"\"25.35\""}],"value":"[:aprc \"25.35\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:quantity</span>","value":":quantity"},{"type":"html","content":"<span class='clj-long'>50</span>","value":"50"}],"value":"[:quantity 50]"}],"value":"{:date \"1980-12-17\", :permno \"14593\", :price 25.9375, :aprc \"25.35\", :quantity 50}"}
;; <=

;; @@
(def data-to-plot
 (map #(assoc % :plot "price")
  data))

(plot data-to-plot :plot :date :price true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=
