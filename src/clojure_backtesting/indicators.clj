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
  [permno n]
    ;(println (get-prev-n-days PRICE-KEY days permno))
    ;(println (map PRICE-KEY (get-prev-n-days PRICE-KEY days permno)))
  (sd (conj (map PRICE-KEY (get-permno-prev-n-days permno (- n 1))) (get-permno-price permno))))

(defn moving-avg
  "Returns volatility of a stock for the last n days."
  [permno n]
    ;(println (get-prev-n-days PRICE-KEY days permno))
    ;(println (map PRICE-KEY (get-prev-n-days PRICE-KEY days permno)))
  (avg (conj (map PRICE-KEY (get-permno-prev-n-days permno (- n 1))) (get-permno-price permno))))
  
;; (defn permno-EMA
;;     "This function is a wrapper of EMA()."
;;     ([permno key mode]
;;         (EMA (get-price permno key mode)))
;;     ([permno key prev-ema mode]
;;         (EMA (get-price permno key mode) prev-ema)))

(def ^:dynamic EMA-map (transient {}))
(def ^:dynamic EMA-keys (atom []))
(def ^:dynamic MACD-map (transient {:MACD-sig (transient {}) :MACD-short (transient {}) :MACD-long (transient {})}))
(def ^:dynamic MACD-keys (atom []))

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
  "Get stable EMA of a security today."
  [permno]
  (if-let  [prev-ema (get EMA-map permno)]
    (if (< (compare (first prev-ema) (get-date)) 0)
      (let [ema (_EMA (nth prev-ema 1) (get-permno-price permno))]
        (def EMA-map (assoc! EMA-map permno [(get-date) ema]))
        ema)
      (nth prev-ema 1))
    ;; (let [prev-data (conj (get-permno-prev-n-days permno (dec EMA-CYCLE)) (get-permno-info permno))]
    ;;   (let [ema (reduce _EMA nil (map PRICE-KEY (reverse prev-data)))]
    ;;     (def EMA-map (assoc! EMA-map permno [(get-date) ema]))
    ;;     (swap! EMA-keys conj permno)
    ;;     ema))
    (if-let [avg (moving-avg permno EMA-CYCLE)]
      (do
        (swap! EMA-keys conj permno)
        (def EMA-map (assoc! EMA-map permno [(get-date) avg]))
        avg)
      nil)
    ))

(def EMA-gen-map (atom {}))
(def EMA-gen-keys (atom {}))
(def EMA-gen-funcs (atom {}))
(def EMA-gen-sizes (atom []))

(defn EMA-generator
  [size]
  (if-let [func (get (deref EMA-gen-funcs) size)]
    func
    (let [id size
          k (/ 2 (+ size 1))
          func (fn [permno]
                 (binding [EMA-CYCLE size
                           EMA-K k
                           EMA-map (get (deref EMA-gen-map) id)
                           EMA-keys (get (deref EMA-gen-keys) id)]
                   (if-let  [prev-ema (get EMA-map permno)]
                     (if (< (compare (first prev-ema) (get-date)) 0)
                       (let [ema (_EMA (nth prev-ema 1) (get-permno-price permno))]
                         (swap! EMA-gen-map assoc id (assoc! EMA-map permno [(get-date) ema]))
                         ema)
                       (nth prev-ema 1))
                     (if-let [avg (moving-avg permno EMA-CYCLE)]
                       (do
              ;; (println (get EMA-gen-keys id))
                         (swap! EMA-keys conj permno)
                         (swap! EMA-gen-map assoc id (assoc! EMA-map permno [(get-date) avg]))
                         avg)
                       nil))))]
      (swap! EMA-gen-map assoc id (transient {}))
      (swap! EMA-gen-keys assoc id (atom []))
      (swap! EMA-gen-funcs assoc id func)
      (swap! EMA-gen-sizes conj size)
      func)))

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

(defn- MACD-signal
  "compute EMA in MACD"
  [permno]
  (let [key :MACD-sig
        EMA-map (get MACD-map key)]
    (if-let  [prev-ema (get EMA-map permno)]
      (if (< (compare (first prev-ema) (get-date)) 0)
        (let [ema (_MACD-signal (nth prev-ema 1) (get-permno-price permno))]
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map permno [(get-date) ema])))
          ema)
        (nth prev-ema 1))
      (if-let [avg (moving-avg permno MACD-SIGNAL)]
        (do
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map permno [(get-date) avg])))
          avg)
        nil))))

(defn- MACD-short
  "compute EMA in MACD"
  [permno]
  (let [key :MACD-short
        EMA-map (get MACD-map key)]
    (if-let  [prev-ema (get EMA-map permno)]
      (if (< (compare (first prev-ema) (get-date)) 0)
        (let [ema (_MACD-short (nth prev-ema 1) (get-permno-price permno))]
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map permno [(get-date) ema])))
          ema)
        (nth prev-ema 1))
      (if-let [avg (moving-avg permno MACD-SHORT)]
        (do
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map permno [(get-date) avg])))
          avg)
        nil))))

(defn- MACD-long
  "compute EMA in MACD"
  [permno]
  (let [key :MACD-long
        EMA-map (get MACD-map key)]
    (if-let  [prev-ema (get EMA-map permno)]
      (if (< (compare (first prev-ema) (get-date)) 0)
        (let [ema (_MACD-long (nth prev-ema 1) (get-permno-price permno))]
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map permno [(get-date) ema])))
          ema)
        (nth prev-ema 1))
      (if-let [avg (moving-avg permno MACD-LONG)]
        (do
          (def MACD-map (assoc! MACD-map key (assoc! EMA-map permno [(get-date) avg])))
          avg)
        nil))))

(defn MACD
  "Returns a vector: (MACD, 9-day EMA (signal), 12-day EMA (short), 26-day EMA (long))"
  [permno]
  (if (and (get-permno-info permno) (= nil (get (get MACD :MACD-long) permno)))
    (swap! MACD-keys conj permno))
  (let [signal (MACD-signal permno)
        short (MACD-short permno)
        long (MACD-long permno)]
    [(- short long) signal short long])
  )

;; (def MACD-gen-map (atom {}))
;; (def MACD-gen-keys (atom {}))
;; (def MACD-gen-funcs (atom {}))

(defn MACD-generator
  [signal short long]
  (let [MACD-signal (EMA-generator signal)
        MACD-short (EMA-generator short)
        MACD-long (EMA-generator long)
        func (fn [permno]
              ;;  (if (and (get-permno-info permno) (= nil (get (get MACD :MACD-long) permno)))
              ;;    (swap! MACD-keys conj permno))
               (let [signal (MACD-signal permno)
                     short (MACD-short permno)
                     long (MACD-long permno)]
                 [(- short long) signal short long]))]
    func)
  )

(defn ROC
    "Returns the rate of change (ROC) value, decimal format.\n
     @n should be greater than 0"
    [permno n] ; time window
    (let [prev-n-date (get-prev-n-date n)
          old-price (get-permno-price prev-n-date permno) ;; get price n days ago
          curr-price (get-permno-price permno)] ;; get today's price
       (if (and (not= old-price nil) (not= curr-price nil) (not= old-price 0))
           (/ (- curr-price old-price) old-price) ; calculate ROC
           )))

(defn RS
  "Returns the average gain and average loss of past n days.
   @n should be greater than 0"
  [permno n] ; time window
  (let [data (get-permno-prev-n-days permno (- n 1))
        prices (reverse (conj (map PRICE-KEY data) (get-permno-price permno)))]
    (if (= (count prices) n)
     (loop [prices prices avg-gain 0 avg-loss 0]
       (if (<= (count prices) 1)
        ;; (if (= avg-loss 0)
        ;;   nil
        ;;   (- 100 (/ 100 (+ 1 (/  avg-gain avg-loss)))))
         [avg-gain avg-loss]
         (let [prev-price (first prices)
               remain (rest prices)
               curr-price (first remain)
               price-diff (- curr-price prev-price)]
           (if (> price-diff 0)
             (recur remain (+ avg-gain price-diff) avg-loss)
             (recur remain avg-gain (- avg-loss price-diff))))))
      ;; (let [avg-gain-func (fn [prev new])
      ;;       avg-gain (reduce avg-gain-func prices)])
      nil)))

(def ^:dynamic RS-map (transient {}))
(def ^:dynamic RS-keys (atom []))

(defn RSI
  "Returns the Relative Strength Index (RSI)."
  [permno]
  (try
    (if-let [prev-RS (get RS-map permno)]
  ;; [(previous Average Gain) x 13 + current Gain] / 14
      (if (= (first prev-RS) (get-date))
        (- 100 (/ 100 (+ 1 (/ (nth (last prev-RS) 0) (nth (last prev-RS) 1)))))
        (let [curr-price (get-permno-price permno)
              last-price (PRICE-KEY (first (get-permno-prev-n-days permno 1)))
              curr-change (- curr-price last-price)
              prev-RS (nth prev-RS 1)
              avg-gain (/ (+ (* (- RSI-CYCLE 1) (nth prev-RS 0)) (if (> curr-change 0) curr-change 0)) RSI-CYCLE)
              avg-loss (/ (+ (* (- RSI-CYCLE 1) (nth prev-RS 1)) (if (< curr-change 0) curr-change 0)) RSI-CYCLE)]
          (def RS-map (assoc! RS-map permno [(get-date) [avg-gain avg-loss]]))
          (if (= avg-loss 0) 100 (- 100 (/ 100 (+ 1 (/ avg-gain avg-loss)))))))
      (let [tmp (RS permno RSI-CYCLE)]
        (if tmp
          (do
            (swap! RS-keys conj permno)
            (def RS-map (assoc! RS-map permno [(get-date) tmp]))
            (if (= (nth tmp 1) 0) 100 (- 100 (/ 100 (+ 1 (/ (nth tmp 0) (nth tmp 1)))))))
          nil)
        ))
    (catch Exception e nil)))

(def RSI-gen-map (atom {}))
(def RSI-gen-keys (atom {}))
(def RSI-gen-funcs (atom {}))
(def RSI-gen-sizes (atom []))

(defn RSI-generator
  [size]
  (if-let [func (get (deref RSI-gen-funcs) size)]
    func
    (let [id size
          func (fn [permno]
                 (binding [RSI-CYCLE size
                           RS-map (get (deref RSI-gen-map) id)
                           RS-keys (get (deref RSI-gen-keys) id)]
                   (try
                     (if-let [prev-RS (get RS-map permno)]
                       ;; [(previous Average Gain) x 13 + current Gain] / 14
                       (if (= (first prev-RS) (get-date))
                         (- 100 (/ 100 (+ 1 (/ (nth (last prev-RS) 0) (nth (last prev-RS) 1)))))
                         (let [curr-price (get-permno-price permno)
                               last-price (PRICE-KEY (first (get-permno-prev-n-days permno 1)))
                               curr-change (- curr-price last-price)
                               prev-RS (nth prev-RS 1)
                               avg-gain (/ (+ (* (- RSI-CYCLE 1) (nth prev-RS 0)) (if (> curr-change 0) curr-change 0)) RSI-CYCLE)
                               avg-loss (/ (+ (* (- RSI-CYCLE 1) (nth prev-RS 1)) (if (< curr-change 0) curr-change 0)) RSI-CYCLE)]
                           (swap! RSI-gen-map assoc id (assoc! RS-map permno [(get-date) [avg-gain avg-loss]]))
                           (if (= avg-loss 0) 100 (- 100 (/ 100 (+ 1 (/ avg-gain avg-loss)))))))
                       (let [tmp (RS permno RSI-CYCLE)]
                         (if tmp
                           (do
                             (swap! RS-keys conj permno)
                             (swap! RSI-gen-map assoc id (assoc! RS-map permno [(get-date) tmp]))
                             (if (= (nth tmp 1) 0) 100 (- 100 (/ 100 (+ 1 (/ (nth tmp 0) (nth tmp 1)))))))
                           nil)))
                     (catch Exception e nil))))]
      (swap! RSI-gen-map assoc id (transient {}))
      (swap! RSI-gen-keys assoc id (atom []))
      (swap! RSI-gen-funcs assoc id func)
      (swap! RSI-gen-sizes conj size)
      func)))


(defn parabolic-SAR
  "Additional columns needed: BIDLO, ASKHI"
  [permno af prev-psar]
  (let [low-price (Double/parseDouble (get-permno-by-key permno :BIDLO))
        high-price (Double/parseDouble (get-permno-by-key permno :ASKHI))
        rising-sar (+ prev-psar (* af (- high-price prev-psar)))
        falling-sar (+ prev-psar (* af (- low-price prev-psar)))]
    (vector rising-sar falling-sar)))

(defn ATR
  "Additional columns needed: BIDLO, ASKHI"
  [permno n prev-atr]
  (let [low-price (Double/parseDouble (get-permno-by-key permno :BIDLO))
        high-price (Double/parseDouble (get-permno-by-key permno :ASKHI))
        current-tr (- high-price low-price)]
    (/ (+ (* prev-atr 13) current-tr) n)))

(defn keltner-channel
    [permno window prev-atr]
    ; set window for EMA
    ;; (if (not= EMA-CYCLE 20)
    ;;     (CHANGE-EMA-CYCLE 20)
    ;;     )
    (let [middle-line (EMA permno)
          upper-channel (+ middle-line (* 2 (ATR permno window prev-atr)))
          lower-channel (- middle-line (* 2 (ATR permno window prev-atr)))]
        (vector middle-line upper-channel lower-channel)
        )
    )


(defn reset-indicator-maps
  []
  (def ^:dynamic EMA-map (transient {}))
  (reset! EMA-keys [])
  (doseq [id (deref EMA-gen-sizes)]
    (swap! EMA-gen-map assoc id (transient {}))
    (swap! EMA-gen-keys assoc id (atom []))
    ;; (swap! EMA-gen-funcs assoc id func)
    ;; (swap! EMA-gen-sizes conj size)
    )
  (def MACD-map (transient {:MACD-sig (transient {}) :MACD-short (transient {}) :MACD-long (transient {})}))
  (reset! MACD-keys [])
  (def ^:dynamic RS-map (transient {}))
  (reset! RS-keys [])
  (doseq [id (deref RSI-gen-sizes)]
    (swap! RSI-gen-map assoc id (transient {}))
    (swap! RSI-gen-keys assoc id (atom []))
    ;; (swap! RSI-gen-funcs assoc id func)
    ;; (swap! RSI-gen-sizes conj size)
    ))

(defn update-daily-indicators
  []
  (doseq [permno (deref EMA-keys)]
    (EMA permno))
  (doseq [size (deref EMA-gen-sizes)]
    (doseq [permno (deref (get (deref EMA-gen-keys) size))]
      ((get (deref EMA-gen-funcs) size) permno)))
  (doseq [permno (deref MACD-keys)]
    (MACD permno))
  (doseq [permno (deref RS-keys)]
    (RSI permno))
  (doseq [size (deref RSI-gen-sizes)]
    (doseq [permno (deref (get (deref RSI-gen-keys) size))]
      ((get (deref RSI-gen-funcs) size) permno))))

;; ;; need to double-check
;; (defn force-index
;;     [permno mode window]
;;     (if (= window 1)
;;         ;; calculate force index for window = 1
;;         (let [prev-price (Double/parseDouble (get (first (get-prev-n-days PRICE-KEY 1 permno)) PRICE-KEY))
;;               curr-price (Double/parseDouble (get-price permno PRICE-KEY mode))
;;               curr-volume (Double/parseDouble (get-by-key permno :VOL mode))]
;;               (* (- curr-price prev-price) curr-volume)
;;                 )
;;         ;; calculate force index for window > 1
;;         (do
;;             ;; check EMA window
;;             (if (not= EMA-CYCLE window)
;;                 (CHANGE-EMA-CYCLE window))
;;             ;; get EMA of force index(1)
;;             (let [prev-price (Double/parseDouble (get (first (get-prev-n-days PRICE-KEY 1 permno)) PRICE-KEY))
;;                   prev-fi (atom prev-price)
;;                   ema-value (atom 0)]
;;                   (doseq [prev-n-prices (get-prev-n-days PRICE-KEY window permno)]
;;                     (do
;;                         (let [curr-price (Double/parseDouble (get prev-n-prices PRICE-KEY))
;;                               curr-fi (force-index permno mode 1)]
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