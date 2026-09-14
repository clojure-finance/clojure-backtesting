;; gorilla-repl.fileformat = 1

;; **
;;; ## Bad Strategy Example
;;; 
;;; Short 50 shares of AAA on the first day and hold the short for ten trading days, through its dividend on 1990-02-15, which a short position has to pay. The point is to see how a losing position shows up in the records: negative quantity and holding value, the borrowed stock under `:short`, and the margin (equity over gross position value) moving with the price.
;; **

;; @@
(ns clojure-backtesting.examples.bad-strategy
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
(init-portfolio "1990-02-05" 5000)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1990-02-05 Cash: $5000&quot;</span>","value":"\"Date: 1990-02-05 Cash: $5000\""}
;; <=

;; @@
(order "10001" -50)
(update-eval-report)
(dotimes [_ 10]
  (println (next-date))
  (update-eval-report))
(end-order)
;; @@
;; ->
;;; 1990-02-06
;;; 1990-02-07
;;; 1990-02-08
;;; 1990-02-09
;;; 1990-02-12
;;; 1990-02-13
;;; 1990-02-14
;;; 1990-02-15
;;; 1990-02-16
;;; 1990-02-19
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-boolean'>true</span>","value":"true"}
;; <=

;; @@
(print-order-record)
;; @@
;; ->
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1990-02-06 |   10001 |  48.91 | 48.91 |     -50.0 |
;;; | 1990-02-19 |   10001 |  47.42 | 47.91 |   50.5125 |
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
;;; |   cash |    N/A |   N/A |       N/A |  5050.20 |
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
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan |   :short | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+----------+-----------+---------|
;;; | 1990-02-05 |   $5000.00 |      0.00% |    0.00% | $0.00 |    $0.00 |      0.00 | 100.00% |
;;; | 1990-02-06 |   $5000.00 |      0.00% |    0.00% | $0.00 | $2445.50 |      0.49 | 204.46% |
;;; | 1990-02-07 |   $5042.50 |      0.85% |    0.85% | $0.00 | $2403.00 |      0.48 | 209.84% |
;;; | 1990-02-08 |   $5070.50 |      0.55% |    1.40% | $0.00 | $2375.00 |      0.47 | 213.49% |
;;; | 1990-02-09 |   $5031.00 |     -0.78% |    0.62% | $0.00 | $2414.50 |      0.48 | 208.37% |
;;; | 1990-02-12 |   $5051.00 |      0.40% |    1.01% | $0.00 | $2394.50 |      0.47 | 210.94% |
;;; | 1990-02-13 |   $5045.00 |     -0.12% |    0.90% | $0.00 | $2400.50 |      0.48 | 210.16% |
;;; | 1990-02-14 |   $4992.00 |     -1.06% |   -0.16% | $0.00 | $2453.50 |      0.49 | 203.46% |
;;; | 1990-02-15 |   $4981.50 |     -0.21% |   -0.37% | $0.00 | $2464.00 |      0.49 | 202.17% |
;;; | 1990-02-16 |   $5008.78 |      0.55% |    0.18% | $0.00 | $2436.72 |      0.49 | 205.55% |
;;; | 1990-02-19 |   $5050.20 |      0.00% |    1.00% | $0.00 |   $-0.00 |      0.00 | 100.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(print-eval-report -1)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value |    :vol |  :r-vol | :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+---------+-----------+---------+---------------|
;;; | 1990-02-06 |      $5000 | 0.0000% | 0.0000% |  0.0000 |    0.0000 |      $0 |        0.0000 |
;;; | 1990-02-07 |      $5042 | 0.4887% | 0.4887% |  9.1652 |    9.1652 |     $42 |        0.0000 |
;;; | 1990-02-08 |      $5070 | 0.4215% | 0.4215% | 13.1838 |   13.1838 |     $70 |        0.0000 |
;;; | 1990-02-09 |      $5031 | 0.6242% | 0.6242% |  3.1440 |    3.1440 |     $31 |        0.7790 |
;;; | 1990-02-12 |      $5051 | 0.5693% | 0.5693% |  4.7164 |    4.7164 |     $51 |        0.7790 |
;;; | 1990-02-13 |      $5045 | 0.5310% | 0.5310% |  3.8268 |    3.8268 |     $45 |        0.7790 |
;;; | 1990-02-14 |      $4992 | 0.6457% | 0.6457% | -0.4921 |   -0.4921 |     $-8 |        1.5482 |
;;; | 1990-02-15 |      $4981 | 0.6073% | 0.6073% | -1.0766 |   -1.0766 |    $-18 |        1.7553 |
;;; | 1990-02-16 |      $5008 | 0.6019% | 0.6019% |  0.4625 |    0.4625 |      $8 |        1.7553 |
;;; | 1990-02-19 |      $5050 | 0.6206% | 0.6206% |  2.3229 |    2.3229 |     $50 |        1.7553 |
;;; | 1990-02-19 |      $5050 | 0.5711% | 0.5711% |  0.4432 |    0.4432 |     $25 |        1.7553 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=
