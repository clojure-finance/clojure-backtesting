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

(defn avg
  "This function returns the mean of the list"
  [list]
  (try
    (if (> (count list) 0)
      (/ (reduce + list) (count list)))
    (catch Exception e nil)))

;; Two functions designed for example / Bollinger Bands
(defn sd
  "This function returns the s.d. of the list"
  [list]
  (try
    (stat/sd list)
    (catch Exception e nil)))

(defn moving-sd
  "Returns volatility of a stock for the last n days."
  [tic n]
    ;(println (get-prev-n-days :PRC days tic))
    ;(println (map :PRC (get-prev-n-days :PRC days tic)))
  (sd (conj (map :PRC (get-tic-prev-n-records tic (- n 1))) (get-tic-price tic))))

(defn moving-avg
  "Returns volatility of a stock for the last n days."
  [tic n]
    ;(println (get-prev-n-days :PRC days tic))
    ;(println (map :PRC (get-prev-n-days :PRC days tic)))
  (avg (conj (map :PRC (get-tic-prev-n-records tic (- n 1))) (get-tic-price tic))))
  
;; (defn tic-EMA
;;     "This function is a wrapper of EMA()."
;;     ([tic key mode]
;;         (EMA (get-price tic key mode)))
;;     ([tic key prev-ema mode]
;;         (EMA (get-price tic key mode) prev-ema)))

(def EMA-map (transient {}))
(def EMA-keys (atom []))
(def MACD-map (transient {:MACD-sig (transient {}) :MACD-short (transient {}) :MACD-long (transient {})}))
(def MACD-keys (atom []))

(defn- _EMA
  "Returns the exponential moving average (EMA) using the recursion formula."
  ([price]
   price)
  ([prev-ema price]
   (if (and prev-ema price)
    ;;  (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))
     (+ (* EMA-K price) (* (- 1 EMA-K) prev-ema))
     price)))

(defn EMA
  "Get stable EMA of a ticker today."
  [tic]
  (if-let  [prev-ema (get EMA-map tic)]
    (if (< (compare (first prev-ema) (get-date)) 0)
      (let [ema (_EMA (get-tic-price tic) (nth prev-ema 1))]
        (def EMA-map (assoc! EMA-map tic [(get-date) ema]))
        ema)
      (nth prev-ema 1))
    ;; (let [prev-data (conj (get-tic-prev-n-records tic (dec EMA-CYCLE)) (get-tic-info tic))]
    ;;   (let [ema (reduce _EMA nil (map :PRC (reverse prev-data)))]
    ;;     (def EMA-map (assoc! EMA-map tic [(get-date) ema]))
    ;;     (swap! EMA-keys conj tic)
    ;;     ema))
    (if-let [avg (moving-avg tic EMA-CYCLE)]
      (do
        (def EMA-map (assoc! EMA-map tic [(get-date) avg]))
        avg)
      nil)
    ))

(defn- _MACD-signal
  "Returns the exponential moving average (EMA) using the recursion formula."
  ([price]
   price)
  ([prev-ema price]
   (if (and prev-ema price)
    ;;  (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))
     (+ (* MACD-SIGNAL-K price) (* (- 1 MACD-SIGNAL-K) prev-ema))
     price)))

(defn- _MACD-short
  "Returns the exponential moving average (EMA) using the recursion formula."
  ([price]
   price)
  ([prev-ema price]
   (if (and prev-ema price)
    ;;  (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))
     (+ (* MACD-SHORT-K price) (* (- 1 MACD-SHORT-K) prev-ema))
     price)))

(defn- _MACD-long
  "Returns the exponential moving average (EMA) using the recursion formula."
  ([price]
   price)
  ([prev-ema price]
   (if (and prev-ema price)
    ;;  (/ (+ (* price 2) (* (- EMA-CYCLE 1) prev-ema)) (+ EMA-CYCLE 1))
     (+ (* MACD-LONG-K price) (* (- 1 MACD-LONG-K) prev-ema))
     price)))

(defn- MACD-singal
  "compute EMA in MACD"
  [tic]
  (let [key :MACD-sig
        EMA-map (get MACD-map key)]
    (if-let  [prev-ema (get EMA-map tic)]
      (if (< (compare (first prev-ema) (get-date)) 0)
        (let [ema (_MACD-signal (nth prev-ema 1) (get-tic-price tic))]
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map tic [(get-date) ema])))
          ema)
        (nth prev-ema 1))
      (if-let [avg (moving-avg tic MACD-SIGNAL)]
        (do
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map tic [(get-date) avg])))
          avg)
        nil))))

(defn- MACD-short
  "compute EMA in MACD"
  [tic]
  (let [key :MACD-short
        EMA-map (get MACD-map key)]
    (if-let  [prev-ema (get EMA-map tic)]
      (if (< (compare (first prev-ema) (get-date)) 0)
        (let [ema (_MACD-short (nth prev-ema 1) (get-tic-price tic))]
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map tic [(get-date) ema])))
          ema)
        (nth prev-ema 1))
      (if-let [avg (moving-avg tic MACD-SHORT)]
        (do
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map tic [(get-date) avg])))
          avg)
        nil))))

(defn- MACD-long
  "compute EMA in MACD"
  [tic]
  (let [key :MACD-long
        EMA-map (get MACD-map key)]
    (if-let  [prev-ema (get EMA-map tic)]
      (if (< (compare (first prev-ema) (get-date)) 0)
        (let [ema (_MACD-long (nth prev-ema 1) (get-tic-price tic))]
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map tic [(get-date) ema])))
          ema)
        (nth prev-ema 1))
      (if-let [avg (moving-avg tic MACD-LONG)]
        (do
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map tic [(get-date) avg])))
          avg)
        nil))))

;; todo
;; (defn MACD
;;     "Returns a vector: (MACD, 12-day EMA, 26-day EMA)"
;;     ([price ema-12 ema-26 ema-9]
;;         (let [ema-12-new (/ (+ (* price 2) (* (- MACD-SHORT 1) ema-12)) (+ MACD-SHORT 1))
;;               ema-26-new (/ (+ (* price 2) (* (- MACD-LONG 1) ema-26)) (+ MACD-LONG 1))
;;               ema-9-new (/ (+ (* price 2) (* (- MACD-SIGNAL 1) ema-9)) (+ MACD-SIGNAL 1))] 
;;         [(- ema-12-new ema-26-new) ema-12-new ema-26-new ema-9-new]))
;;     ([price vector]
;;         (MACD price (nth vector 1) (nth vector 2) (nth vector 3))))
(defn MACD
  "Returns a vector: (MACD, 9-day EMA (signal), 12-day EMA (short), 26-day EMA (long))"
  [tic]
  (if (and (get-tic-info tic) (= nil (get (get MACD :MACD-long) tic)))
    (swap! MACD-keys conj tic))
  (let [signal (MACD-singal tic)
        short (MACD-short tic)
        long (MACD-long tic)]
    [(- short long) signal short long])
  )

(defn reset-indicator-maps
  []
  (def EMA-map (transient {}))
  (reset! EMA-keys [])
  (def MACD-map (transient {:MACD-sig (transient {}) :MACD-short (transient {}) :MACD-long (transient {})}))
  (reset! MACD-keys []))

(defn update-daily-indicators
  []
  (doseq [tic (deref EMA-keys)]
    (EMA tic))
  (doseq [tic (deref MACD-keys)]
    (MACD tic)))


(defn ROC
    "Returns the rate of change (ROC) value, decimal format.\n
     @n should be greater than 0"
    [tic n] ; time window
    (let [prev-n-date (get-prev-n-date n)
          old-price (get-tic-price prev-n-date tic) ;; get price n days ago
          curr-price (get-tic-price tic)] ;; get today's price
       (if (and (not= old-price nil) (not= curr-price nil) (not= old-price 0))
           (/ (- curr-price old-price) old-price) ; calculate ROC
           )))

(defn RSI
  "Returns the relative strength index (RSI) value.\n
   @n should be greater than 0"
  [tic n] ; time window
  (let [data (get-tic-prev-n-records tic n)
        prices (reverse (conj (map :PRC data) (get-tic-price tic)))]
    (loop [prices prices avg-gain 0 avg-loss 0]
      (if (<= (count prices) 1)
        (if (= avg-loss 0)
          nil
          (/  avg-gain avg-loss))
        (let [prev-price (first prices)
              remain (rest prices)
              curr-price (first remain)
              price-diff (- curr-price prev-price)]
          (if (> price-diff 0)
            (recur remain (+ avg-gain price-diff) avg-loss)
            (recur remain avg-gain (+ avg-loss price-diff)))
          ))))
  ;; (let [num-of-days (atom n)
  ;;       avg-gain (atom 0.0)
  ;;       avg-loss (atom 0.0)]
  ;;   (while (> @num-of-days 1) ;; check if counter is > 0
  ;;     (let [prev-price (Double/parseDouble (get (first (get-prev-n-days :PRC (deref num-of-days) tic)) :PRC))
  ;;           curr-price (Double/parseDouble (get (first (get-prev-n-days :PRC (- (deref num-of-days) 1) tic)) :PRC))
  ;;           price-diff (- curr-price prev-price)]
  ;;       (if (pos? price-diff)
  ;;         (swap! avg-gain (partial + price-diff)) ;; add to 1st average gain
  ;;         (swap! avg-loss (partial + price-diff)) ;; add to 1st average loss
  ;;         ))
  ;;     (swap! num-of-days dec))
  ;;   (/ (deref avg-gain) (deref avg-loss)) ; calculate RSI
  ;;   )
  )

(defn parabolic-SAR
  "Additional columns needed: BIDLO, ASKHI"
  [tic af prev-psar]
  (let [low-price (Double/parseDouble (get-tic-by-key tic :BIDLO))
        high-price (Double/parseDouble (get-tic-by-key tic :ASKHI))
        rising-sar (+ prev-psar (* af (- high-price prev-psar)))
        falling-sar (+ prev-psar (* af (- low-price prev-psar)))]
    (vector rising-sar falling-sar)))

(defn ATR
  "Additional columns needed: BIDLO, ASKHI"
  [tic n prev-atr]
  (let [low-price (Double/parseDouble (get-tic-by-key tic :BIDLO))
        high-price (Double/parseDouble (get-tic-by-key tic :ASKHI))
        current-tr (- high-price low-price)]
    (/ (+ (* prev-atr 13) current-tr) n)))

(defn keltner-channel
    [tic window prev-atr]
    ; set window for EMA
    ;; (if (not= EMA-CYCLE 20)
    ;;     (CHANGE-EMA-CYCLE 20)
    ;;     )
    (let [middle-line (EMA tic)
          upper-channel (+ middle-line (* 2 (ATR tic window prev-atr)))
          lower-channel (- middle-line (* 2 (ATR tic window prev-atr)))]
        (vector middle-line upper-channel lower-channel)
        )
    )

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