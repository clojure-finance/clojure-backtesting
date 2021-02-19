(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clj-time.core :as clj-t]
            [clojure.pprint :as pprint]
            [clojure.core.matrix.stats :as stat]
            [clojure.java.io :as io]
            ))


;; Get current portfolio total value
(defn portfolio-total 
  "This function returns the current total value of the portfolio."
  []
  (get-in (last (deref portfolio-value)) [:tot-value])
)

;; Calculate portfolio total returns
(defn portfolio-total-ret
  "This function returns the current daily return of the portfolio in decimal."
  []  
  (get (last (deref portfolio-value)) :tot-ret)
)


;; Helper function
(defn square
  [n]
  (* n n))

;; Helper function
;
; input: a collection
; output: integer / fraction (if rational)
(defn mean
  [coll]
  (/ (reduce + coll) (count coll)))

;; Helper function
;
; input: a collection
; output: sample standard deviation, float
(defn standard-deviation
  [coll]
  (if (= (compare (- (count coll) 1) 1) -1) ; check division by zero
    0.0
    (Math/sqrt (/ (reduce + (map square (map - coll (repeat (mean coll)))))
      (- (count coll) 1)))
  )
)
  

;; Get list of daily returns
(defn get-daily-returns
  "This function returns a collection of daily returns from 'portfolio-value'."
  []
  (def dailyret-record (atom []))
  (doseq [daily-record (deref portfolio-value)] ;; then update the price & aprc of the securities in the portfolio
    (let [daily-ret (daily-record :daily-ret)]
      ;(println daily-ret)
      (swap! dailyret-record conj daily-ret)
    )
  )
  dailyret-record ; can be refactoring no need to create atom
)

;; Volatility (in %)
(defn volatility
  "This function returns the volatility of the portfolio in %."
  []
  (stat/sd (deref (get-daily-returns)))
  ;(* (standard-deviation (deref (get-daily-returns))) 100)
) 

(defn volatility-optimised
  "This function returns the volatility of the portfolio in %."
  []
  (if (= (count (deref eval-record)) 0)
    (* (stat/sd (deref (get-daily-returns))) 100)
    (let [prev-vol (get-in (last (deref eval-record)) [:vol])
          prev-vol-square (square prev-vol)
          x-n (get-in (last (deref portfolio-value)) [:daily-ret])
          curr-mean (/ (get-in (last (deref portfolio-value)) [:tot-ret]) (count (deref portfolio-value)))
          prev-mean (/ (get-in (first (take-last 2 (deref portfolio-value))) [:tot-ret]) (- (count (deref portfolio-value)) 1))
          n (count (deref portfolio-value))
          numerator (- (* (- x-n prev-mean) (- x-n curr-mean)) prev-vol-square)
         ]
      (Math/sqrt (+ prev-vol-square (/ numerator n)))
    ) 
  )
)

;; Sharpe ratio (in %)
(defn sharpe-ratio
  "This function returns the sharpe ratio of the portfolio in %."
  []
  (let [vol (volatility)]
    (if (not= vol 0.0)
      (/ (portfolio-total-ret) vol)
      0.0
    )
  )
)

;; PnL per trade (in $)
(defn pnl-per-trade
  "This function returns the profit/loss per trade in dollars."
  []
  (/ (- (portfolio-total) init-capital) (count (deref order-record)))
)

;; Update evaluation report
(defn update-eval-report
  "This function updates the evaluation report."
  [date]
  (if (not= (count (deref order-record)) 0) ; check that order record is not empty
    (let [total-val-data (portfolio-total)
          volatility-data (volatility)
          sharpe-ratio-data (sharpe-ratio)
          pnl-per-trade-data (pnl-per-trade)
         ]
      (do
        ; numerical values
        (swap! eval-record conj {:date date
                                 :tot-value total-val-data
                                 :vol volatility-data
                                 :sharpe sharpe-ratio-data
                                 :pnl-pt pnl-per-trade-data})
        
        ; string formatting
        (swap! eval-report-data conj {:date date
                                      :tot-value (str "$" (int total-val-data))
                                      :vol (str (format "%.4f" (* volatility-data 100)) "%")
                                      :sharpe (str (format "%.4f" sharpe-ratio-data) "%")
                                      :pnl-pt (str "$" (int pnl-per-trade-data))})
        ; output to file
        (.write evalreport-wrtr (format "%s,%f,%f,%f,%f\n" date (double total-val-data) (double volatility-data) (double sharpe-ratio-data) (double pnl-per-trade-data)))
      )
    )
  )
)

;; Print evaluation report
(defn eval-report
  "This function prints the first n rows of the evaluation report, pass a -ve number to print full report."
  [& [n]]
  (if (> n 0)
    (pprint/print-table (take n (deref eval-report-data)))
    (pprint/print-table (deref eval-report-data))
  )
)


