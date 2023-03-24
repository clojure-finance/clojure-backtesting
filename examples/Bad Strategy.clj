;; gorilla-repl.fileformat = 1

;; **
;;; ## Bad Strategy Example
;; **

;; @@
; import libraries from kernel
(ns clojure-backtesting.updated_examples.badstrat
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
(load-dataset "./CRSP" "main" add-aprc)
(init-portfolio "1980-12-15" 2000)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1980-12-15 Cash: $2000&quot;</span>","value":"\"Date: 1980-12-15 Cash: $2000\""}
;; <=

;; @@
;; (reset! data-set (add-aprc (read-csv-row "../resources/CRSP-extract.csv")))

(order "28636" -100)
(update-eval-report) ; update-eval-report does not take any arguments
(println (next-date))
(update-eval-report)
(println (next-date))
(update-eval-report)
(println (next-date))
(println (next-date))
(next-date)
(next-date)
(update-eval-report)

(end-order)
;; @@
;; ->
;;; 1980-12-16
;;; 1980-12-17
;;; 1980-12-18
;;; 1980-12-19
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>true</span>","value":"true"}
;; <=

;; @@
(print-order-record)
;; @@
;; ->
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1980-12-16 |   28636 |  7.375 | 22.18 |      -100 |
;;; | 1980-12-23 |   28636 |  7.125 | 21.85 |       100 |
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
;;; |   cash |    N/A |   N/A |       N/A |  2032.97 |
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
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-15 |   $2000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-16 |   $2000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-17 |   $2016.40 |      0.35% |    0.35% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-18 |   $2032.97 |      0.36% |    0.71% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-19 |   $2049.70 |      0.36% |    1.07% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-22 |   $2049.70 |      0.00% |    1.07% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-23 |   $2032.97 |      0.00% |    0.71% | $0.00 |      0.00 |   0.00% |
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
;;; | 1980-12-16 |      $2000 | 0.0000% | 0.0000% | 0.0000% |   0.0000% |      $0 |        0.0000 |
;;; | 1980-12-17 |      $2016 | 0.2048% | 0.2048% | 1.7321% |   1.7321% |     $16 |      100.0000 |
;;; | 1980-12-23 |      $2032 | 0.2688% | 0.2688% | 2.6419% |   2.6419% |     $32 |      199.9984 |
;;; | 1980-12-23 |      $2032 | 0.1899% | 0.1899% | 3.7385% |   3.7385% |     $16 |      100.0000 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
