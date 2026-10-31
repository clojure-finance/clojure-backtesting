(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.data-management :refer [get-permno-by-key]]
            [clojure-backtesting.portfolio :refer :all]))

;; Configuration
(def ROLLING-TIME-WINDOW (atom 30))
(def TRADING-DAYS-PER-YEAR 252)

;; ============ Evaluation metrics calculation ============

(defn portfolio-total
  "This function returns the current total value of the portfolio."
  []
  (get-in (last (deref portfolio-value)) [:tot-value]))

(defn portfolio-total-ret
  "This function returns the current total return of the portfolio."
  []
  (get (last (deref portfolio-value)) :tot-ret))

(defn portfolio-daily-ret
  "This function returns the current daily return of the portfolio."
  []
  (get-in (last (deref portfolio-value)) [:daily-ret]))

(defn get-daily-returns
  "This function returns a collection of daily returns from 'portfolio-value'."
  []
  (map :daily-ret (deref portfolio-value)))

(defn- sd-or-zero
  "Sample standard deviation, or 0.0 when there are fewer than two values."
  [xs]
  (if (< (count xs) 2)
    0.0
    (sample-sd xs)))

(defn volatility
  "The volatility of the portfolio: sample standard deviation of all daily
   log returns so far, as a fraction (the formatted report shows it in %)."
  []
  (sd-or-zero (get-daily-returns)))

(defn rolling-volatility
  "The volatility of the portfolio over the rolling time window, as a
   fraction (the formatted report shows it in %)."
  []
  (sd-or-zero (take-last (deref ROLLING-TIME-WINDOW) (get-daily-returns))))

(defn- sharpe-of
  "Annualised Sharpe ratio of a sequence of daily log returns, assuming a
   zero risk-free rate: mean daily return / daily sd * sqrt(252)."
  [rets]
  (let [vol (sd-or-zero rets)]
    (if (zero? vol)
      0.0
      (* (/ (mean rets) vol) (Math/sqrt TRADING-DAYS-PER-YEAR)))))

(defn sharpe-ratio
  "This function returns the annualised sharpe ratio of the portfolio."
  []
  (sharpe-of (get-daily-returns)))

(defn rolling-sharpe-ratio
  "This function returns the annualised sharpe ratio over the rolling time window."
  []
  (sharpe-of (take-last (deref ROLLING-TIME-WINDOW) (get-daily-returns))))

(defn pnl-per-trade
  "The profit/loss per trade in dollars: total profit over the number of
   orders the strategy placed, not counting those end-order places to
   close positions."
  []
  (/ (- (portfolio-total) (deref init-capital))
     (- (count (deref order-record)) (deref close-out-orders))))

(defn max-drawdown
  "This function returns the maximum drawdown: the largest peak-to-trough
   decline of the portfolio total value, as a fraction of the peak."
  []
  (let [values (map :tot-value (deref portfolio-value))]
    (if (empty? values)
      0.0
      (first
       (reduce (fn [[mdd peak] v]
                 (let [peak (max peak (double v))
                       dd (if (pos? peak) (/ (- peak v) peak) 0.0)]
                   [(max mdd dd) peak]))
               [0.0 (double (first values))]
               values)))))

;; ============ Update configuration & evaluation metrics ============

(defn update-rolling-window
  "This functions updates the time window for rolling functions."
  [n]
  (reset! ROLLING-TIME-WINDOW n)
  (println (str "Time window is updated as " n ".")))

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
          date (get-date)]
      ; numerical values
      (swap! eval-record conj {:date date
                               :tot-value total-val-data
                               :vol volatility-data
                               :r-vol rolling-volatility-data
                               :sharpe sharpe-ratio-data
                               :r-sharpe rolling-sharpe-ratio-data
                               :pnl-pt pnl-per-trade-data
                               :max-drawdown max-drawdown-data})
      ; string formatting
      (swap! eval-report-data conj {:date date
                                    :tot-value (str "$" (int total-val-data))
                                    :vol (str (format "%.4f" (* volatility-data 100)) "%")
                                    :r-vol (str (format "%.4f" (* rolling-volatility-data 100)) "%")
                                    :sharpe (format "%.4f" sharpe-ratio-data)
                                    :r-sharpe (format "%.4f" rolling-sharpe-ratio-data)
                                    :pnl-pt (str "$" (int pnl-per-trade-data))
                                    :max-drawdown (str (format "%.4f" (* max-drawdown-data 100)) "%")})
      ; output to file
      (write-record! evalreport-wrtr (format "%s,%f,%f,%f,%f,%f,%f,%f\n" date (double total-val-data) (double volatility-data) (double rolling-volatility-data) (double sharpe-ratio-data) (double rolling-sharpe-ratio-data) (double pnl-per-trade-data) (double max-drawdown-data))))))

;; ============ Benchmark ============

(defn- covariance [xs ys]
  (let [mx (mean xs) my (mean ys)]
    (/ (reduce + 0.0 (map (fn [x y] (* (- x mx) (- y my))) xs ys))
       (dec (count xs)))))

(defn benchmark-stats
  "Compares the portfolio with security `permno` of the main dataset (an
   index or ETF, say) over the run so far, on the days both have a value:
   {:days :portfolio-return :benchmark-return :excess-return :beta :alpha
   :correlation :tracking-error :information-ratio}. Returns are total
   returns over the period as fractions; the benchmark's comes from its
   adjusted price, so it includes dividends. :alpha (zero risk-free rate),
   :tracking-error and :information-ratio are annualised from daily log
   returns. nil with fewer than two common days."
  [permno]
  (let [pairs (->> (partition 2 1 (deref portfolio-value))
                   (keep (fn [[prev curr]]
                           (let [a0 (get-permno-by-key (:date prev) permno :APRC)
                                 a1 (get-permno-by-key (:date curr) permno :APRC)]
                             (when (and a0 a1 (pos? a0) (pos? a1))
                               [(:daily-ret curr) (Math/log (/ (double a1) (double a0)))])))))
        rp (mapv first pairs)
        rb (mapv second pairs)
        n (count pairs)]
    (when (>= n 2)
      (let [active (mapv - rp rb)
            var-b (covariance rb rb)
            beta (when (pos? var-b) (/ (covariance rp rb) var-b))
            sd-p (sample-sd rp)
            sd-b (sample-sd rb)
            te (sample-sd active)
            annual (Math/sqrt TRADING-DAYS-PER-YEAR)]
        {:days n
         :portfolio-return (- (Math/exp (reduce + rp)) 1.0)
         :benchmark-return (- (Math/exp (reduce + rb)) 1.0)
         :excess-return (- (Math/exp (reduce + rp)) (Math/exp (reduce + rb)))
         :beta beta
         :alpha (when beta (* TRADING-DAYS-PER-YEAR (- (mean rp) (* beta (mean rb)))))
         :correlation (when (and (pos? sd-p) (pos? sd-b)) (/ (covariance rp rb) (* sd-p sd-b)))
         :tracking-error (* te annual)
         :information-ratio (when (pos? te) (* annual (/ (mean active) te)))}))))
