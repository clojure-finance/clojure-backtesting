(ns clojure-backtesting.fundamentals
  "Point-in-time fundamentals for strategies: statement values as they were
   known on the backtest date, and valuation and quality ratios built on
   them.

   A value is visible from the day its report became public (see
   `clojure-backtesting.yahoo.build/report-date`), never earlier. Values come
   back in the share basis of the backtest date, so per-share figures and
   share counts match the day's quoted price even when a later split has
   been applied to the source data, and in the security's trading currency
   (major unit: pounds, not pence), converted at the previous day's FX rate
   when the company reports in another currency. A value that cannot be stated that
   way (no FX rate, nothing public yet, or a period that ended more than
   FUNDAMENTALS-MAX-AGE-MONTHS ago) is nil, never guessed.

   Fields are kebab-case Yahoo statement fields such as :total-revenue,
   :net-income, :diluted-eps, :stockholders-equity or
   :ordinary-shares-number. Frequencies:
     :quarterly  the latest quarter
     :annual     the latest fiscal year
     :trailing   Yahoo's own trailing-twelve-month figure
     :ttm        twelve months: Yahoo's trailing figure, the sum of the last
                 four quarters, or the last fiscal year, whichever ends
                 latest (flow fields only; share counts and rates take the
                 latest quarter instead of a sum)
     :latest     the latest quarter or fiscal year, whichever ends latest
   With no frequency, income-statement and cash-flow fields use :ttm and
   balance-sheet fields :latest."
  (:require [clojure.edn :as edn]
            [clojure-backtesting.counter :refer [get-date]]
            [clojure-backtesting.data-management :refer [get-permno-price]]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.yahoo.build :refer [major-currency]])
  (:import [java.time LocalDate]
           [java.time.temporal ChronoUnit]))

;; ============ Store ============

(def fundamentals-store
  "{security-id entry}, as written by clojure-backtesting.yahoo.build."
  (atom {}))

(def fx-rates
  "{[from to] (sorted-map date rate)}: units of `to` per unit of `from`."
  (atom {}))

(def ^:private warned (atom #{}))

(defn- warn-once! [k msg]
  (when-not (contains? (deref warned) k)
    (swap! warned conj k)
    (println msg)))

(defn load-fundamentals
  "Loads a fundamentals.edn file (as build-yahoo-dataset! writes it),
   replacing any fundamentals loaded before."
  [path]
  (let [{:keys [store fx]} (edn/read-string (slurp (str path)))]
    (reset! fundamentals-store store)
    (reset! fx-rates (into {} (map (fn [[pair series]] [pair (into (sorted-map) series)])) fx))
    (reset! warned #{})
    (str "Fundamentals: " (count store) " securities, "
         (reduce + (map #(count (:reports %)) (vals store))) " reports, "
         (count fx) " FX series")))

(defn clear-fundamentals!
  "Unloads the fundamentals."
  []
  (reset! fundamentals-store {})
  (reset! fx-rates {})
  (reset! warned #{}))

(defn available-fields
  "The fundamental fields known for `permno`, sorted."
  [permno]
  (sort (keys (get-in (deref fundamentals-store) [permno :fields]))))

;; ============ Basis and currency ============

(defn- minus-months [date months]
  (str (.minusMonths (LocalDate/parse date) (long months))))

(defn- split-factor-after
  "Product of the split ratios after `date`: shares of the store's basis per
   share held on `date`."
  [splits date]
  (transduce (comp (filter #(pos? (compare (first %) date))) (map second)) * 1.0 splits))

(defn fx-rate
  "Units of `to` per unit of `from` known on `date`: the latest rate dated
   before it, since a day's FX close comes after many markets close. 1.0
   when the currencies are the same, nil when unknown."
  [from to date]
  (cond
    (or (nil? from) (nil? to) (= from to)) 1.0
    :else (when-let [series (get (deref fx-rates) [from to])]
            (some-> (first (rsubseq series < date)) val double))))

(defn- in-trading-terms
  "`value` of `kind`, reported in `currency` in the store's share basis,
   restated in the share basis of `date` and in the major unit of
   `entry`'s trading currency. nil when the FX rate is unknown."
  [entry kind value currency date]
  (let [basis (split-factor-after (:splits entry) date)
        [major _] (major-currency (:trading-currency entry))
        v (case kind
            :per-share (* value basis)
            :shares (/ value basis)
            value)]
    (if (#{:shares :ratio} kind)
      v
      (if-let [rate (fx-rate currency major date)]
        (* v rate)
        (warn-once! [currency major]
                    (str "No FX rate from " currency " to " major
                         "; fundamentals reported in " currency " are treated as unavailable."))))))

;; ============ Lookups ============

(defn- flow-field? [entry field]
  (not= :balance-sheet (get-in entry [:fields field :statement])))

(defn- report [entry frequency period-end]
  (get-in entry [:reports [frequency period-end]]))

(defn- public-on? [entry frequency period-end date]
  (when-let [r (report entry frequency period-end)]
    (not (pos? (compare (:public-date r) date)))))

(defn- public-values
  "[[period-end value] ...] of `field` at `frequency` public on `date`, newest first."
  [entry field frequency date]
  (->> (rseq (get-in entry [:fields field :values frequency] []))
       (filter (fn [[pe _]] (public-on? entry frequency pe date)))))

(defn- info
  "The full description of one observation, restated for `date`."
  [entry field frequency period-end value date & [source]]
  (let [kind (get-in entry [:fields field :kind] :amount)
        r (report entry (if (= frequency :sum-of-quarters) :quarterly frequency) period-end)
        currency (:currency r)
        converted (in-trading-terms entry kind value currency date)]
    (when (some? converted)
      {:field field
       :value converted
       :frequency (if (= frequency :sum-of-quarters) :ttm frequency)
       :source (or source frequency)
       :period-end period-end
       :report-date (:report-date r)
       :public-date (:public-date r)
       :report-date-source (:source r)
       :reported-value value
       :reported-currency currency
       :currency (first (major-currency (:trading-currency entry)))})))

(defn- consecutive-quarters? [period-ends]
  (every? (fn [[a b]] (<= 70 (.between ChronoUnit/DAYS (LocalDate/parse b) (LocalDate/parse a)) 110))
          (partition 2 1 period-ends)))

(defn- ttm-candidates
  "Twelve-month candidates; share counts and rates are not summed over
   quarters but taken from the latest quarter."
  [entry field date]
  (let [trailing (first (public-values entry field :trailing date))
        quarters (take 4 (public-values entry field :quarterly date))
        annual (first (public-values entry field :annual date))
        additive? (#{:amount :per-share} (get-in entry [:fields field :kind] :amount))]
    (cond-> []
      trailing (conj {:frequency :trailing :period-end (first trailing) :value (second trailing) :rank 0})
      (and (not additive?) (seq quarters))
      (conj {:frequency :quarterly :period-end (ffirst quarters) :value (second (first quarters)) :rank 1})
      (and additive? (= 4 (count quarters)) (consecutive-quarters? (map first quarters)))
      (conj {:frequency :sum-of-quarters :period-end (ffirst quarters)
             :value (reduce + (map second quarters)) :rank 1})
      annual (conj {:frequency :annual :period-end (first annual) :value (second annual) :rank 2}))))

(defn- choose
  "The candidate with the latest period end; ties go to the lowest :rank."
  [candidates]
  (first (sort-by (juxt (comp - #(.toEpochDay (LocalDate/parse %)) :period-end) :rank) candidates)))

(defn fundamental-info-as-of
  "Like fundamental-info, on `date` rather than the backtest date."
  [date permno field frequency]
  (when-let [entry (get (deref fundamentals-store) permno)]
    (when (get-in entry [:fields field])
      (let [frequency (or frequency (if (flow-field? entry field) :ttm :latest))
            chosen (case frequency
                     :ttm (when (flow-field? entry field) (choose (ttm-candidates entry field date)))
                     :latest (choose (concat (some->> (first (public-values entry field :quarterly date))
                                                      (zipmap [:period-end :value]) (merge {:frequency :quarterly :rank 0}) vector)
                                             (some->> (first (public-values entry field :annual date))
                                                      (zipmap [:period-end :value]) (merge {:frequency :annual :rank 1}) vector)))
                     (some->> (first (public-values entry field frequency date))
                              (zipmap [:period-end :value]) (merge {:frequency frequency})))]
        (when (and chosen (not (neg? (compare (:period-end chosen) (minus-months date FUNDAMENTALS-MAX-AGE-MONTHS)))))
          (info entry field (:frequency chosen) (:period-end chosen) (:value chosen) date
                (when (= frequency :ttm) (:frequency chosen))))))))

(defn fundamental-info
  "The latest public value of `field` for `permno` on the backtest date,
   with where it comes from: {:value :field :frequency :source :period-end
   :report-date :public-date :report-date-source :reported-value
   :reported-currency :currency}. :report-date-source says how the public
   date was found (:earnings-history, :announcement, or :lag when it is an
   assumed reporting lag). nil when nothing is public yet or it is too old."
  ([permno field] (fundamental-info permno field nil))
  ([permno field frequency] (fundamental-info-as-of (get-date) permno field frequency)))

(defn fundamental
  "The latest public value of `field` for `permno` on the backtest date,
   or nil. See the namespace docstring for `frequency`."
  ([permno field] (fundamental permno field nil))
  ([permno field frequency] (:value (fundamental-info permno field frequency))))

(defn fundamental-as-of
  "Like fundamental, on `date` rather than the backtest date."
  ([date permno field] (fundamental-as-of date permno field nil))
  ([date permno field frequency] (:value (fundamental-info-as-of date permno field frequency))))

(defn fundamental-history
  "Up to `n` public values of `field` at `frequency` (:quarterly, :annual or
   :trailing) on the backtest date, newest first, each as fundamental-info
   describes it. Old values are included regardless of their age."
  [permno field frequency n]
  (let [date (get-date)]
    (if-let [entry (get (deref fundamentals-store) permno)]
      (->> (public-values entry field frequency date)
           (keep (fn [[pe v]] (info entry field frequency pe v date)))
           (take n)
           vec)
      [])))

;; ============ Earnings announcements ============

(defn latest-earnings
  "The latest earnings announcement public on the backtest date:
   {:report-date :public-date :period-end :eps-actual :eps-estimate
   :surprise-pct}, or nil. :surprise-pct is in percent."
  [permno]
  (let [date (get-date)]
    (->> (get-in (deref fundamentals-store) [permno :earnings] [])
         rseq
         (filter #(not (pos? (compare (:public-date %) date))))
         first)))

(defn days-since-earnings
  "Calendar days since the latest public earnings announcement, or nil."
  [permno]
  (when-let [{:keys [public-date]} (latest-earnings permno)]
    (.between ChronoUnit/DAYS (LocalDate/parse public-date) (LocalDate/parse (get-date)))))

;; ============ Prices in fundamental terms ============

(defn- price-major
  "Today's price in the major unit of the trading currency."
  [permno]
  (when-let [p (get-permno-price permno)]
    (* (double p) (second (major-currency (get-in (deref fundamentals-store) [permno :trading-currency]))))))

(defn- ratio [a b]
  (when (and a b (not (zero? b)))
    (/ (double a) (double b))))

(defn- pos-ratio
  "a / b when b is positive, else nil: a multiple of negative earnings is meaningless."
  [a b]
  (when (and a b (pos? b))
    (/ (double a) (double b))))

(defn- first-of [permno & fields]
  (some #(fundamental permno %) fields))

(defn shares-outstanding
  "Shares outstanding in today's share basis: the latest ordinary shares
   number, else shares issued, else diluted average shares. nil, with a
   warning, when it differs more than twofold from the count implied by net
   income over diluted EPS: Yahoo counts some multi-class companies in
   another class than the one quoted (Berkshire Hathaway's B shares in A
   equivalents), which would make every ratio on market value wrong."
  [permno]
  (let [shares (first-of permno :ordinary-shares-number :share-issued :diluted-average-shares)
        income (first-of permno :net-income-common-stockholders :net-income)
        eps (fundamental permno :diluted-eps)
        implied (when (and income eps (not (zero? eps))) (/ income eps))]
    (if (and shares implied (pos? shares) (pos? implied)
             (> (max (/ shares implied) (/ implied shares)) 2.0))
      (warn-once! [:shares permno]
                  (format "%s: %.4g shares outstanding but net income over EPS implies %.4g; market value ratios are left out."
                          permno (double shares) (double implied)))
      shares)))

(defn market-cap
  "Today's price times shares outstanding, in the trading currency."
  [permno]
  (when-let [p (price-major permno)]
    (some-> (shares-outstanding permno) (* p))))

(defn- book-equity [permno]
  (first-of permno :common-stock-equity :stockholders-equity))

(defn pe-ratio
  "Price over twelve-month diluted EPS; nil when EPS is not positive."
  [permno]
  (pos-ratio (price-major permno) (fundamental permno :diluted-eps)))

(defn earnings-yield
  "Twelve-month diluted EPS over price (negative for losses)."
  [permno]
  (ratio (fundamental permno :diluted-eps) (price-major permno)))

(defn book-to-market
  "Book equity over market capitalisation."
  [permno]
  (ratio (book-equity permno) (market-cap permno)))

(defn price-to-book
  "Market capitalisation over book equity; nil when equity is not positive."
  [permno]
  (pos-ratio (market-cap permno) (book-equity permno)))

(defn sales-to-price
  "Twelve-month revenue over market capitalisation."
  [permno]
  (ratio (fundamental permno :total-revenue) (market-cap permno)))

(defn free-cash-flow-yield
  "Twelve-month free cash flow over market capitalisation."
  [permno]
  (ratio (fundamental permno :free-cash-flow) (market-cap permno)))

(defn enterprise-value
  "Market capitalisation plus total debt minus cash; nil unless all three are known."
  [permno]
  (let [mcap (market-cap permno)
        debt (fundamental permno :total-debt)
        cash (first-of permno :cash-and-cash-equivalents :cash-cash-equivalents-and-short-term-investments)]
    (when (and mcap debt cash)
      (- (+ mcap debt) cash))))

(defn ev-to-ebitda
  "Enterprise value over twelve-month EBITDA; nil when EBITDA is not positive."
  [permno]
  (pos-ratio (enterprise-value permno) (fundamental permno :ebitda)))

(defn dividend-yield
  "Dividends with an ex-date in the last 365 days over today's price, both
   per share of today's basis. Uses the price history's dividends, so it
   needs no statement data."
  [permno]
  (let [date (get-date)
        {:keys [splits dividends]} (get (deref fundamentals-store) permno)
        start (str (.minusDays (LocalDate/parse date) 365))
        basis (split-factor-after splits date)
        paid (->> dividends
                  (filter (fn [[d _]] (and (pos? (compare d start)) (not (pos? (compare d date))))))
                  (map (fn [[_ amount]] (* amount basis)))
                  (reduce + 0.0))]
    (when (and dividends (get-permno-price permno))
      (ratio paid (get-permno-price permno)))))

(defn return-on-equity
  "Twelve-month net income over book equity; nil when equity is not positive."
  [permno]
  (pos-ratio (fundamental permno :net-income) (book-equity permno)))

(defn return-on-assets
  "Twelve-month net income over total assets."
  [permno]
  (pos-ratio (fundamental permno :net-income) (fundamental permno :total-assets)))

(defn gross-margin
  "Twelve-month gross profit over revenue."
  [permno]
  (pos-ratio (fundamental permno :gross-profit) (fundamental permno :total-revenue)))

(defn operating-margin
  "Twelve-month operating income over revenue."
  [permno]
  (pos-ratio (fundamental permno :operating-income) (fundamental permno :total-revenue)))

(defn net-margin
  "Twelve-month net income over revenue."
  [permno]
  (pos-ratio (fundamental permno :net-income) (fundamental permno :total-revenue)))

(defn debt-to-equity
  "Total debt over book equity; nil when equity is not positive."
  [permno]
  (pos-ratio (fundamental permno :total-debt) (book-equity permno)))

(defn current-ratio
  "Current assets over current liabilities."
  [permno]
  (pos-ratio (fundamental permno :current-assets) (fundamental permno :current-liabilities)))

(defn growth
  "Year-on-year growth of `field` at `frequency` (:annual by default, or
   :quarterly for the latest quarter against the same quarter a year
   before): latest / year-earlier - 1, nil when the year-earlier value is
   missing or not positive."
  ([permno field] (growth permno field :annual))
  ([permno field frequency]
   (let [history (fundamental-history permno field frequency 5)
         latest (first history)
         target (some-> latest :period-end LocalDate/parse (.minusYears 1))
         earlier (when target
                   (first (filter #(<= (Math/abs (.between ChronoUnit/DAYS target (LocalDate/parse (:period-end %)))) 20)
                                  (rest history))))]
     (when (and latest earlier
                (= (:period-end latest) (:period-end (fundamental-info permno field frequency))))
       (some-> (pos-ratio (:value latest) (:value earlier)) (- 1.0))))))

;; ============ Piotroski F-score ============

(defn- annual-values
  "{period-end value} of the public fiscal-year values of `field`."
  [permno field]
  (into {} (map (juxt :period-end :value)) (fundamental-history permno field :annual 4)))

(defn piotroski-signals
  "The nine signals of Piotroski's (2000) F-score from the last two fiscal
   years, each true, false, or nil when its inputs are missing:
   :roa (net income over beginning assets is positive), :cfo (operating
   cash flow is positive), :delta-roa (ROA improved), :accrual (cash flow
   exceeds net income), :delta-leverage (long-term debt over assets fell),
   :delta-liquidity (current ratio rose), :no-dilution (shares did not
   increase), :delta-margin (gross margin rose), :delta-turnover (revenue
   over beginning assets rose). Years are aligned on the fiscal years of
   total assets."
  [permno]
  (let [years (mapv :period-end (fundamental-history permno :total-assets :annual 3))
        [ni ta cfo ltd ca cl sh gp rev]
        (map #(annual-values permno %)
             [:net-income :total-assets :operating-cash-flow :long-term-debt :current-assets
              :current-liabilities :ordinary-shares-number :gross-profit :total-revenue])
        at (fn [m i] (some->> (get years i) (get m)))
        roa (fn [i] (ratio (at ni i) (at ta (inc i))))
        cmp (fn [a b f] (when (and a b) (f a b)))]
    {:roa (some-> (roa 0) pos?)
     :cfo (some-> (at cfo 0) pos?)
     :delta-roa (cmp (roa 0) (roa 1) >)
     :accrual (cmp (at cfo 0) (at ni 0) >)
     :delta-leverage (cmp (ratio (at ltd 0) (at ta 0)) (ratio (at ltd 1) (at ta 1)) <)
     :delta-liquidity (cmp (ratio (at ca 0) (at cl 0)) (ratio (at ca 1) (at cl 1)) >)
     :no-dilution (cmp (at sh 0) (at sh 1) <=)
     :delta-margin (cmp (ratio (at gp 0) (at rev 0)) (ratio (at gp 1) (at rev 1)) >)
     :delta-turnover (cmp (ratio (at rev 0) (at ta 1)) (ratio (at rev 1) (at ta 2)) >)}))

(defn piotroski-f-score
  "The F-score (0 to 9): the number of piotroski-signals that hold. nil
   unless all nine can be computed, which needs three fiscal years of total
   assets."
  [permno]
  (let [signals (vals (piotroski-signals permno))]
    (when (every? some? signals)
      (count (filter true? signals)))))
