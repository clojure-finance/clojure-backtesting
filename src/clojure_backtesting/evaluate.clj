(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure.pprint :as pprint]))

(def eval_record (atom []))

;; Get current portfolio total value
(defn portfolio-total 
  "This function returns the current total value of the portfolio."
  []
  (get-in (last (deref portfolio_value)) [:tot_value])
)

;; Get current portfolio daily return
(defn portfolio-daily-ret 
  "This function returns the current daily return of the portfolio."
  []
  (get-in (last (deref portfolio_value)) [:daily_ret])
)

;; Calculate portfolio total returns
(defn portfolio-total-ret
  "This function returns the current daily return of the portfolio."
  []
  (def total-ret 0)
  (doseq [daily-record (deref portfolio_value)] ;; then update the price & aprc of the securities in the portfolio
    (let [daily-ret (daily-record :daily_ret)]
      ;(println daily-ret)
      (def total-ret (+ total-ret daily-ret))
    )
  )
  total-ret
)

;; Annualised return
(defn annualised-return
  "This function calculates the annualised return of the portfolio." 
  []
  (- (Math/pow (+ 1 (portfolio-total-ret)) (/ 252 num-of-tradays)) 1)
)

;; Helper function
(defn square
  [n]
  (* n n))

;; Helper function
(defn mean
  [coll]
  (/ (reduce + coll) (count coll)))

;; Helper function
(defn standard-deviation
  [coll]
  (Math/sqrt (/ (reduce + (map square (map - coll (repeat (mean coll)))))
                (- (count coll) 1))))

;; Get list of daily returns
(defn get-daily-returns
  "This function returns a collection of daily returns from 'portfolio-value'."
  []
  (def dailyret_record (atom []))
  (doseq [daily-record (deref portfolio_value)] ;; then update the price & aprc of the securities in the portfolio
    (let [daily-ret (daily-record :daily_ret)]
      ;(println daily-ret)
      (swap! dailyret_record concat [daily-ret])
    )
  )
  dailyret_record
)

;; Volatility
(defn volatility
  "This function returns the volatility of the portfolio."
  []
  (standard-deviation (deref (get-daily-returns)))
) 

;; Sharpe ratio
(defn sharpe-ratio
  "This function returns the sharpe ratio."
  []
  (if (not= (volatility) 0.0)
    (/ (portfolio-total-ret) (volatility))
    0.0
  )
)

;; Annualised volatility
(defn annualised-volatility
  "This function returns the annualised volatility of the portfolio."
  []
  (* (Math/sqrt 252) (standard-deviation (deref (get-daily-returns))))
) 

;; Annualised sharpe ratio
(defn annualised-sharpe-ratio
  "This function returns the annualised sharpe ratio."
  []
  (if (not= (annualised-volatility) 0.0)
    (/ (annualised-return) (annualised-volatility))
    0.0
  )
)

;; PnL per trade
(defn pnl-per-trade
  []
  (/ (- (portfolio-total) init-capital) (count (deref order_record)))
)

;; Update evaluation report
(defn update-eval-report
  "This function updates the evaluation report."
  [date]
  (let [total-val (portfolio-total)
        daily-ret (portfolio-daily-ret)
        total-ret (portfolio-total-ret)
        volatility (volatility)
        sharpe-ratio (sharpe-ratio)
        annualised-ret (annualised-return)
        annualised-vol (annualised-volatility)
        annualised-sharpe (annualised-sharpe-ratio)
        pnl-per-trade (pnl-per-trade)
        ]

    (swap! eval_record concat [{:date date
                                :total-val (Double/parseDouble (format "%.2f" total-val))
                                :daily-ret (Double/parseDouble (format "%.3f" daily-ret))
                                :total-ret (Double/parseDouble (format "%.3f" total-ret))
                                :volatility (Double/parseDouble (format "%.3f" volatility))
                                :sharpe (Double/parseDouble (format "%.3f" sharpe-ratio))
                                :annualised-ret (Double/parseDouble (format "%.3f" annualised-ret))
                                :annualised-vol (Double/parseDouble (format "%.3f" annualised-vol))
                                :annualised-sharpe (Double/parseDouble (format "%.3f" annualised-sharpe))
                                :pnl-per-trade (Double/parseDouble (format "%.2f" pnl-per-trade)) 
                              }])
  )
)

;; Print evaluation report
(defn eval-report
  "This function prints the evaluation report."
  []
  (pprint/print-table (deref eval_record))
)


