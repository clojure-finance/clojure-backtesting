(ns clojure-backtesting.core-test
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            ;;[clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojure.spec.alpha :as s]
            ))

(deftest math-calculation
  "Check functions for mathematical calculations."
  (testing 
    (is (= 3 (mean [1 2 3 4 5])))
    (is (= 7/2 (mean [1 2 3 4 5 6])))
    (is (= 4 (square 2)))
    (is (= 6.25 (square 2.5)))
    (is (= 1.0 (standard-deviation [1 2 3])))
    (is (= 1.581 (Double/parseDouble (format "%.3f" (standard-deviation [1 2 3 4 5])))))
  )
)

; (deftest dataset
;   "Check if dataset is sorted by date and ticker."
;   (testing 
    
;   )
; )

(deftest init-portfolio
  "Check the init-portfolio function."
  (testing 
    (init_portfolio "1980-12-16" 5000)
    (is (= 5000 init-capital))
    (let [tot_val (get-in (deref portfolio) [:cash :tot_val])]
      (is (= 5000 tot_val))
    )
  )
)

(deftest simple-trade-1
  "Initial capital = 5000.
  Buy 10 AAPL on 1980-12-16, sell 5 on 1980-12-17."
  (testing 
    (reset! data-set (add_aprc (read-csv-row "./resources/CRSP-extract.csv")))
    (init_portfolio "1980-12-16" 5000)
    (order_internal "1980-12-16" "AAPL" 10)
    (update-eval-report "1980-12-16")
    (order_internal "1980-12-17" "IBM" -5)
    (update-eval-report "1980-12-17")
    ;; check order record

    ;; check portfolio
    (is (= 5000 init-capital))
    ; (let [tot_val (get-in (deref portfolio) [:cash :tot_val])]
    ;   (is (= 5000 tot_val))
    ; )

    ;; check summary stat
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


