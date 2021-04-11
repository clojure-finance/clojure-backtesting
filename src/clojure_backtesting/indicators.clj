(ns clojure-backtesting.data-management
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
    "This function uses the recursion formula to calculate EMA. 
     Note that the result is trustworthy after calling it for 20 times."
    ([price]
     price)
    ([price prev-ema]
     (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))))
  
(defn tic-EMA
    "This function is the wrapper of EMA()."
    ([tic key]
        (EMA (get-price tic key "lazy")))
    ([tic key prev-ema]
        (EMA (get-price tic key "lazy") prev-ema)))

(defn MACD
    "Return a vector of three values: MACD, 12 days EMA, 26 days EMA"
    ([price ema-12 ema-26]
        (let [ema-12-new (/ (+ (* price 2) (* (- 12 1) ema-12)) (+ 12 1))
            ema-26-new (/ (+ (* price 2) (* (- 26 1) ema-26)) (+ 26 1))]
        [(- ema-12-new ema-26-new) ema-12-new ema-26-new]))
    ([price vector]
        (MACD price (nth vector 1) (nth vector 2))))