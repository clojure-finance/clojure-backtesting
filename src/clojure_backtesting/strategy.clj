(ns clojure-backtesting.strategy
  "Helpers for cross-sectional strategies: calendar triggers, ranking and
   screening, and orders that target a fraction of the portfolio."
  (:require [clojure-backtesting.counter :refer [get-date]]
            [clojure-backtesting.data :refer [data-files]]
            [clojure-backtesting.data-management :refer [available-permnos get-permno-price]]
            [clojure-backtesting.order :refer [order]]
            [clojure-backtesting.portfolio :refer [portfolio portfolio-value]])
  (:import [java.time LocalDate]
           [java.time.temporal IsoFields]))

;; ============ Calendar ============

(defn- period-key [date unit]
  (let [d (LocalDate/parse date)]
    (case unit
      :week [(.get d IsoFields/WEEK_BASED_YEAR) (.get d IsoFields/WEEK_OF_WEEK_BASED_YEAR)]
      :month [(.getYear d) (.getMonthValue d)]
      :quarter [(.getYear d) (quot (dec (.getMonthValue d)) 3)]
      :year [(.getYear d)]
      (throw (ex-info (str "Unknown period " unit ", expected :week, :month, :quarter or :year.")
                      {:type :invalid-period :unit unit})))))

(defn new-period?
  "True on the first trading day in the dataset of a new :week, :month,
   :quarter or :year, and on the first day of the run (the date
   init-portfolio started from), so a strategy invests at once."
  [unit]
  (let [today (get-date)
        prev (first (first (rsubseq data-files < today)))]
    (or (nil? prev)
        (= today (:date (first (deref portfolio-value))))
        (not= (period-key prev unit) (period-key today unit)))))

;; ============ Ranking ============

(defn- finite? [x]
  (and (number? x) (not (Double/isNaN (double x))) (not (Double/isInfinite (double x)))))

(defn rank-by
  "[[permno value] ...] for the securities where (f permno) is a finite
   number, highest value first. Options: :universe, the securities to rank
   (today's securities by default); :ascending true for lowest first."
  [f & {:keys [universe ascending]}]
  (let [ranked (->> (or universe (available-permnos))
                    (keep (fn [p] (let [v (f p)] (when (finite? v) [p (double v)]))))
                    (sort-by (fn [[p v]] [v p])))]
    (vec (if ascending ranked (reverse ranked)))))

(defn top-n
  "The `n` securities with the highest (f permno), or the lowest with
   :ascending true; takes the options of rank-by."
  [f n & opts]
  (mapv first (take n (apply rank-by f opts))))

(defn equal-weights
  "{permno 1/n} for the given securities."
  [permnos]
  (let [ps (distinct permnos)]
    (zipmap ps (repeat (if (seq ps) (/ 1.0 (count ps)) 0.0)))))

;; ============ Target orders ============

(defn portfolio-equity
  "The portfolio's equity as last valued (today's close once next-date has run)."
  []
  (:tot-value (peek (deref portfolio-value))))

(defn target-quantity
  "Shares of `permno` worth `weight` of today's equity at today's price, or
   nil without a price. With `whole-shares?` the count is rounded toward zero."
  [permno weight whole-shares?]
  (when-let [price (get-permno-price permno)]
    (let [q (/ (* (double weight) (double (portfolio-equity))) (double price))]
      (if whole-shares? (double (long q)) q))))

(defn order-target-percent
  "Orders `permno` so that it becomes `weight` (a fraction, negative for a
   short) of today's equity at today's price. Like `order`, it fills at the
   next close; the trade is the difference to the holding then. Options:
   :whole-shares? true rounds toward zero, and the options of `order`.
   Returns the target quantity, or nil when there is no price today."
  [permno weight & {:keys [whole-shares?] :as opts}]
  (when-let [q (target-quantity permno weight whole-shares?)]
    (apply order permno q :remaining true (mapcat identity (dissoc opts :whole-shares?)))
    q))

(defn rebalance
  "Targets the portfolio `weights` ({permno fraction of equity}); what they
   leave over stays in cash. Every holding not in `weights` is closed.
   Takes the options of order-target-percent. Returns {permno target
   quantity} for the orders placed."
  [weights & {:as opts}]
  (let [held (remove #{:cash} (keys (deref portfolio)))
        args (mapcat identity (dissoc opts :whole-shares?))]
    (doseq [p held :when (not (contains? weights p))]
      (apply order p 0 :remaining true args))
    (into {}
          (keep (fn [[p w]] (when-let [q (apply order-target-percent p w (mapcat identity opts))] [p q])))
          weights)))
