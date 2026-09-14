(ns clojure-backtesting.indicators
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.data-management :refer :all]))

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
    (sample-sd list)
    (catch Exception e nil)))

(defn- last-n-prices
  "Today's price and the n-1 before it, oldest first, or nil unless all n
   are available."
  [permno n]
  (let [prices (reverse (conj (map PRICE-KEY (get-permno-prev-n-days permno (- n 1)))
                              (get-permno-price permno)))]
    (when (and (= (count prices) n) (every? some? prices))
      prices)))

(defn moving-sd
  "Sample standard deviation of the last n closes, or nil unless all n are available."
  [permno n]
  (some-> (last-n-prices permno n) sd))

(defn moving-avg
  "Simple moving average of the last n closes, or nil unless all n are available."
  [permno n]
  (some-> (last-n-prices permno n) avg))

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
     (+ (* EMA-K price) (* (- 1 EMA-K) prev-ema))
     price)))

(defn EMA
  "Get stable EMA of a security today."
  [permno]
  (if-let [prev-ema (get EMA-map permno)]
    (if (< (compare (first prev-ema) (get-date)) 0)
      (let [ema (_EMA (nth prev-ema 1) (get-permno-price permno))]
        (def EMA-map (assoc! EMA-map permno [(get-date) ema]))
        ema)
      (nth prev-ema 1))
    (if-let [avg (moving-avg permno EMA-CYCLE)]
      (do
        (swap! EMA-keys conj permno)
        (def EMA-map (assoc! EMA-map permno [(get-date) avg]))
        avg)
      nil)))

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
                   (if-let [prev-ema (get EMA-map permno)]
                     (if (< (compare (first prev-ema) (get-date)) 0)
                       (let [ema (_EMA (nth prev-ema 1) (get-permno-price permno))]
                         (swap! EMA-gen-map assoc id (assoc! EMA-map permno [(get-date) ema]))
                         ema)
                       (nth prev-ema 1))
                     (if-let [avg (moving-avg permno EMA-CYCLE)]
                       (do
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
     (+ (* MACD-SIGNAL-K price) (* (- 1 MACD-SIGNAL-K) prev-ema))
     price)))

(defn- _MACD-short
  "Returns the exponential moving average (EMA) using the recursion formula."
  ([price]
   price)
  ([prev-ema price]
   (if (and prev-ema price)
     (+ (* MACD-SHORT-K price) (* (- 1 MACD-SHORT-K) prev-ema))
     price)))

(defn- _MACD-long
  "Returns the exponential moving average (EMA) using the recursion formula."
  ([price]
   price)
  ([prev-ema price]
   (if (and prev-ema price)
     (+ (* MACD-LONG-K price) (* (- 1 MACD-LONG-K) prev-ema))
     price)))

(defn- MACD-signal
  "compute EMA in MACD"
  [permno]
  (let [key :MACD-sig
        EMA-map (get MACD-map key)]
    (if-let [prev-ema (get EMA-map permno)]
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
    (if-let [prev-ema (get EMA-map permno)]
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
    (if-let [prev-ema (get EMA-map permno)]
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
  (when (and (get-permno-info permno)
             (nil? (get (get MACD-map :MACD-long) permno))
             (not (some #{permno} (deref MACD-keys))))
    (swap! MACD-keys conj permno))
  (let [signal (MACD-signal permno)
        short (MACD-short permno)
        long (MACD-long permno)]
    (when (and signal short long)
      [(- short long) signal short long])))

(defn MACD-generator
  [signal short long]
  (let [MACD-signal (EMA-generator signal)
        MACD-short (EMA-generator short)
        MACD-long (EMA-generator long)
        func (fn [permno]
               (let [signal (MACD-signal permno)
                     short (MACD-short permno)
                     long (MACD-long permno)]
                 (when (and signal short long)
                   [(- short long) signal short long])))]
    func))

(defn ROC
  "Returns the rate of change (ROC) over the last n trading days as a
   decimal, or nil when fewer than n earlier days exist or either price is
   missing.
   @n should be greater than 0"
  [permno n] ; time window
  (when-let [prev-n-date (get-prev-n-date n)]
    (let [old-price (get-permno-price prev-n-date permno) ;; get price n days ago
          curr-price (get-permno-price permno)] ;; get today's price
      (when (and old-price curr-price (not= old-price 0))
        (/ (- curr-price old-price) old-price)))))

(defn RS
  "Returns [average-gain average-loss] over the past n days (n prices, n-1
   changes), the seed for Wilder's RSI smoothing. nil unless all n prices
   are available.
   @n should be greater than 1"
  [permno n] ; time window
  (let [data (get-permno-prev-n-days permno (- n 1))
        prices (reverse (conj (map PRICE-KEY data) (get-permno-price permno)))]
    (if (and (= (count prices) n) (every? some? prices))
      (let [changes (map - (rest prices) prices)
            gains (filter pos? changes)
            losses (filter neg? changes)]
        [(/ (reduce + 0.0 gains) (- n 1))
         (/ (- (reduce + 0.0 losses)) (- n 1))])
      nil)))

(defn- wilder-update
  "One step of Wilder's smoothing of [avg-gain avg-loss] with today's price change."
  [[avg-gain avg-loss] change n]
  [(/ (+ (* (- n 1) avg-gain) (max change 0.0)) n)
   (/ (+ (* (- n 1) avg-loss) (max (- change) 0.0)) n)])

(defn- rsi-value
  "RSI from [avg-gain avg-loss]; 100 when there have been no losses."
  [[avg-gain avg-loss]]
  (if (zero? avg-loss)
    100
    (- 100 (/ 100 (+ 1 (/ avg-gain avg-loss))))))

(def ^:dynamic RS-map (transient {}))
(def ^:dynamic RS-keys (atom []))

(defn RSI
  "Returns the Relative Strength Index (RSI) over RSI-CYCLE days, seeded with
   the simple average gain and loss and then Wilder-smoothed day by day."
  [permno]
  (try
    (if-let [prev-RS (get RS-map permno)]
      (if (= (first prev-RS) (get-date))
        (rsi-value (nth prev-RS 1))
        (let [curr-price (get-permno-price permno)
              last-price (PRICE-KEY (first (get-permno-prev-n-days permno 1)))
              rs (wilder-update (nth prev-RS 1) (- curr-price last-price) RSI-CYCLE)]
          (def RS-map (assoc! RS-map permno [(get-date) rs]))
          (rsi-value rs)))
      (let [tmp (RS permno RSI-CYCLE)]
        (if tmp
          (do
            (swap! RS-keys conj permno)
            (def RS-map (assoc! RS-map permno [(get-date) tmp]))
            (rsi-value tmp))
          nil)))
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
                       (if (= (first prev-RS) (get-date))
                         (rsi-value (nth prev-RS 1))
                         (let [curr-price (get-permno-price permno)
                               last-price (PRICE-KEY (first (get-permno-prev-n-days permno 1)))
                               rs (wilder-update (nth prev-RS 1) (- curr-price last-price) RSI-CYCLE)]
                           (swap! RSI-gen-map assoc id (assoc! RS-map permno [(get-date) rs]))
                           (rsi-value rs)))
                       (let [tmp (RS permno RSI-CYCLE)]
                         (if tmp
                           (do
                             (swap! RS-keys conj permno)
                             (swap! RSI-gen-map assoc id (assoc! RS-map permno [(get-date) tmp]))
                             (rsi-value tmp))
                           nil)))
                     (catch Exception e nil))))]
      (swap! RSI-gen-map assoc id (transient {}))
      (swap! RSI-gen-keys assoc id (atom []))
      (swap! RSI-gen-funcs assoc id func)
      (swap! RSI-gen-sizes conj size)
      func)))

(defn parabolic-SAR
  "Wilder's parabolic stop-and-reverse for `permno`, one trading day per
   call. `state` is the map returned by the previous call, or nil on the
   first day, when the trend is read from today's close against the
   previous one. Returns {:sar :trend :ep :af}: the stop level, :up or
   :down, the extreme point of the current trend, and the acceleration
   factor, which grows by SAR-AF-STEP on every new extreme up to SAR-AF-MAX.
   In an up trend the stop never rises above the previous two lows, and the
   mirror image holds in a down trend.
   Additional columns needed: BIDLO, ASKHI"
  [permno state]
  (let [high (->double (get-permno-by-key permno :ASKHI))
        low (->double (get-permno-by-key permno :BIDLO))
        prev-days (take 2 (get-permno-prev-n-days permno 2))
        prev-highs (keep #(->double (:ASKHI %)) prev-days)
        prev-lows (keep #(->double (:BIDLO %)) prev-days)]
    (if (nil? state)
      (let [prev-close (some-> (first prev-days) PRICE-KEY)
            up? (or (nil? prev-close) (>= (get-permno-price permno) prev-close))]
        (if up?
          {:sar (apply min low prev-lows) :trend :up :ep high :af SAR-AF-STEP}
          {:sar (apply max high prev-highs) :trend :down :ep low :af SAR-AF-STEP}))
      (let [{:keys [sar trend ep af]} state
            next-sar (+ sar (* af (- ep sar)))]
        (if (= trend :up)
          (if (< low next-sar)
            {:sar ep :trend :down :ep low :af SAR-AF-STEP}
            (let [new-ep (max ep high)]
              {:sar (apply min next-sar prev-lows)
               :trend :up
               :ep new-ep
               :af (if (> new-ep ep) (min SAR-AF-MAX (+ af SAR-AF-STEP)) af)}))
          (if (> high next-sar)
            {:sar ep :trend :up :ep high :af SAR-AF-STEP}
            (let [new-ep (min ep low)]
              {:sar (apply max next-sar prev-highs)
               :trend :down
               :ep new-ep
               :af (if (< new-ep ep) (min SAR-AF-MAX (+ af SAR-AF-STEP)) af)})))))))

(defn ATR
  "Average true range over n days, Wilder-smoothed from `prev-atr`. The true
   range is the largest of high-low, |high-previous close| and
   |low-previous close|; without a previous close it is high-low.
   Additional columns needed: BIDLO, ASKHI"
  [permno n prev-atr]
  (let [low-price (->double (get-permno-by-key permno :BIDLO))
        high-price (->double (get-permno-by-key permno :ASKHI))
        prev-close (PRICE-KEY (first (get-permno-prev-n-days permno 1)))
        current-tr (if prev-close
                     (max (- high-price low-price)
                          (Math/abs (double (- high-price prev-close)))
                          (Math/abs (double (- low-price prev-close))))
                     (- high-price low-price))]
    (/ (+ (* prev-atr (- n 1)) current-tr) n)))

(defn keltner-channel
  "[middle upper lower]: the EMA with bands two ATRs either side."
  [permno window prev-atr]
  (let [middle-line (EMA permno)
        band (* 2 (ATR permno window prev-atr))]
    (vector middle-line (+ middle-line band) (- middle-line band))))

(defn reset-indicator-maps
  []
  (def ^:dynamic EMA-map (transient {}))
  (reset! EMA-keys [])
  (doseq [id (deref EMA-gen-sizes)]
    (swap! EMA-gen-map assoc id (transient {}))
    (swap! EMA-gen-keys assoc id (atom [])))
  (def MACD-map (transient {:MACD-sig (transient {}) :MACD-short (transient {}) :MACD-long (transient {})}))
  (reset! MACD-keys [])
  (def ^:dynamic RS-map (transient {}))
  (reset! RS-keys [])
  (doseq [id (deref RSI-gen-sizes)]
    (swap! RSI-gen-map assoc id (transient {}))
    (swap! RSI-gen-keys assoc id (atom []))))

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
