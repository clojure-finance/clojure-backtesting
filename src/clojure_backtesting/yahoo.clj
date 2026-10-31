(ns clojure-backtesting.yahoo
  "Yahoo Finance as a data source, through clj-yfinance.

   Three steps, each usable on its own:

     (download-yahoo! \"data/tech\" [\"AAPL\" \"MSFT\" \"NVDA\" \"SPY\"] :start \"2015-01-01\")
     (build-yahoo-dataset! \"data/tech\")
     (load-yahoo-dataset \"data/tech\")

   download-yahoo! saves what Yahoo returns, unchanged, as one snapshot per
   security and download under <dir>/raw; nothing is lost by building, and
   later downloads add snapshots rather than replace them, so the
   fundamentals history grows beyond the few years Yahoo shows at a time.
   build-yahoo-dataset! turns the snapshots into the backtester's layout
   (see clojure-backtesting.yahoo.build), offline. load-yahoo-dataset
   loads prices as the main dataset and the point-in-time fundamentals (see
   clojure-backtesting.fundamentals).

   Yahoo only has securities that still trade, so a universe picked today
   leaves out the companies that failed or were taken over since: results
   carry survivorship bias. Its statements cover about four fiscal years
   and five quarters, and its values are as restated today, not as first
   reported."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-yfinance.core :as yf]
            [clj-yfinance.experimental.earnings :as yf-earnings]
            [clj-yfinance.experimental.timeseries :as yf-ts]
            [clojure-backtesting.data :as data]
            [clojure-backtesting.fundamentals :as fundamentals]
            [clojure-backtesting.yahoo.build :as build])
  (:import [java.time Instant LocalDate ZoneOffset]
           [java.util.concurrent Callable Executors TimeUnit]))

(def default-fields
  "The Yahoo statement fields downloaded unless :fields says otherwise:
   the inputs of the ratios in clojure-backtesting.fundamentals and of most
   textbook factors."
  ["TotalRevenue" "CostOfRevenue" "GrossProfit" "ResearchAndDevelopment"
   "SellingGeneralAndAdministration" "OperatingExpense" "OperatingIncome" "EBIT" "EBITDA"
   "InterestExpense" "PretaxIncome" "TaxProvision" "NetIncome" "NetIncomeCommonStockholders"
   "BasicEPS" "DilutedEPS" "BasicAverageShares" "DilutedAverageShares"
   "TotalAssets" "CurrentAssets" "CashAndCashEquivalents" "CashCashEquivalentsAndShortTermInvestments"
   "AccountsReceivable" "Inventory" "NetPPE" "Goodwill" "TotalLiabilitiesNetMinorityInterest"
   "CurrentLiabilities" "CurrentDebt" "LongTermDebt" "TotalDebt" "NetDebt" "StockholdersEquity"
   "CommonStockEquity" "RetainedEarnings" "WorkingCapital" "InvestedCapital" "TangibleBookValue"
   "OrdinarySharesNumber" "ShareIssued" "TreasurySharesNumber"
   "OperatingCashFlow" "CapitalExpenditure" "FreeCashFlow" "DepreciationAndAmortization"
   "StockBasedCompensation" "CashDividendsPaid" "RepurchaseOfCapitalStock" "IssuanceOfCapitalStock"
   "IssuanceOfDebt" "RepaymentOfDebt" "InvestingCashFlow" "FinancingCashFlow" "ChangeInWorkingCapital"])

(def ^:private statement-instrument-types
  "Instrument types Yahoo has financial statements for."
  #{"EQUITY"})

;; ============ Downloading ============

(defn- date->instant ^Instant [date]
  (.toInstant (.atStartOfDay (LocalDate/parse date) ZoneOffset/UTC)))

(def ^:private print-lock (Object.))

(defn- say [& xs]
  (locking print-lock (apply println xs)))

(defn- envelope-data
  "[data error] of a clj-yfinance verbose result."
  [result]
  (if (:ok? result) [(:data result) nil] [nil (:error result)]))

(defn- chart-request
  "Daily bars from `start` (or the first trade) up to today. Always up to
   today: Yahoo divides old quotes by every split until today but returns
   only the splits inside the requested range, so a range ending earlier
   could not be restated as traded."
  [ticker start]
  (let [result (apply yf/fetch-historical* ticker
                      (concat [:interval "1d" :auto-adjust false :events true]
                              (if start [:start (date->instant start)] [:period "max"])))]
    (if (:ok? result)
      [{:meta (:meta result)
        :bars (:data result)
        :events {:dividends (vec (vals (get-in result [:events :dividends])))
                 :splits (vec (vals (get-in result [:events :splits])))}}
       nil]
      [nil (:error result)])))

(defn fetch-snapshot
  "Downloads one security from Yahoo: daily bars with dividends and splits
   and, for equities when `fundamentals?`, the statements (`fields`, Yahoo
   names), the earnings history and the earnings announcement dates. Waits
   `pause-ms` between requests. Returns the snapshot map; failures are
   recorded under :errors."
  [ticker {:keys [start fundamentals? fields pause-ms]}]
  (let [pause! #(when (pos? pause-ms) (Thread/sleep (long pause-ms)))
        fetched-at (str (.truncatedTo (Instant/now) java.time.temporal.ChronoUnit/SECONDS))
        [chart chart-error] (chart-request ticker start)
        statements? (and fundamentals? chart
                         (contains? statement-instrument-types (get-in chart [:meta :instrument-type])))
        part (fn [f] (pause!) (envelope-data (f)))
        [statements st-error] (if statements?
                                (part #(yf-ts/fetch-statements* ticker :fields (vec fields)
                                                                :frequencies #{:annual :quarterly :trailing}))
                                [[] nil])
        [history h-error] (if statements? (part #(yf-earnings/fetch-earnings-history* ticker)) [[] nil])
        [dates d-error] (if statements? (part #(yf-earnings/fetch-earnings-dates* ticker :limit 100)) [[] nil])]
    {:format 1
     :ticker ticker
     :fetched-at fetched-at
     :source {:library "clj-yfinance"}
     :request {:start start :fundamentals? (boolean statements?) :fields (when statements? (vec fields))}
     :chart chart
     :statements (or statements [])
     :earnings-history (or history [])
     :earnings-dates (or dates [])
     :errors (into {} (remove (comp nil? val))
                   {:chart chart-error :statements st-error :earnings-history h-error :earnings-dates d-error})}))

(defn- snapshot-dir [raw-dir ticker]
  (io/file raw-dir (build/encode-ticker ticker)))

(defn- downloaded-today? [dir]
  (let [today (str/replace (str (LocalDate/now ZoneOffset/UTC)) "-" "")]
    (some #(str/starts-with? (.getName ^java.io.File %) today) (.listFiles (io/file dir)))))

(defn- run-bounded
  "Runs the no-argument functions `fs` on `n` threads; returns their results in order."
  [n fs]
  (let [pool (Executors/newFixedThreadPool (max 1 n))]
    (try
      (mapv #(.get ^java.util.concurrent.Future %) (.invokeAll pool (mapv (fn [f] (reify Callable (call [_] (f)))) fs)))
      (finally
        (.shutdown pool)
        (.awaitTermination pool 1 TimeUnit/MINUTES)))))

(defn- fx-pairs-needed
  "[from to] pairs that convert the reporting currencies of the latest
   snapshots into their trading currencies."
  [raw-dir]
  (set (for [[_ snaps] (:securities (build/read-snapshots raw-dir))
             :let [snap (last snaps)
                   [major _] (build/major-currency (get-in snap [:chart :meta :currency]))]
             c (distinct (keep :currency (:statements snap)))
             :when (and major (not= c major))]
         [c major])))

(defn- fetch-fx-snapshot [[from to] start]
  (let [ticker (build/fx-ticker from to)
        fetched-at (str (.truncatedTo (Instant/now) java.time.temporal.ChronoUnit/SECONDS))
        [chart error] (chart-request ticker start)]
    {:format 1 :ticker ticker :from from :to to :fetched-at fetched-at
     :source {:library "clj-yfinance"} :chart chart :errors (if error {:chart error} {})}))

(defn download-yahoo!
  "Downloads `tickers` from Yahoo Finance into <dir>/raw, one snapshot per
   security, plus the exchange rates needed to state foreign-currency
   fundamentals in each security's trading currency. A security already
   downloaded today (UTC) is skipped unless :force? is true, so an
   interrupted download can simply be run again. Prices run up to today
   (end them earlier with build-yahoo-dataset!'s :end). Options:
     :start            yyyy-MM-dd start of the price history (all of it by default)
     :fundamentals?    also fetch statements and earnings dates (true)
     :fields           Yahoo statement fields (default-fields), or :all for
                       clj-yfinance's whole catalog
     :concurrency      securities downloaded at once (2; Yahoo rate-limits)
     :pause-ms         wait between requests per thread (300)
     :force?           download again even if done today
   Returns {:downloaded [...] :skipped [...] :failed {ticker error} :fx [...]}."
  [dir tickers & {:keys [start fundamentals? fields concurrency pause-ms force?]
                  :or {fundamentals? true fields default-fields concurrency 2 pause-ms 300}}]
  (let [raw (io/file dir "raw")
        fields (if (= fields :all) (vec (mapcat val yf-ts/statement-fields)) fields)
        opts {:start start :fundamentals? fundamentals? :fields fields :pause-ms pause-ms}
        tickers (vec (distinct tickers))
        todo (if force? tickers (remove #(downloaded-today? (snapshot-dir raw %)) tickers))
        results (run-bounded concurrency
                             (for [[i t] (map-indexed vector todo)]
                               (fn []
                                 (let [snap (fetch-snapshot t opts)
                                       f (io/file (snapshot-dir raw t) (build/snapshot-file-name (:fetched-at snap)))]
                                   (build/write-edn-file! f snap)
                                   (say (format "[%d/%d] %s: %s" (inc i) (count todo) t
                                                (if-let [e (:errors snap)]
                                                  (if (seq e) (str "errors in " (str/join ", " (map name (keys e)))) "ok")
                                                  "ok")))
                                   snap))))
        fx (when fundamentals?
             (vec (for [pair (sort (fx-pairs-needed raw))
                        :let [d (io/file raw build/fx-dir-name (build/encode-ticker (apply build/fx-ticker pair)))]
                        :when (or force? (not (downloaded-today? d)))]
                    (let [snap (fetch-fx-snapshot pair start)]
                      (build/write-edn-file! (io/file d (build/snapshot-file-name (:fetched-at snap))) snap)
                      (say (str "FX " (:ticker snap) ": " (if (seq (:errors snap)) "failed" "ok")))
                      pair))))]
    {:downloaded (mapv :ticker results)
     :skipped (vec (remove (set todo) tickers))
     :failed (into (sorted-map)
                   (keep (fn [s] (when-let [e (get-in s [:errors :chart])] [(:ticker s) e])))
                   results)
     :fx (or fx [])}))

;; ============ Building and loading ============

(defn build-yahoo-dataset!
  "Builds main/, fundamentals.edn and dataset.edn from the snapshots in
   <dir>/raw, into `dir` or the directory given as :out-dir. Takes the
   options of clojure-backtesting.yahoo.build/build-dataset! as well.
   Prints the warnings and returns the dataset description."
  [dir & {:keys [out-dir] :as opts}]
  (let [info (apply build/build-dataset! (io/file dir "raw") (or out-dir dir)
                    (mapcat identity (dissoc opts :out-dir)))]
    (doseq [w (:warnings info)]
      (println "Warning:" (case (:type w)
                            :no-prices (str (:ticker w) " has no prices: " (get-in w [:error :message]))
                            :missing-fx (str "no exchange rate for " (str/join "/" (:pair w)))
                            :estimated-report-dates (str (:ticker w) ": " (:periods w)
                                                         " report dates are estimated from the reporting lag")
                            (pr-str w))))
    info))

(defn load-yahoo-dataset
  "Loads a built Yahoo dataset: its prices as the main dataset and its
   fundamentals. A Compustat-style dataset loaded before is unloaded, since
   it belongs to another universe. Returns a description of the date range."
  [dir]
  (alter-var-root #'data/headers2 (constantly nil))
  (alter-var-root #'data/data-files2 (constantly {}))
  (let [msg (data/load-dataset (str (io/file dir "main")) "main")
        f (io/file dir "fundamentals.edn")]
    (if (.exists f)
      (str msg "; " (fundamentals/load-fundamentals f))
      (do (fundamentals/clear-fundamentals!) msg))))

(defn yahoo-dataset!
  "Downloads `tickers` into `dir`, builds the dataset and loads it, in one
   call. Takes the options of download-yahoo! and build-yahoo-dataset!."
  [dir tickers & {:as opts}]
  (let [download-keys [:fundamentals? :fields :concurrency :pause-ms :force?]
        result (apply download-yahoo! dir tickers (mapcat identity (select-keys opts (conj download-keys :start))))]
    (when (seq (:failed result))
      (println "Not downloaded:" (str/join ", " (keys (:failed result)))))
    (apply build-yahoo-dataset! dir (mapcat identity (apply dissoc opts download-keys)))
    (load-yahoo-dataset dir)))

(defn dataset-info
  "The description build-yahoo-dataset! wrote to <dir>/dataset.edn."
  [dir]
  (build/read-edn-file (io/file dir "dataset.edn")))
