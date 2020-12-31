(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            [clj-time.core :as clj-t]
            [clojure.pprint :as pprint]))


;; Get current portfolio total value
(defn portfolio-total 
  "This function returns the current total value of the portfolio."
  []
  (get-in (last (deref portfolio-value)) [:tot-value])
)

;; Get current portfolio daily return (in %)
(defn portfolio-daily-ret 
  "This function returns the current daily return of the portfolio in %."
  []
  (* (get-in (last (deref portfolio-value)) [:daily-ret]) 100)
)

;; Calculate portfolio total returns
(defn portfolio-total-ret
  "This function returns the current daily return of the portfolio in %."
  []
  (def total-ret 0.0)
  (doseq [daily-record (deref portfolio-value)]
    (let [daily-ret (daily-record :daily-ret)]
      ;(println daily-ret)
      (def total-ret (+ total-ret daily-ret))
    )
  )
  total-ret
)

;; Calculate number of days between first and last dates in order record
(defn num-of-tradays
  "This function calculates the annualised return of the portfolio." 
  []
  (let [first-date (get (first (deref order-record)) :date)
          last-date (get (last (deref order-record)) :date)]

    (def first-year (Integer/parseInt (subs first-date 0 4)))
    (def first-month (Integer/parseInt (subs first-date 5 7)))
    (def first-day (Integer/parseInt (subs first-date 8 10)))
    (def last-year (Integer/parseInt (subs last-date 0 4)))
    (def last-month (Integer/parseInt (subs last-date 5 7)))
    (def last-day (Integer/parseInt (subs last-date 8 10)))

    (def interval-min (clj-t/in-minutes (clj-t/interval (clj-t/date-time first-year first-month first-day) (clj-t/date-time last-year last-month last-day))))
    (/ (/ interval-min 60) 24)
  )
)

;; Return calculated with rolling fixed window of 1 year (in %)
(defn rolling-return
  "This function calculates the return of the portfolio, with a rolling 
  fixed window of 1 year." 
  []
  (def tradays (num-of-tradays))
  (if (= tradays 0) (def tradays 1))
  ;(println tradays)
  (- (Math/pow (+ 1 (/ (portfolio-total-ret) 100)) (/ 252 tradays)) 1)
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
      (swap! dailyret-record concat [daily-ret])
    )
  )
  dailyret-record
)

;; Volatility (in %)
(defn volatility
  "This function returns the volatility of the portfolio in %."
  []
  (* (standard-deviation (deref (get-daily-returns))) 100)
) 

;; Sharpe ratio (in %)
(defn sharpe-ratio
  "This function returns the sharpe ratio of the portfolio in %."
  []
  (if (not= (volatility) 0.0)
    (/ (* (portfolio-total-ret) 100) (volatility))
    0.0
  )
)

;; Volatility, rolling window (in %)
(defn rolling-volatility
  "This function returns the volatility of the portfolio in %, calculated with a
  rolling fixed window of 1 year.."
  []
  (* (Math/sqrt 252) (standard-deviation (deref (get-daily-returns))))
) 

;; Sharpe ratio, rolling window (in %)
(defn rolling-sharpe-ratio
  "This function returns the sharpe ratio in %, calculated with a
  rolling fixed window of 1 year."
  []
  (if (not= (rolling-volatility) 0.0)
    (/ (rolling-return) (rolling-volatility))
    0.0
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
  (let [total-val-data (portfolio-total)
        daily-ret-data (portfolio-daily-ret)
        total-ret-data (portfolio-total-ret)
        volatility-data (volatility)
        sharpe-ratio-data (sharpe-ratio)
        rolling-ret-data (rolling-return)
        rolling-vol-data (rolling-volatility)
        rolling-sharpe-data (rolling-sharpe-ratio)
        pnl-per-trade-data (pnl-per-trade)
        ]
    (do
      ; numerical values
      (swap! eval-record concat [(into (sorted-map) {:date date
        :tot-val total-val-data
        :ret-da daily-ret-data 
        :ret-tot total-ret-data
        :vol-e volatility-data
        :sharpe-e sharpe-ratio-data
        :ret-r rolling-ret-data
        :vol-r rolling-vol-data
        :sharpe-r rolling-sharpe-data
        :pnl-pt pnl-per-trade-data
      })])
      
      ; string formatting
      (swap! eval-report-data concat [(into (sorted-map) {:date date
              :tot-val (str "$" (int total-val-data))
              :ret-da (str (format "%.2f" daily-ret-data) "%")
              :ret-tot (str (format "%.2f" (* total-ret-data 100)) "%")
              :vol-e (str (format "%.2f" volatility-data) "%")
              :sharpe-e (str (format "%.2f" sharpe-ratio-data) "%")
              :ret-r (str (format "%.2f" rolling-ret-data) "%")
              :vol-r (str (format "%.2f" rolling-vol-data) "%")
              :sharpe-r (str (format "%.2f" rolling-sharpe-data) "%")
              :pnl-pt (str "$" (int pnl-per-trade-data))
      })])
    )
  )
)

;; Print evaluation report
(defn eval-report
  "This function prints the evaluation report."
  []
  (pprint/print-table (deref eval-report-data))
)


