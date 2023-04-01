;; gorilla-repl.fileformat = 1

;; **
;;; ## Buying on margin example
;; **

;; @@
; import libraries from kernel
(ns clojure-backtesting.examples.buy-on-margin
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
;;; ### Trade without leverage
;;; 
;;; The trade would be allowed if you possess enough cash to pay.
;; **

;; @@
(init-portfolio "1980-12-15" 400)
(order "28636" 10 :leverage false :remaining true :print true) ;without leverage, remaining value

(next-date)
(next-date)
(next-date)

(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
;; @@
;; ->
;;; Order: 1980-12-16 | 28636 | 10.000000.
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1980-12-16 |   28636 |  7.375 | 22.18 |        10 |
;;; 
;;; | :asset | :price |   :aprc | :quantity | :tot-val |
;;; |--------+--------+---------+-----------+----------|
;;; |   cash |    N/A |     N/A |       N/A |   178.23 |
;;; |  28636 |  7.125 | 21.8473 |        10 |   218.47 |
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-15 |    $400.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-16 |    $400.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-17 |    $398.36 |     -0.18% |   -0.18% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-18 |    $396.70 |     -0.18% |   -0.36% | $0.00 |      0.00 |   0.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; However, the trade would not be allowed if you do not have sufficient cash.
;; **

;; @@
(init-portfolio "1980-12-15" 100)
(order "28636" 10 :leverage false :remaining true :print true) ;without leverage, remaining value

(next-date)
(next-date)
(next-date)

(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
;; @@
;; ->
;;; Order request 1980-12-16 | 28636 | 10 fails.
;;; Failure reason: You do not have enough money to buy or have enough stock to sell. Try to solve by enabling leverage.
;;; 
;;; | :asset | :price | :aprc | :quantity | :tot-val |
;;; |--------+--------+-------+-----------+----------|
;;; |   cash |    N/A |   N/A |       N/A |   100.00 |
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-15 |    $100.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Trade with leverage
;; **

;; @@
(init-portfolio "1980-12-15" 20)

(order "28636" 1 :print true) ;with leverage, exact value trade

(next-date)
(next-date)
(next-date)

(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
;; @@
;; ->
;;; Order: 1980-12-16 | 28636 | 1.000000.
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1980-12-16 |   28636 |  7.375 | 22.18 |         1 |
;;; 
;;; | :asset | :price |   :aprc | :quantity | :tot-val |
;;; |--------+--------+---------+-----------+----------|
;;; |   cash |    N/A |     N/A |       N/A |    -2.18 |
;;; |  28636 |  7.125 | 21.8473 |         1 |    21.85 |
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-15 |     $20.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-16 |     $20.00 |      0.00% |    0.00% | $2.18 |      0.11 |  90.18% |
;;; | 1980-12-17 |     $19.84 |     -0.04% |   -0.04% | $2.18 |      0.11 |  90.11% |
;;; | 1980-12-18 |     $19.67 |     -0.04% |   -0.08% | $2.18 |      0.11 |  90.04% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Initial margin
;; **

;; **
;;; The purchase would not be allowed if the ratio of cash to total value of assets bought on margin goes below the initial margin.
;; **

;; @@
; check variable
(println INITIAL-MARGIN)
;; @@
;; ->
;;; 0.5
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; **Example**: order failed, since cash is insufficient.
;; **

;; @@
(init-portfolio "1980-12-15" 100)
(order "28636" 10 :remaining true :print true) ;with leverage, remaining value

(next-date)
(next-date)
(next-date)

(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
;; @@
;; ->
;;; Order request 1980-12-16 | 28636 | 10 fails due to initial margin requirement.
;;; 
;;; | :asset | :price | :aprc | :quantity | :tot-val |
;;; |--------+--------+-------+-----------+----------|
;;; |   cash |    N/A |   N/A |       N/A |   100.00 |
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-15 |    $100.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; Alternatively, you could manually update the initial margin to enable such a case. You could set it to `nil` in order to disable the initial margin requirement.
;; **

;; @@
(update-initial-margin 0.1)
(println INITIAL-MARGIN)
;; @@
;; ->
;;; 0.1
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(init-portfolio "1980-12-15" 100)
(order "28636" 10 :remaining true :print true) ;with leverage, remaining value

(next-date)
(next-date)
(next-date)

(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
;; @@
;; ->
;;; Order: 1980-12-16 | 28636 | 10.000000.
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1980-12-16 |   28636 |  7.375 | 22.18 |        10 |
;;; 
;;; | :asset | :price |   :aprc | :quantity | :tot-val |
;;; |--------+--------+---------+-----------+----------|
;;; |   cash |    N/A |     N/A |       N/A |  -121.77 |
;;; |  28636 |  7.125 | 21.8473 |        10 |   218.47 |
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret |   :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+---------+-----------+---------|
;;; | 1980-12-15 |    $100.00 |      0.00% |    0.00% |   $0.00 |      0.00 |   0.00% |
;;; | 1980-12-16 |    $100.00 |      0.00% |    0.00% | $121.77 |      1.22 |  45.09% |
;;; | 1980-12-17 |     $98.36 |     -0.89% |   -0.89% | $121.77 |      1.24 |  44.68% |
;;; | 1980-12-18 |     $96.70 |     -0.93% |   -1.82% | $121.77 |      1.26 |  44.26% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Maintenance margin
;; **

;; **
;;; All positions will be automatically closed if the portfolio margin goes below the maintenace margin.
;; **

;; @@
(init-portfolio "1980-12-15" 100)
(order "28636" -10 :remaining true :print true) ;with leverage, remaining value
(order "25785" 20 :remaining true :print true)

(next-date)
(next-date)
(next-date)

(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
;; @@
;; ->
;;; Order request 1980-12-16 | 25785 | 20 fails due to initial margin requirement.
;;; Order: 1980-12-16 | 28636 | -10.000000.
;;; 
;;; |      :date | :permno | :price | :aprc | :quantity |
;;; |------------+---------+--------+-------+-----------|
;;; | 1980-12-16 |   28636 |  7.375 | 22.18 |       -10 |
;;; 
;;; | :asset | :price |   :aprc | :quantity | :tot-val |
;;; |--------+--------+---------+-----------+----------|
;;; |   cash |    N/A |     N/A |       N/A |   321.77 |
;;; |  28636 |  7.125 | 21.8473 |       -10 |  -218.47 |
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1980-12-15 |    $100.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-16 |    $100.00 |     -0.00% |   -0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-17 |    $101.64 |      0.71% |    0.71% | $0.00 |      0.00 |   0.00% |
;;; | 1980-12-18 |    $103.30 |      0.70% |    1.41% | $0.00 |      0.00 |   0.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
