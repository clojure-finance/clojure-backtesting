(ns clojure-backtesting.kpifunc
  (:require [clojure-backtesting.data :refer :all]
            ))

;; Functions for getting the returns
(defn get-return
  [init-capital order-list]
  (float (/ (->> order-list
     (map #(* -1 (:price %) (:quan %)))
     (reduce +)) init-capital))) ;;helper function

(defn get-overall-return
  [init-capital]
  (get-return init-capital order_record))

(defn get-security-return
  [security init-capital]
  (get-return init-capital (filter #(= (:tic %) security) order_record)))

(defn get-annualized-return
  [init-capital dataset]
  (- (Math/pow (+ 1 (get-overall-return init-capital)) (/ 252 (count-days dataset))) 1)

;; Functions for getting the volatilty
(defn get-annualized-volatility
  ;; need to calculatet the daily return volatility
  )

;; Functions for getting the Sharpe ratio
(defn get-sharpe-ratio
  []
  (/ (get-annualized-return init-capital dataset) (get-annualized-volatility))
