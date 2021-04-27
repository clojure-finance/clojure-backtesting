(ns clojure-backtesting.indicators
    (:require [clojure.test :refer :all]
              [clojure-backtesting.data :refer :all]
              [clojure-backtesting.parameters :refer :all]
              [clojure-backtesting.counter :refer :all]
              [clojure-backtesting.data-management :refer :all]
              [clojure.string :as str]
              [clj-time.core :as clj-t]
              [java-time :as t]
              [clojure.core.matrix.stats :as stat] ;; For the standard deviation formula
              ))


(defn EMA
    "Returns the exponential moving average (EMA) using the recursion formula. 
     Note that the result is trustworthy after calling it for 20 times."
    ([price]
     price)
    ([price prev-ema]
     (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))))
  
(defn tic-EMA
    "This function is a wrapper of EMA()."
    ([tic key]
        (EMA (get-price tic key "lazy")))
    ([tic key prev-ema]
        (EMA (get-price tic key "lazy") prev-ema)))

(defn MACD
    "Returns a vector: (MACD, 12-day EMA, 26-day EMA)"
    ([price ema-12 ema-26]
        (let [ema-12-new (/ (+ (* price 2) (* (- 12 1) ema-12)) (+ 12 1))
            ema-26-new (/ (+ (* price 2) (* (- 26 1) ema-26)) (+ 26 1))]
        [(- ema-12-new ema-26-new) ema-12-new ema-26-new]))
    ([price vector]
        (MACD price (nth vector 1) (nth vector 2))))

(defn ROC
    "Returns the rate of change (ROC) value."
    [tic n] ; time window
    (let [old-price (Double/parseDouble (get (first (get-prev-n-days :PRC n tic)) :PRC)) ;; get price n days ago
          curr-price (Double/parseDouble (get (last (get-prev-n-days :PRC n tic)) :PRC))] ;; get today's price
       (if (not= old-price 0)
           (* (/ (- curr-price old-price) old-price) 100) ; calculate ROC
           )))

(defn RSI
    "Returns the relative strength index (RSI) value."
    [tic n] ; time window
    (let [num-of-days (atom n)
          avg-gain (atom 0.0)
          avg-loss (atom 0.0)]
        (while (> @num-of-days 1) ;; check if counter is > 0
            (let [prev-price (Double/parseDouble (get (first (get-prev-n-days :PRC (deref num-of-days) tic)) :PRC))
                  curr-price (Double/parseDouble (get (first (get-prev-n-days :PRC (- (deref num-of-days) 1) tic)) :PRC))
                  price-diff (- curr-price prev-price)]
                 (if (pos? price-diff)
                      (swap! avg-gain (partial + price-diff)) ;; add to 1st average gain
                      (swap! avg-loss (partial + price-diff)) ;; add to 1st average loss
                     ))
            (swap! num-of-days dec))
        (/ avg-gain avg-loss) ; calculate RSI
        ))

(defn sd-last-n-days
    "Returns volatility of a stock for the last n days."
    [tic days]
    ;(println (get-prev-n-days :PRC days tic))
    ;(println (map :PRC (get-prev-n-days :PRC days tic)))
    (moving-sd :PRC (get-prev-n-days :PRC days tic))
    )

(defn parabolic-SAR
    [tic mode af prev-psar]
    (let [low-price (Double/parseDouble (get-by-key tic :BIDLO mode))
          high-price (Double/parseDouble (get-by-key tic :ASKHI mode))
          rising-sar (+ prev-psar (* af (- high-price prev-psar)))
          falling-sar (+ prev-psar (* af (- low-price prev-psar)))]
        (vector rising-sar falling-sar)
        )
    )
