(ns clojure-backtesting.sample-yahoo
  "Synthetic raw Yahoo Finance snapshots in exactly the shape
   `clojure-backtesting.yahoo` downloads, so that the Yahoo pipeline, its
   tests and the examples run offline and without redistributing Yahoo
   data. Invented securities over the weekdays of 2022 to 2024, downloaded
   on 2025-01-02 (ALFA also on 2024-06-01):

   - ALFA  pays a quarterly dividend and splits 4-for-1 on 2024-08-01,
           between its two snapshots; reports after the close, about 30
           days after each quarter (40 after the fiscal year); one quarter's
           net income is restated in the second snapshot
   - BETA  makes losses until 2022; its earnings history is missing and its
           announcement dates stop in May 2023, as Yahoo's do, so later
           report dates fall back to the reporting lag
   - GAMA  trades in US dollars but reports in euros, before the open
   - DLTA  has prices only, with one bar without a close
   - IDX   an index to benchmark against
   - ZZZZ  was not found (delisted)

   plus the EURUSD=X exchange rate. Like Yahoo, the snapshots quote prices,
   volumes, dividends and per-share figures in the share basis of the day
   they were downloaded, and hold four fiscal years and five quarters of
   statements. `lein run -m clojure-backtesting.sample-yahoo` rewrites
   resources/sample-yahoo."
  (:require [clojure.java.io :as io]
            [clojure-backtesting.yahoo.build :as build])
  (:import [java.time Instant LocalDate ZoneOffset]
           [java.util Random]))

(def first-day "2022-01-03")
(def last-day "2024-12-31")
(def download-day "2025-01-02")
(def fetched-at "2025-01-02T22:00:00Z")
(def early-fetched-at "2024-06-01T22:00:00Z")

(def trading-days
  (->> (LocalDate/parse first-day)
       (iterate #(.plusDays ^LocalDate % 1))
       (take-while #(not (.isAfter ^LocalDate % (LocalDate/parse download-day))))
       (remove #(> (.getValue (.getDayOfWeek ^LocalDate %)) 5))
       (mapv str)))

(defn- epoch [date hour-utc]
  (+ (.toEpochSecond (.atStartOfDay (LocalDate/parse date) ZoneOffset/UTC)) (* 3600 hour-utc)))

(defn- round [x places]
  (let [m (Math/pow 10 places)] (/ (Math/round (* (double x) m)) m)))

(defn- plus-days [date n] (str (.plusDays (LocalDate/parse date) (long n))))

;; ============ Prices ============

(def ^:private securities
  {"ALFA" {:seed 11 :start 110.0 :drift 0.0002 :beta 1.1 :vol 0.012 :currency "USD" :name "Alfa Corp"
           :splits {"2024-08-01" 4.0}
           :dividends (into {} (for [y [2022 2023 2024] md ["-02-14" "-05-16" "-08-15" "-11-14"]]
                                 [(str y md) 0.10]))} ; per share of the final basis: 0.40 as paid before the split
   "BETA" {:seed 12 :start 5.0 :drift 0.0003 :beta 1.4 :vol 0.02 :currency "USD" :name "Beta Inc"}
   "GAMA" {:seed 13 :start 30.0 :drift 0.0004 :beta 0.8 :vol 0.01 :currency "USD" :name "Gama SE ADR"}
   "DLTA" {:seed 14 :start 15.0 :drift 0.0 :beta 1.0 :vol 0.015 :currency "USD" :name "Delta Ltd" :no-close #{"2023-07-03"}}
   "IDX" {:seed 15 :start 1000.0 :drift 0.0 :beta 1.0 :vol 0.0 :currency "USD" :name "Sample Index" :type "INDEX"}})

(defn- split-factor-after [splits date]
  (reduce * 1.0 (keep (fn [[d r]] (when (pos? (compare d date)) r)) splits)))

(defn- known-splits
  "The splits of `splits` that had happened by `as-of`."
  [splits as-of]
  (filter (fn [[d _]] (pos? (compare as-of d))) splits))

(def ^:private market-returns
  "The daily log return of the market every security loads on: 0.03% a day
   with 0.9% volatility."
  (let [rng (Random. 7)]
    (mapv (fn [_] (+ 0.0003 (* 0.009 (.nextGaussian rng)))) trading-days)))

(defn- weekday-bars
  "Bars in the share basis of `as-of`, as Yahoo serves them on that day:
   the as-traded price divided by the splits known by then. The random walk
   is the price per share of the final basis (after every split): `beta`
   times the market's return plus `drift` and noise of volatility `vol`."
  [{:keys [seed start drift beta vol splits no-close]} as-of]
  (let [rng (Random. seed)
        known (known-splits splits as-of)]
    (loop [days trading-days market market-returns economic start bars []]
      (if-let [d (first days)]
        (let [economic (* economic (Math/exp (+ drift (* beta (first market)) (* vol (.nextGaussian rng)))))
              traded (* economic (split-factor-after splits d))
              f (split-factor-after known d)
              close (round (/ traded f) 4)
              open (round (* close (+ 1.0 (* 0.004 (.nextGaussian rng)))) 4)
              high (round (* (max open close) (+ 1.0 (Math/abs (* 0.006 (.nextGaussian rng))))) 4)
              low (round (* (min open close) (- 1.0 (Math/abs (* 0.006 (.nextGaussian rng))))) 4)
              volume (long (* (+ 500000 (.nextInt rng 1000000)) f))]
          (recur (rest days) (rest market) economic
                 (conj bars {:timestamp (epoch d 14) :open open :high high :low low
                             :close (when-not (contains? no-close d) close)
                             :volume volume :adj-close nil})))
        bars))))

(defn- chart [ticker as-of]
  (let [{:keys [currency name type splits dividends] :as s} (securities ticker)
        known (known-splits splits as-of)]
    {:meta {:symbol ticker :currency currency :exchange-name "NMS" :full-exchange-name "NasdaqGS"
            :instrument-type (or type "EQUITY") :first-trade-date (epoch first-day 14)
            :timezone "EST" :exchange-timezone-name "America/New_York" :gmt-offset -18000
            :price-hint 2 :long-name name :short-name name}
     :bars (vec (take-while #(< (:timestamp %) (epoch (plus-days (subs as-of 0 10) 1) 0))
                            (weekday-bars s (subs as-of 0 10))))
     :events {:dividends (vec (for [[d amount] (sort dividends) :when (pos? (compare as-of d))]
                                {:amount (round (/ (* amount (split-factor-after splits d)) (split-factor-after known d)) 6)
                                 :date (epoch d 14)}))
              :splits (vec (for [[d r] (sort known)]
                             {:date (epoch d 14) :numerator r :denominator 1.0 :splitRatio (str (long r) ":1")}))}}))

;; ============ Statements ============

(def ^:private statement-of
  {"TotalRevenue" :income-statement "GrossProfit" :income-statement "OperatingIncome" :income-statement
   "NetIncome" :income-statement "EBITDA" :income-statement "DilutedEPS" :income-statement
   "DilutedAverageShares" :income-statement
   "TotalAssets" :balance-sheet "CurrentAssets" :balance-sheet "CurrentLiabilities" :balance-sheet
   "LongTermDebt" :balance-sheet "TotalDebt" :balance-sheet "StockholdersEquity" :balance-sheet
   "CommonStockEquity" :balance-sheet "CashAndCashEquivalents" :balance-sheet
   "OrdinarySharesNumber" :balance-sheet
   "OperatingCashFlow" :cash-flow "FreeCashFlow" :cash-flow})

(def ^:private field-keyword
  {"TotalRevenue" :total-revenue "GrossProfit" :gross-profit "OperatingIncome" :operating-income
   "NetIncome" :net-income "EBITDA" :ebitda "DilutedEPS" :diluted-eps
   "DilutedAverageShares" :diluted-average-shares "TotalAssets" :total-assets
   "CurrentAssets" :current-assets "CurrentLiabilities" :current-liabilities
   "LongTermDebt" :long-term-debt "TotalDebt" :total-debt "StockholdersEquity" :stockholders-equity
   "CommonStockEquity" :common-stock-equity "CashAndCashEquivalents" :cash-and-cash-equivalents
   "OrdinarySharesNumber" :ordinary-shares-number "OperatingCashFlow" :operating-cash-flow
   "FreeCashFlow" :free-cash-flow})

(def ^:private companies
  "Annual figures per fiscal year (calendar years), in the final share basis."
  {"ALFA" {:currency "USD" :revenue 1000e6 :growth 0.10 :margins {:gp 0.45 :oi 0.20 :ni 0.15 :ebitda 0.25}
           :shares 40e6 :buyback 0.01 :report-days 30 :annual-report-days 40 :report-hour 21}
   "BETA" {:currency "USD" :revenue 300e6 :growth 0.25 :margins {:gp 0.30 :oi 0.05 :ni 0.08 :ebitda 0.10}
           :losses-until 2022 :shares 120e6 :buyback -0.03 :report-days 35 :annual-report-days 50 :report-hour 4}
   "GAMA" {:currency "EUR" :revenue 2000e6 :growth 0.04 :margins {:gp 0.55 :oi 0.18 :ni 0.12 :ebitda 0.24}
           :shares 150e6 :buyback 0.0 :report-days 28 :annual-report-days 45 :report-hour 12}})

(def ^:private seasonal [1.0 0.95 1.0 1.05])

(defn- year-figures [{:keys [revenue growth margins losses-until shares buyback]} year]
  (let [n (- year 2019)
        rev (* revenue (Math/pow (+ 1 growth) n))
        ni-margin (if (and losses-until (<= year losses-until)) -0.05 (:ni margins))
        sh (* shares (Math/pow (- 1 buyback) n))
        ta (* 2.0 rev)]
    {"TotalRevenue" rev "GrossProfit" (* rev (:gp margins)) "OperatingIncome" (* rev (:oi margins))
     "NetIncome" (* rev ni-margin) "EBITDA" (* rev (:ebitda margins))
     "DilutedAverageShares" sh "DilutedEPS" (/ (* rev ni-margin) sh)
     "TotalAssets" ta "CurrentAssets" (* 0.6 ta) "CurrentLiabilities" (* 0.3 ta)
     "LongTermDebt" (* 0.2 ta) "TotalDebt" (* 0.25 ta) "StockholdersEquity" (* 0.45 ta)
     "CommonStockEquity" (* 0.45 ta) "CashAndCashEquivalents" (* 0.1 ta) "OrdinarySharesNumber" sh
     "OperatingCashFlow" (* 1.2 rev (Math/abs ni-margin)) "FreeCashFlow" (* 0.9 rev ni-margin)}))

(def ^:private flow? #(not= :balance-sheet (statement-of %)))

(defn- quarter-figures [company year q]
  (let [annual (year-figures company year)]
    (into {} (map (fn [[f v]]
                    [f (cond
                         (= f "DilutedEPS") (/ (* (get annual "NetIncome") 0.25 (nth seasonal q))
                                               (get annual "DilutedAverageShares"))
                         (= f "DilutedAverageShares") v
                         (flow? f) (* v 0.25 (nth seasonal q))
                         :else (* v (+ 0.97 (* 0.01 q))))]))
          annual)))

(defn- quarter-end [year q] (nth [(str year "-03-31") (str year "-06-30") (str year "-09-30") (str year "-12-31")] q))

(defn- report-epoch [{:keys [report-days annual-report-days report-hour]} period-end annual?]
  (epoch (plus-days period-end (if annual? annual-report-days report-days)) report-hour))

(defn- reported-by? [company period-end annual? as-of]
  (< (report-epoch company period-end annual?) (.getEpochSecond (Instant/parse as-of))))

(defn- in-basis
  "A final-basis figure in the share basis of a snapshot taken at `as-of`."
  [ticker yahoo-field value as-of]
  (let [later (split-factor-after (filter (fn [[d _]] (pos? (compare d (subs as-of 0 10))))
                                          (:splits (securities ticker)))
                                  "0000")]
    (case (build/field-kind yahoo-field)
      :per-share (* value later)
      :shares (/ value later)
      value)))

(defn- statement-rows [ticker as-of restate?]
  (let [company (companies ticker)
        quarters (->> (for [y (range 2019 2025) q (range 4)] [y q])
                      (filter (fn [[y q]] (reported-by? company (quarter-end y q) (= q 3) as-of)))
                      (take-last 5))
        years (take-last 4 (filter #(reported-by? company (str % "-12-31") true as-of) (range 2019 2025)))
        row (fn [f freq pe ptype v]
              {:ticker ticker :statement (statement-of f) :field (field-keyword f) :yahoo-field f
               :frequency freq :period-end pe :period-type ptype :currency (:currency company)
               :value (in-basis ticker f v as-of)})
        restated (fn [f pe v] (if (and restate? (= f "NetIncome") (= pe "2023-12-31")) (* v 1.02) v))
        q-rows (for [[y q] quarters [f v] (quarter-figures company y q)]
                 (row f :quarterly (quarter-end y q) "3M" (restated f (quarter-end y q) v)))
        a-rows (for [y years [f v] (year-figures company y)]
                 (row f :annual (str y "-12-31") "12M" v))
        ttm (fn [[y q]] (let [qs (take-last 4 (filter #(neg? (compare % [y (inc q)])) (for [y (range 2019 2025) q (range 4)] [y q])))]
                          (for [f (keys statement-of) :when (and (flow? f) (not= f "DilutedAverageShares"))]
                            (row f :trailing (quarter-end y q) "TTM"
                                 (reduce + (map (fn [[y q]] (get (quarter-figures company y q) f)) qs))))))]
    (vec (concat q-rows a-rows (ttm (last quarters))))))

;; ============ Earnings ============

(defn- quarter-periods [company as-of]
  (->> (for [y (range 2019 2025) q (range 4)] [y q])
       (filter (fn [[y q]] (reported-by? company (quarter-end y q) (= q 3) as-of)))))

(defn- earnings-history [ticker as-of]
  (let [company (companies ticker)]
    (vec (for [[y q] (take-last 4 (quarter-periods company as-of))
               :let [pe (quarter-end y q)
                     at (report-epoch company pe (= q 3))
                     actual (in-basis ticker "DilutedEPS" (get (quarter-figures company y q) "DilutedEPS") as-of)]]
           {:ticker ticker :period-end pe :reported-date (str (.toLocalDate (.atOffset (Instant/ofEpochSecond at) ZoneOffset/UTC)))
            :reported-at at :fiscal-quarter (str (inc q) "Q" y) :calendar-quarter (str (inc q) "Q" y)
            :eps-actual (round actual 4) :eps-estimate (round (* 0.97 actual) 4)
            :eps-difference (round (* 0.03 actual) 4) :surprise-pct 3.09}))))

(defn- earnings-dates [ticker as-of until]
  (let [company (companies ticker)
        timing ({"ALFA" "TAS" "BETA" "TNS" "GAMA" "BMO"} ticker)]
    (vec (concat
          (for [[y q] (quarter-periods company as-of)
                :let [pe (quarter-end y q)
                      at (report-epoch company pe (= q 3))
                      eps (in-basis ticker "DilutedEPS" (get (quarter-figures company y q) "DilutedEPS") as-of)]
                :when (or (nil? until) (neg? (compare (str (Instant/ofEpochSecond at)) until)))]
            {:ticker ticker :company-name ticker :event-name nil :announced-at (str (Instant/ofEpochSecond at))
             :announced-epoch at :timing timing :eps-estimate (round (* 0.97 eps) 4) :eps-actual (round eps 4)
             :surprise-pct 3.09 :timezone "EST" :gmt-offset-ms -18000000})
          [{:ticker ticker :company-name ticker :event-name " Annual Shareholders Meeting"
            :announced-at "2023-03-01T17:00:00Z" :announced-epoch (epoch "2023-03-01" 17) :timing "TAS"
            :eps-estimate nil :eps-actual nil :surprise-pct nil :timezone "EST" :gmt-offset-ms -18000000}]))))

;; ============ Snapshots ============

(defn snapshot [ticker as-of & {:keys [restate?]}]
  (let [fundamentals? (contains? companies ticker)]
    {:format 1
     :ticker ticker
     :fetched-at as-of
     :source {:library "clj-yfinance" :version "synthetic"}
     :request {:start first-day :fundamentals? fundamentals?}
     :chart (chart ticker as-of)
     :statements (if fundamentals? (statement-rows ticker as-of restate?) [])
     :earnings-history (if (and fundamentals? (not= ticker "BETA")) (earnings-history ticker as-of) [])
     :earnings-dates (if fundamentals? (earnings-dates ticker as-of (when (= ticker "BETA") "2023-06-01")) [])
     :errors (cond-> {}
               (= ticker "BETA") (assoc :earnings-history {:type :missing-data :message "No quoteSummary result in response"})
               (not fundamentals?) (assoc :statements {:type :no-data :message "No statement data"}))}))

(def ^:private missing-snapshot
  {:format 1 :ticker "ZZZZ" :fetched-at fetched-at :source {:library "clj-yfinance" :version "synthetic"}
   :request {:start first-day} :chart nil :statements [] :earnings-history [] :earnings-dates []
   :errors {:chart {:type :http-error :status 404 :message "No data found, symbol may be delisted"}}})

(defn- fx-snapshot []
  (let [rng (Random. 99)]
    {:format 1 :ticker "EURUSD=X" :from "EUR" :to "USD" :fetched-at fetched-at
     :source {:library "clj-yfinance" :version "synthetic"}
     :chart {:meta {:symbol "EURUSD=X" :currency "USD" :instrument-type "CURRENCY"
                    :exchange-timezone-name "Europe/London" :gmt-offset 0}
             :bars (loop [days trading-days rate 1.13 bars []]
                     (if-let [d (first days)]
                       (let [rate (* rate (Math/exp (* 0.004 (.nextGaussian rng))))]
                         (recur (rest days) rate (conj bars {:timestamp (epoch d 0) :open (round rate 5) :high (round rate 5)
                                                             :low (round rate 5) :close (round rate 5) :volume 0})))
                       bars))
             :events {:dividends [] :splits []}}}))

(defn write-sample-snapshots
  "Writes the raw snapshots under <dir>/raw. Returns dir."
  [dir]
  (let [raw (io/file dir "raw")
        put! (fn [d snap] (build/write-edn-file! (io/file d (build/snapshot-file-name (:fetched-at snap))) snap))]
    (put! (io/file raw "ALFA") (snapshot "ALFA" early-fetched-at))
    (doseq [t (keys securities)]
      (put! (io/file raw (build/encode-ticker t)) (snapshot t fetched-at :restate? (= t "ALFA"))))
    (put! (io/file raw "ZZZZ") missing-snapshot)
    (put! (io/file raw build/fx-dir-name "EURUSD=X") (fx-snapshot))
    dir))

(defn -main
  "Rewrites resources/sample-yahoo (or the given directory)."
  [& [dir]]
  (let [dir (io/file (or dir "resources/sample-yahoo"))]
    (doseq [f (reverse (file-seq (io/file dir "raw")))] (io/delete-file f true))
    (write-sample-snapshots dir)
    (println "Sample Yahoo snapshots written to" (str (io/file dir "raw")))))
