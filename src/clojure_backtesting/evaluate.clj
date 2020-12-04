(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure.pprint :as pprint]
            ))

(def eval_record (atom []))

;; Get current portfolio total value
(defn portfolio-total 
  "This function returns the current total value of the portfolio."
  []
  (get-in (last (deref portfolio_value)) [:tot_value])
)

;; Get current portfolio daily return (in %)
(defn portfolio-daily-ret 
  "This function returns the current daily return of the portfolio in %."
  []
  (* (get-in (last (deref portfolio_value)) [:daily_ret]) 100)
)

;; Calculate portfolio total returns
(defn portfolio-total-ret
  "This function returns the current daily return of the portfolio in %."
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

;; Return calculated with rolling fixed window of 1 year (in %)
(defn rolling-return
  "This function calculates the return of the portfolio, with a rolling 
  fixed window of 1 year." 
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
    (* (/ (portfolio-total-ret) (volatility)) 100)
    0.0
  )
)

;; Volatility, rolling window (in %)
(defn rolling-volatility
  "This function returns the volatility of the portfolio in %, calculated with a
  rolling fixed window of 1 year.."
  []
  (* (* (Math/sqrt 252) (standard-deviation (deref (get-daily-returns)))) 100)
) 

;; Sharpe ratio, rolling window (in %)
(defn rolling-sharpe-ratio
  "This function returns the sharpe ratio in %, calculated with a
  rolling fixed window of 1 year."
  []
  (if (not= (rolling-volatility) 0.0)
    (* (/ (rolling-return) (rolling-volatility)) 100)
    0.0
  )
)

;; PnL per trade (in $)
(defn pnl-per-trade
  "This function returns the profit/loss per trade in dollars."
  []
  (/ (- (portfolio-total) init-capital) (count (deref order_record)))
)

;; Update evaluation report
(defn update-eval-report
  "This function updates the evaluation report."
  [date]
  (let [total-val (str "$" (int (portfolio-total)))
        daily-ret (str (format "%.2f" (portfolio-daily-ret)) "%")
        total-ret (str (format "%.2f" (portfolio-total-ret)) "%")
        volatility (str (format "%.2f" (volatility)) "%")
        sharpe-ratio (str (format "%.2f" (sharpe-ratio)) "%")
        rolling-ret (str (format "%.2f" (rolling-return)) "%")
        rolling-vol (str (format "%.2f" (rolling-volatility)) "%")
        rolling-sharpe (str (format "%.2f" (rolling-sharpe-ratio)) "%")
        pnl-per-trade (str "$" (format "%.2f" (pnl-per-trade)))
        ]

    (swap! eval_record concat [(into (sorted-map) {:date date
                                :tot-val total-val
                                :ret-daily daily-ret
                                :ret-total total-ret
                                :vol-exp volatility
                                :sharpe-exp sharpe-ratio
                                :ret-roll rolling-ret
                                :vol-roll rolling-vol
                                :sharpe-roll rolling-sharpe
                                :pnl-per-t pnl-per-trade 
                              })])
  )
)

;; Print evaluation report
(defn eval-report
  "This function prints the evaluation report."
  []
  (pprint/print-table (deref eval_record))
)


