(ns clojure-backtesting.core-test
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.large-data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [clojure.core.matrix.stats :as stat]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojure.spec.alpha :as s]
            ))

(deftest math-calculation
  "Check functions for mathematical calculations."
  (testing 
    (is (= 1.0 (log-10 10)))
    (is (= 6.25 (square 2.5)))
    (is (= 1.0 (stat/sd [1 2 3])))
    (is (= 1.581 (Double/parseDouble (format "%.3f" (stat/sd [1 2 3 4 5])))))
  )
)

; (deftest dataset
;   "Check if dataset is sorted by date and ticker."
;   (testing 
    
;   )
; )

(deftest init-test
  "Check the init-portfolio and date functions."
  (testing 
    (init-portfolio "1980-12-16" 5000)
    (is (= "1980-12-16" (get-date)))
    (is (= 5000 init-capital))
    (let [tot-val (get-in (deref portfolio) [:cash :tot-val])]
      (is (= 5000 tot-val))
    )
    (next-date)
    (is (= "1980-12-17" (get-date)))
  )
)

(deftest simple-trade-1
  "Initial capital = 5000.
  Buy 10 AAPL on 1980-12-17, sell 5 on 1980-12-18."
  (testing 
    (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
    (init-portfolio "1980-12-16" 5000)
    (order "AAPL" 10)
    (update-eval-report (get-date))
    (next-date)
    (order "AAPL" -5)
    (update-eval-report (get-date))

    ;; check order record
    (pprint/print-table (deref order-record))

    ;; check portfolio
    (is (= 5000 init-capital))
    (let [tot-val (get-in (deref portfolio) [:cash :tot-val])]
       (is (= 4872.0 (Math/ceil tot-val)))
    )

    ;; check summary stat
    (eval-report -1)

    ; [total-val (portfolio-total)
    ; daily-ret (portfolio-daily-ret)
    ; total-ret (portfolio-total-ret)
    ; volatility (volatility)
    ; sharpe-ratio (sharpe-ratio)
    ; annualised-ret (annualised-return)
    ; annualised-vol (annualised-volatility)
    ; annualised-sharpe (annualised-sharpe-ratio)
    ; pnl-per-trade (pnl-per-trade)
    ; ]
  )
)

;; (deftest a-test
;;   (testing "FIXME, I fail."
;;     (reset! data-set (add-aprc (read-csv-row "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv")))
;;     ; (println (take 20 (deref data-set)))
;;     (init-portfolio "1980-12-16" 5000)
;;     (println get-date)
;;     (is (= "1980-12-16" (get-date)))
;;     (is (= "1980-12-17" (next-date)))
;; ))
