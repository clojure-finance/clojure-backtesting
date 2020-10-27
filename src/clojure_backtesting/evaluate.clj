(ns clojure-backtesting.evaluate
  (:require [clojure-backtesting.data :refer :all]))

;; GET PORFTOLIO VALUE
(defn get-portfolio-values [coll]
  (last (deref portfolio_value)))

;; GET RETURNS
(defn get-return
  [order-list]
  (float (/ (->> order-list
     (map #(* -1 (:price %) (:quan %)))
     (reduce +)) init-capital))) ;;helper function

(defn get-overall-return
 []
 (get-return init-capital order_record))

(defn get-annualized-return
 []
 (- (Math/pow (+ 1 (get-overall-return init-capital)) (/ 252 num-of-tradays)) 1))

;; GET VOLATILITY
(defn square
  [n]
  (* n n))

(defn mean
  [coll]
  (/ (reduce + coll) (count coll)))

(defn standard-deviation
  [coll]
  (Math/sqrt (/ (reduce + (map square (map - coll (repeat (mean coll)))))
              (- (count coll) 1))))

(defn get-annualized-volatilty
  [atom-coll]
  (* (Math/sqrt 252) (standard-deviation (vals (deref atom-coll))))) ;; multiplt square root of 252 to get the annualized vol

;; GET SHARPE RATIO
(defn annualized-sharpe-ratio
  [atom-coll]
  (get-annualized-return atom-coll)/(get-annualized-volatility))

;; GET PNL PER TRADE
(defn get-pnl-per-trade
  []
  (/ (- (last (deref portfolio_value)) init-capital)) (count order_record))

;; GET THE MAXIMUM DRAWDOWN
(defn get-highest ;; get peak value before largest drop
  []
  )

;; get lowest value before new high
(defn get-lowest
  []
  )
