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


(defn _EMA
  "Returns the exponential moving average (EMA) using the recursion formula. 
     Note that the result is trustworthy after calling it for 20 times."
  ([price]
   price)
  ([prev-ema price]
   (if prev-ema
     (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))
     price)))
  
;; (defn tic-EMA
;;     "This function is a wrapper of EMA()."
;;     ([tic key mode]
;;         (EMA (get-price tic key mode)))
;;     ([tic key prev-ema mode]
;;         (EMA (get-price tic key mode) prev-ema)))

(def EMA-map (transient {}))

(defn EMA
  "Get stable EMA of a ticker today."
  [tic]
  (if-let  [prev-ema (get EMA-map tic)]
    (if (< (compare (first prev-ema) (get-date)) 0)
      (let [ema (_EMA (get-tic-price tic) (nth prev-ema 1))]
        (def EMA-map (assoc! EMA-map tic [(get-date) ema]))
        ema)
      (nth prev-ema 1))
    (let [prev-data (get-tic-prev-n-records 20 tic)]
      (let [ema (reduce _EMA nil (map :PRC (reverse prev-data)))]
        (def EMA-map (assoc! EMA-map tic [(get-date) ema]))
        ema))))

(defn reset-indicator-maps
  []
  (def EMA-map (transient {})))

(defn update-daily-indicators
  []
  (doseq [[tic _] EMA-map]
    (EMA tic)))

;; (defn MACD
;;     "Returns a vector: (MACD, 12-day EMA, 26-day EMA)"
;;     ([price ema-12 ema-26 ema-9]
;;         (let [ema-12-new (/ (+ (* price 2) (* (- MACD-SHORT 1) ema-12)) (+ MACD-SHORT 1))
;;               ema-26-new (/ (+ (* price 2) (* (- MACD-LONG 1) ema-26)) (+ MACD-LONG 1))
;;               ema-9-new (/ (+ (* price 2) (* (- MACD-SIGNAL 1) ema-9)) (+ MACD-SIGNAL 1))] 
;;         [(- ema-12-new ema-26-new) ema-12-new ema-26-new ema-9-new]))
;;     ([price vector]
;;         (MACD price (nth vector 1) (nth vector 2) (nth vector 3))))

(defn ROC
    "Returns the rate of change (ROC) value, decimal format."
    [tic n] ; time window
    (let [prev-n-date (get-prev-n-date n)
          old-price (get-tic-price prev-n-date tic) ;; get price n days ago
          curr-price (get-tic-price tic)] ;; get today's price
       (if (or (and (not= old-price nil) (not= curr-price nil)) (not= old-price 0))
           (/ (- curr-price old-price) old-price) ; calculate ROC
           )))

;; (defn RSI
;;     "Returns the relative strength index (RSI) value."
;;     [tic n] ; time window
;;     (let [num-of-days (atom n)
;;           avg-gain (atom 0.0)
;;           avg-loss (atom 0.0)]
;;         (while (> @num-of-days 1) ;; check if counter is > 0
;;             (let [prev-price (Double/parseDouble (get (first (get-prev-n-days :PRC (deref num-of-days) tic)) :PRC))
;;                   curr-price (Double/parseDouble (get (first (get-prev-n-days :PRC (- (deref num-of-days) 1) tic)) :PRC))
;;                   price-diff (- curr-price prev-price)]
;;                  (if (pos? price-diff)
;;                       (swap! avg-gain (partial + price-diff)) ;; add to 1st average gain
;;                       (swap! avg-loss (partial + price-diff)) ;; add to 1st average loss
;;                      ))
;;             (swap! num-of-days dec))
;;         (/ (deref avg-gain) (deref avg-loss)) ; calculate RSI
;;         ))

;; (defn sd-last-n-days
;;     "Returns volatility of a stock for the last n days."
;;     [tic days]
;;     ;(println (get-prev-n-days :PRC days tic))
;;     ;(println (map :PRC (get-prev-n-days :PRC days tic)))
;;     (moving-sd :PRC (get-prev-n-days :PRC days tic))
;;     )

;; (defn parabolic-SAR
;;     [tic mode af prev-psar]
;;     (let [low-price (Double/parseDouble (get-by-key tic :BIDLO mode))
;;           high-price (Double/parseDouble (get-by-key tic :ASKHI mode))
;;           rising-sar (+ prev-psar (* af (- high-price prev-psar)))
;;           falling-sar (+ prev-psar (* af (- low-price prev-psar)))]
;;         (vector rising-sar falling-sar)
;;         )
;;     )

;; (defn ATR
;;     [tic mode prev-atr n]
;;     (let [low-price (Double/parseDouble (get-by-key tic :BIDLO mode))
;;           high-price (Double/parseDouble (get-by-key tic :ASKHI mode))
;;           current-tr (- high-price low-price)]
;;         (/ (+ (* prev-atr 13) current-tr) n)
;;         )
;;     )

;; (defn keltner-channel
;;     [tic mode window prev-atr]
;;     ; set window for EMA
;;     (if (not= EMA-CYCLE 20)
;;         (CHANGE-EMA-CYCLE 20)
;;         )
;;     (let [middle-line (Double/parseDouble (tic-EMA tic :PRC mode))
;;           upper-channel (+ middle-line (* 2 (ATR tic mode prev-atr window)))
;;           lower-channel (- middle-line (* 2 (ATR tic mode prev-atr window)))]
;;         (vector middle-line upper-channel lower-channel)
;;         )
;;     )

;; ;; need to double-check
;; (defn force-index
;;     [tic mode window]
;;     (if (= window 1)
;;         ;; calculate force index for window = 1
;;         (let [prev-price (Double/parseDouble (get (first (get-prev-n-days :PRC 1 tic)) :PRC))
;;               curr-price (Double/parseDouble (get-price tic :PRC mode))
;;               curr-volume (Double/parseDouble (get-by-key tic :VOL mode))]
;;               (* (- curr-price prev-price) curr-volume)
;;                 )
;;         ;; calculate force index for window > 1
;;         (do
;;             ;; check EMA window
;;             (if (not= EMA-CYCLE window)
;;                 (CHANGE-EMA-CYCLE window))
;;             ;; get EMA of force index(1)
;;             (let [prev-price (Double/parseDouble (get (first (get-prev-n-days :PRC 1 tic)) :PRC))
;;                   prev-fi (atom prev-price)
;;                   ema-value (atom 0)]
;;                   (doseq [prev-n-prices (get-prev-n-days :PRC window tic)]
;;                     (do
;;                         (let [curr-price (Double/parseDouble (get prev-n-prices :PRC))
;;                               curr-fi (force-index tic mode 1)]
;;                               (println (EMA curr-fi (deref prev-fi)))
;;                               (reset! ema-value (EMA curr-fi (deref prev-fi)))
;;                               (reset! prev-fi curr-fi))
;;                         ))
;;                     ;; return force index
;;                     (deref ema-value)
;;                 )
;;             )
;;         )
;;     )