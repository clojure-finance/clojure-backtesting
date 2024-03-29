(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clj-time.core :as clj-t]
            [clojure.pprint :as pprint]
            [clojure.core.matrix.stats :as stat]
            [clojure.java.io :as io]
            ))

          
;; Configuration
(def ROLLING-TIME-WINDOW (atom 30))

;; Helper functions
(defn square
  [n]
  (* n n))

;; ============ Evaluation metrics calculation ============

;; Get current portfolio total value
(defn portfolio-total 
  "This function returns the current total value of the portfolio."
  []
  (get-in (last (deref portfolio-value)) [:tot-value]))

;; Get current portfolio total return
(defn portfolio-total-ret
  "This function returns the current total return of the portfolio."
  []  
  (get (last (deref portfolio-value)) :tot-ret))

(defn portfolio-daily-ret
  "This function returns the current daily return of the portfolio."
  []
  (get-in (last (deref portfolio-value)) [:daily-ret]))

;; Get list of daily returns
(defn get-daily-returns
  "This function returns a collection of daily returns from 'portfolio-value'."
  []
  (map :daily-ret (deref portfolio-value)))


;; Volatility (in %)
(defn volatility
  "This function returns the volatility of the portfolio in %."
  []
  (stat/sd (get-daily-returns)))

;; Volatility, with rolling time window (in %)
(defn rolling-volatility
  "This function returns the volatility of the portfolio in %."
  []
  (stat/sd (take-last (deref ROLLING-TIME-WINDOW) (get-daily-returns))))

;; Faster calculation of s.d., but only an estimation (based on Welford's online algorithm)
(defn volatility-optimised
  "This function returns the volatility of the portfolio in %."
  []
  (if (= (count (deref eval-record)) 0)
    (* (stat/sd (get-daily-returns)) 100)
    (let [prev-vol (get-in (last (deref eval-record)) [:vol])
          prev-vol-square (square prev-vol)
          x-n (get-in (last (deref portfolio-value)) [:daily-ret])
          curr-mean (/ (get-in (last (deref portfolio-value)) [:tot-ret]) (count (deref portfolio-value)))
          prev-mean (/ (get-in (first (take-last 2 (deref portfolio-value))) [:tot-ret]) (- (count (deref portfolio-value)) 1))
          n (count (deref portfolio-value))
          numerator (- (* (- x-n prev-mean) (- x-n curr-mean)) prev-vol-square)
         ]
      (Math/sqrt (+ prev-vol-square (/ numerator n)))
    )))

;; Sharpe ratio (in %)
(defn sharpe-ratio
  "This function returns the sharpe ratio of the portfolio in %."
  []
  (let [vol (volatility)]
    (if (not= vol 0.0)
      (/ (portfolio-total-ret) vol)
      0.0)))

;; Sharpe ratio, with rolling time window (in %)
(defn rolling-sharpe-ratio
  "This function returns the rolling sharpe ratio of the portfolio in %."
  []
  (let [vol (rolling-volatility)]
    (if (= vol 0.0)
      0.0
      (/ (portfolio-total-ret) vol))))

;; PnL per trade (in $)
(defn pnl-per-trade
  "This function returns the profit/loss per trade in dollars."
  []
  (/ (- (portfolio-total) init-capital) (count (deref order-record))))

;; Maximum drawdown (in %)
(defn max-drawdown
  "This function returns the maximum drawdown."
  []
  (let [max-ret (apply max (get-daily-returns))
        min-ret (apply min (get-daily-returns))
       ]
    (if (= max-ret 0.0)
      0.0
      (/ (- max-ret min-ret) max-ret))))


;; ============ Update configuraion & evaluation metrics ============

;; Update rolling time window
(defn update-rolling-window
  "This functions updates the time window for rolling functions."
  [n]
  (reset! ROLLING-TIME-WINDOW n)
  (println (str "Time window is updated as " n "."))) ; success message

;; Update evaluation report
(defn update-eval-report
  "This function updates the evaluation report."
  []
  (when (and (not= (count (deref order-record)) 0) (not (deref TERMINATED))) ; check that order record is not empty
    (let [total-val-data (portfolio-total)
          volatility-data (volatility)
          rolling-volatility-data (rolling-volatility)
          sharpe-ratio-data (sharpe-ratio)
          rolling-sharpe-ratio-data (rolling-sharpe-ratio)
          pnl-per-trade-data (pnl-per-trade)
          max-drawdown-data (max-drawdown)
          date (get-date)
         ]
       ; numerical values
        (swap! eval-record conj {:date date
                                 :tot-value total-val-data
                                 :vol volatility-data
                                 :r-vol rolling-volatility-data
                                 :sharpe sharpe-ratio-data
                                 :r-sharpe rolling-sharpe-ratio-data
                                 :pnl-pt pnl-per-trade-data
                                 :max-drawdown max-drawdown-data
                                 })
        ; string formatting
        (swap! eval-report-data conj {:date date
                                      :tot-value (str "$" (int total-val-data))
                                      :vol (str (format "%.4f" (* volatility-data 100)) "%")
                                      :r-vol (str (format "%.4f" (* rolling-volatility-data 100)) "%")
                                      :sharpe (str (format "%.4f" sharpe-ratio-data) "%")
                                      :r-sharpe (str (format "%.4f" rolling-sharpe-ratio-data) "%")
                                      :pnl-pt (str "$" (int pnl-per-trade-data))
                                      :max-drawdown (str (format "%.4f" (* max-drawdown-data 100)))
                                      })
        ; output to file
        (.write evalreport-wrtr (format "%s,%f,%f,%f,%f,%f,%f,%f\n" date (double total-val-data) (double volatility-data) (double rolling-volatility-data) (double sharpe-ratio-data) (double rolling-sharpe-ratio-data) (double pnl-per-trade-data) (double max-drawdown-data)))
      )))
