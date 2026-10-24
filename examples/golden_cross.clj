;; gorilla-repl.fileformat = 1

;; **
;;; ## Golden Cross Example
;; **

;; @@
(ns clojure-backtesting.examples.golden-cross
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
;;; The following code implements the golden cross rule on two securities:
;;; 
;;; - MA 15 above MA 30 (golden cross): hold a position
;;; - MA 15 below MA 30 (death cross): sell everything
;;; 
;;; `moving-avg` returns nil until 30 closes are available, so the strategy does nothing for the first 30 trading days.
;; **

;; @@
(defn golden-cross [permno quantity]
  (let [ma15 (moving-avg permno 15)
        ma30 (moving-avg permno 30)]
    (when (and ma15 ma30)
      (if (> ma15 ma30)
        (order permno quantity :print false)
        (order permno 0 :remaining true)))))

(while (< (compare (get-date) "1990-03-29") 0)
  (golden-cross "10001" 10)
  (golden-cross "10002" 5)
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
;;; | 1990-02-13 |   10002 | 128.45 | 128.45 |       5.0 |
;;; | 1990-02-14 |   10002 | 129.49 | 129.49 |       5.0 |
;;; | 1990-02-15 |   10002 | 129.32 | 129.32 |       5.0 |
;;; | 1990-02-16 |   10002 |  131.6 | 131.60 |       5.0 |
;;; | 1990-02-19 |   10002 | 132.31 | 132.31 |       5.0 |
;;; | 1990-02-20 |   10002 |  134.2 | 134.20 |       5.0 |
;;; | 1990-02-21 |   10002 | 135.08 | 135.08 |       5.0 |
;;; | 1990-02-22 |   10002 | 133.11 | 133.11 |       5.0 |
;;; | 1990-02-23 |   10002 | 133.44 | 133.44 |       5.0 |
;;; | 1990-02-26 |   10002 | 131.03 | 131.03 |       5.0 |
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
;;; |   cash |    N/A |   N/A |       N/A |  9960.90 |
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
;;; | 1990-02-13 |     $10000 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |      $0 |       0.0000% |
;;; | 1990-02-14 |     $10005 | 0.0092% | 0.0095% |  2.8062 |    2.8983 |      $2 |       0.0000% |
;;; | 1990-02-15 |     $10003 | 0.0096% | 0.0101% |  1.7520 |    1.8358 |      $1 |       0.0170% |
;;; | 1990-02-16 |     $10037 | 0.0591% | 0.0629% |  2.9721 |    3.1653 |      $9 |       0.0170% |
;;; | 1990-02-19 |     $10051 | 0.0623% | 0.0671% |  3.7710 |    4.0829 |     $10 |       0.0170% |
;;; | 1990-02-20 |     $10099 | 0.0974% | 0.1062% |  4.4646 |    4.9169 |     $16 |       0.0170% |
;;; | 1990-02-21 |     $10125 | 0.1035% | 0.1138% |  5.1732 |    5.8009 |     $17 |       0.0170% |
;;; | 1990-02-22 |     $10056 | 0.1547% | 0.1746% |  1.5236 |    1.7108 |      $7 |       0.6810% |
;;; | 1990-02-23 |     $10069 | 0.1538% | 0.1757% |  1.8405 |    2.0946 |      $7 |       0.6810% |
;;; | 1990-02-26 |      $9961 | 0.2310% | 0.2678% | -0.6654 |   -0.7653 |     $-3 |       1.6216% |
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
(first data-to-plot)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:plot &quot;portfolio&quot;, :date &quot;1990-01-02&quot;, :leverage 0.0, :short 0.0, :tot-ret 0.0, :loan 0.0, :tot-value 10000, :daily-ret 0.0, :margin 1.0}</span>","value":"{:plot \"portfolio\", :date \"1990-01-02\", :leverage 0.0, :short 0.0, :tot-ret 0.0, :loan 0.0, :tot-value 10000, :daily-ret 0.0, :margin 1.0}"}
;; <=

;; @@
(plot data-to-plot :plot :date :daily-ret true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=
