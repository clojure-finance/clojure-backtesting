(ns clojure-backtesting.yahoo.build
  "Turns raw Yahoo Finance snapshots, as `clojure-backtesting.yahoo` saves
   them, into datasets the backtester loads. Nothing here touches the
   network, so a dataset can be rebuilt with other options without
   downloading again.

   Prices become a main dataset in the CRSP layout. Yahoo quotes every
   price, volume and dividend in today's share basis, i.e. divided by all
   later splits; the rows here are as traded on the day (PRC, OPENPRC,
   BIDLO, ASKHI, VOL, DIVAMT), with CFACPR = CFACSHR = the product of the
   later splits, RET the total return (CRSP's definition, dividends at the
   ex-date) and RETX the return without dividends. The adjusted price
   columns add-aprc would add (INIT-PRICE, APRC, CUM-RET) are computed here
   as well, so loading the dataset needs no rewrite.

   Fundamentals become a point-in-time store: every statement value keeps
   its period end and the date it became public, taken from Yahoo's
   earnings report dates where they exist and from a reporting lag
   otherwise (recorded as such). Values are kept in the latest snapshot's
   share basis and currency as reported."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure-backtesting.data :as data])
  (:import [java.math BigDecimal MathContext]
           [java.time Instant LocalDate LocalDateTime ZoneId ZoneOffset]
           [java.time.temporal ChronoUnit]))

;; ============ Dates and time zones ============

(defn- zone-named [name]
  (try (ZoneId/of name) (catch Exception _ nil)))

(defn exchange-zone
  "The exchange's time zone from a chart meta map: its IANA name, else its
   UTC offset, else UTC."
  ^ZoneId [meta]
  (or (some-> (:exchange-timezone-name meta) zone-named)
      (some-> (:gmt-offset meta) int ZoneOffset/ofTotalSeconds)
      ZoneOffset/UTC))

(defn- local-date-time ^LocalDateTime [epoch ^ZoneId zone]
  (LocalDateTime/ofInstant (Instant/ofEpochSecond (long epoch)) zone))

(defn epoch->date
  "The yyyy-MM-dd date in `zone` of an epoch-seconds timestamp."
  [epoch zone]
  (str (.toLocalDate (local-date-time epoch zone))))

(defn plus-days [date n]
  (str (.plusDays (LocalDate/parse date) (long n))))

(defn days-between
  "Days from `from` to `to`, both yyyy-MM-dd."
  [from to]
  (.between ChronoUnit/DAYS (LocalDate/parse from) (LocalDate/parse to)))

(defn instant->date
  "The UTC yyyy-MM-dd date of an ISO-8601 instant string such as a
   snapshot's :fetched-at."
  [s]
  (subs (str (Instant/parse s)) 0 10))

;; ============ Splits and dividends ============

(defn- event-seq
  "Yahoo keys events by a keywordized timestamp; snapshots store the values."
  [events]
  (if (map? events) (vals events) events))

(defn split-events
  "Splits as [{:date :ratio}], sorted, where :ratio is the number of new
   shares per old share (4.0 for a 4-for-1 split, 0.1 for 1-for-10)."
  [events zone]
  (->> (event-seq (:splits events))
       (keep (fn [{:keys [date numerator denominator]}]
               (when (and date numerator denominator (pos? numerator) (pos? denominator))
                 {:date (epoch->date date zone) :ratio (/ (double numerator) (double denominator))})))
       (sort-by :date)
       vec))

(defn dividend-events
  "Cash dividends as [{:date :amount}], sorted by ex-date, amounts in
   today's share basis as Yahoo gives them."
  [events zone]
  (->> (event-seq (:dividends events))
       (keep (fn [{:keys [date amount]}]
               (when (and date amount (pos? amount))
                 {:date (epoch->date date zone) :amount (double amount)})))
       (sort-by :date)
       vec))

(defn split-factor-after
  "Product of the ratios of the `splits` dated after `date`: how many of
   today's shares one share held on `date` has become. CRSP calls this
   factor CFACPR."
  [splits date]
  (transduce (comp (filter #(pos? (compare (:date %) date))) (map :ratio)) * 1.0 splits))

;; ============ Prices ============

(def main-columns
  ["PERMNO" "date" "TICKER" "PRC" "OPENPRC" "BIDLO" "ASKHI" "VOL" "RET" "RETX"
   "DIVAMT" "CFACPR" "CFACSHR" "CURRENCY" "INIT-PRICE" "APRC" "CUM-RET"])

(defn- round-sig
  "Rounds to 7 significant digits, which removes the single-precision noise
   in Yahoo's quotes once they are scaled by a split factor."
  [x]
  (when x
    (.doubleValue (.round (BigDecimal. (double x)) (MathContext. 7)))))

(defn daily-bars
  "Yahoo's daily bars as maps with a :date in the exchange's time zone, one
   per date (the last wins), sorted, without bars lacking a positive close
   and without bars dated on or after `before-date` (the download day,
   whose session may not have finished)."
  [bars zone before-date]
  (->> bars
       (keep (fn [{:keys [timestamp close] :as bar}]
               (when (and timestamp close (pos? close))
                 (assoc bar :date (epoch->date timestamp zone)))))
       (remove #(and before-date (not (neg? (compare (:date %) before-date)))))
       (reduce (fn [m bar] (assoc m (:date bar) bar)) (sorted-map))
       vals
       vec))

(defn- dividends-by-bar
  "{bar-date amount}: each dividend is credited on the first bar dated on or
   after its ex-date. Dividends before the first or after the last bar are
   dropped."
  [dividends dates]
  (let [dates (apply sorted-set dates)]
    (reduce (fn [m {:keys [date amount]}]
              (if-let [bar-date (and (seq dates)
                                     (not (neg? (compare date (first dates))))
                                     (first (subseq dates >= date)))]
                (update m bar-date (fnil + 0.0) amount)
                m))
            {}
            dividends)))

(defn price-rows
  "The main-dataset rows (in `main-columns` order) for one security from
   its snapshot's :chart, oldest first. `id` goes in PERMNO and `ticker` in
   TICKER. Options: :before-date drops bars on or after it."
  [id ticker {:keys [meta bars events]} {:keys [before-date]}]
  (let [zone (exchange-zone meta)
        splits (split-events events zone)
        bars (daily-bars bars zone before-date)
        divs (dividends-by-bar (dividend-events events zone) (map :date bars))
        currency (:currency meta)]
    ;; RET and RETX come from the rounded as-traded values, so that they
    ;; agree exactly with PRC, CFACPR and DIVAMT
    (loop [bars bars prev-close nil init-price nil cum-ret 0.0 rows []]
      (if-let [{:keys [date open high low close volume]} (first bars)]
        (let [f (split-factor-after splits date)
              as-traded #(some-> % double (* f) round-sig)
              prc (as-traded close)
              divamt (let [d (get divs date 0.0)] (if (pos? d) (round-sig (* d f)) 0.0))
              close-now (/ prc f) ; today's share basis
              ret (when prev-close (- (/ (+ close-now (/ divamt f)) prev-close) 1.0))
              retx (when prev-close (- (/ close-now prev-close) 1.0))
              init-price (or init-price prc)
              cum-ret (if ret (+ cum-ret (Math/log (+ 1.0 ret))) cum-ret)]
          (recur (rest bars) close-now init-price cum-ret
                 (conj rows [id date ticker prc (as-traded open) (as-traded low) (as-traded high)
                             (some-> volume double (/ f) Math/round) ret retx
                             divamt f f currency
                             init-price (* init-price (Math/exp cum-ret)) cum-ret])))
        rows))))

;; ============ Currencies ============

(def ^:private minor-units
  {"GBp" ["GBP" 0.01] "GBX" ["GBP" 0.01] "ZAc" ["ZAR" 0.01] "ZAC" ["ZAR" 0.01] "ILA" ["ILS" 0.01]})

(defn major-currency
  "[currency factor] such that a price quoted in `currency` times factor is
   in the major unit: GBp (pence) is [\"GBP\" 0.01]."
  [currency]
  (get minor-units currency [currency 1.0]))

(defn fx-ticker
  "Yahoo's ticker for the rate that converts `from` into `to`: \"EURUSD=X\"
   quotes US dollars per euro."
  [from to]
  (str from to "=X"))

(defn fx-series
  "[[date rate] ...] from an FX snapshot's chart, sorted by date."
  [{:keys [meta bars]}]
  (let [zone (exchange-zone meta)]
    (mapv (juxt :date :close) (daily-bars bars zone nil))))

;; ============ Fundamentals ============

(def ^:private per-share-fields
  #{"BasicContinuousOperations" "DilutedContinuousOperations"
    "BasicDiscontinuousOperations" "DilutedDiscontinuousOperations"
    "BasicExtraordinary" "DilutedExtraordinary"
    "BasicAccountingChange" "DilutedAccountingChange"})

(defn field-kind
  "How a Yahoo statement field scales with the share basis: :per-share
   (EPS and the like, divided by a split), :shares (share counts,
   multiplied by it), :ratio (a rate, neither scaled nor converted between
   currencies) or :amount (currency amounts)."
  [yahoo-field]
  (let [f (str yahoo-field)]
    (cond
      (= f "TaxRateForCalcs") :ratio
      (or (str/includes? f "EPS") (str/includes? f "PerShare") (per-share-fields f)) :per-share
      (or (re-find #"Shares(Number)?$" f) (= f "ShareIssued")) :shares
      :else :amount)))

(defn rebase
  "Converts `value` of `kind` from the share basis of a snapshot to one
   `factor` times as many shares per share."
  [kind value factor]
  (case kind
    :per-share (/ value factor)
    :shares (* value factor)
    value))

(def ^:private close-hours
  "The local hour (rounded down) of the regular close of exchanges that
   close before 16:00. An announcement from this hour on counts from the
   next day."
  {"Asia/Tokyo" 15 "Asia/Shanghai" 15 "Asia/Seoul" 15 "Asia/Taipei" 13 "Asia/Kolkata" 15
   "Asia/Calcutta" 15 "Asia/Karachi" 15 "Asia/Manila" 15})

(defn close-hour
  "The hour from which an announcement in `zone` counts from the next day:
   `override` when given, else the exchange's close, 16:00 by default."
  [zone override]
  (or override (get close-hours (str zone) 16)))

(def default-report-lag-days
  "Days after the period end assumed for a report whose date Yahoo does not
   give, roughly the SEC deadlines for 10-Q and 10-K filings. A quarter or
   half-year that ends a fiscal year takes the annual lag."
  {:quarterly 45 :trailing 45 :annual 90})

(def ^:private match-window-days
  "How long after a period end an announcement may come to count as its
   report: long enough for annual results (Hong Kong allows three months),
   short enough not to take the next quarter's announcement for a missing
   one."
  {:quarterly 100 :trailing 100 :annual 100})

(defn- midnight-utc? [epoch]
  (zero? (mod (long epoch) 86400)))

(defn- announcement
  "{:date :public-date} for an announcement at `epoch` (time known) or on
   `date` (time unknown; an epoch at midnight, UTC or local, carries a date
   only). Before `after-close-hour` local time it can be acted on the same
   day, otherwise from the next day; with an unknown time the next day is
   assumed."
  [epoch date timing zone after-close-hour]
  (let [t (when epoch (local-date-time epoch zone))]
    (if (and t (not (midnight-utc? epoch)) (not= java.time.LocalTime/MIDNIGHT (.toLocalTime t)))
      (let [d (str (.toLocalDate t))
            ;; nil timing (Yahoo's earnings history) and "TAS" carry the real
            ;; time; any other code, such as "TNS" (time not supplied), does not
            same-day? (case timing
                        "BMO" true
                        (nil "TAS") (< (.getHour t) after-close-hour)
                        false)]
        {:date d :public-date (if same-day? d (plus-days d 1))})
      (when-let [date (or date
                          (when t
                            (if (midnight-utc? epoch)
                              (epoch->date epoch ZoneOffset/UTC)
                              (str (.toLocalDate t)))))]
        {:date date :public-date (if (= timing "BMO") date (plus-days date 1))}))))

(defn- earnings-announcement?
  "Whether a row of Yahoo's earnings dates marks an earnings report: an
   unnamed row with an actual EPS (a scheduled one has only an estimate),
   or a named earnings event such as \"Q2 2025 Earnings Call\", which is
   held when the results are out. Other events, such as shareholder
   meetings, are not reports."
  [{:keys [event-name eps-actual]}]
  (if (str/blank? event-name)
    (some? eps-actual)
    (boolean (re-find #"(?i)earning" event-name))))

(defn- earnings-with-eps?
  "An earnings report row that carries an actual EPS."
  [row]
  (and (earnings-announcement? row) (some? (:eps-actual row))))

(defn report-candidates
  "The report dates known for a security, from Yahoo's earnings history
   (period end paired with its report date) and its earnings announcement
   dates (announcements only). Returns {:history [{:period-end :date
   :public-date}] :announcements [{:date :public-date}]}, each sorted."
  [{:keys [earnings-history earnings-dates]} zone after-close-hour]
  {:history (->> earnings-history
                 (keep (fn [{:keys [period-end reported-date reported-at]}]
                         (when period-end
                           (some-> (announcement reported-at reported-date nil zone after-close-hour)
                                   (assoc :period-end period-end)))))
                 (sort-by :period-end)
                 vec)
   :announcements (->> earnings-dates
                       (filter earnings-announcement?)
                       (keep (fn [{:keys [announced-epoch timing]}]
                               (announcement announced-epoch nil timing zone after-close-hour)))
                       distinct
                       (sort-by :date)
                       vec)})

(defn report-date
  "When the report for the period of `frequency` ending `period-end` was
   made public: {:report-date :public-date :source}. :source is
   :earnings-history when Yahoo pairs the period with its report date,
   :announcement when the first earnings announcement within a few months
   after the period end is taken as its report, and :lag when neither is
   known and `lag-days` after the period end is assumed (the annual lag for
   a period ending within ten days of one of `annual-ends`). Announcements after
   `as-of` (the download day) are ignored: they are only scheduled."
  [frequency period-end {:keys [history announcements]} {:keys [lag-days as-of annual-ends]}]
  (let [after-end? #(pos? (compare (:date %) period-end))
        known? #(or (nil? as-of) (not (pos? (compare (:date %) as-of))))
        exact (->> history
                   (filter #(<= (Math/abs (long (days-between (:period-end %) period-end))) 10))
                   (filter after-end?)
                   (sort-by #(Math/abs (long (days-between (:period-end %) period-end))))
                   first)
        window (get match-window-days frequency 100)
        matched (->> announcements
                     (filter after-end?)
                     (filter known?)
                     (filter #(<= (days-between period-end (:date %)) window))
                     first)]
    (cond
      exact {:report-date (:date exact) :public-date (:public-date exact) :source :earnings-history}
      matched {:report-date (:date matched) :public-date (:public-date matched) :source :announcement}
      :else (let [year-end? (some #(<= (Math/abs (long (days-between % period-end))) 10) annual-ends)
                  d (plus-days period-end (get lag-days (if year-end? :annual frequency) 45))]
              {:report-date d :public-date d :source :lag}))))

(defn- relative-diff [a b]
  (let [scale (max (Math/abs (double a)) (Math/abs (double b)))]
    (if (zero? scale) 0.0 (/ (Math/abs (- (double a) (double b))) scale))))

(defn statement-values
  "Statement values merged over a security's snapshots (oldest first), in
   the share basis of the last one. Returns {[field frequency period-end]
   {:value :currency :statement :yahoo-field :kind :revised?}}. With
   `vintage` :first a value restated in a later snapshot keeps its earliest
   version, the one closest to what was first reported; with :latest the
   newest wins. Yahoo's trailing per-share values and share counts for
   periods before a split are left out: unlike its quarterly and annual
   ones they are not consistently restated (NVDA's trailing EPS is quoted
   per pre-split share up to January 2024 and per post-split share from
   April 2024, both before its June 2024 split), so for those periods
   twelve-month per-share figures come from the quarters or the year."
  [snapshots splits vintage]
  (reduce
   (fn [acc {:keys [fetched-at statements]}]
     (let [factor (split-factor-after splits (instant->date fetched-at))]
       (reduce
        (fn [acc {:keys [field frequency period-end value currency statement yahoo-field]}]
          (if (and field frequency period-end (number? value)
                   (not (and (= frequency :trailing) (not= :amount (field-kind yahoo-field))
                             (not= 1.0 (split-factor-after splits period-end)))))
            (let [kind (field-kind yahoo-field)
                  v (rebase kind (double value) factor)
                  k [field frequency period-end]]
              (if-let [old (get acc k)]
                (let [revised? (or (:revised? old) (> (relative-diff (:value old) v) 1e-6))]
                  (assoc acc k (if (= vintage :latest)
                                 (assoc old :value v :currency currency :revised? revised?)
                                 (assoc old :revised? revised?))))
                (assoc acc k {:value v :currency currency :statement statement
                              :yahoo-field yahoo-field :kind kind :revised? false})))
            acc))
        acc
        statements)))
   {}
   snapshots))

(defn- latest-by [key-fn rows]
  (vals (reduce (fn [m r] (assoc m (key-fn r) r)) {} rows)))

(defn earnings-events
  "Earnings announcements with their EPS, as [{:report-date :public-date
   :period-end :eps-actual :eps-estimate :surprise-pct}] sorted by public
   date, from both Yahoo sources; :period-end is nil when only the
   announcement date is known. Yahoo's EPS here is the analysts' adjusted
   figure and its share basis is not consistent (some values are restated
   for later splits, some not), so compare :surprise-pct, not the EPS."
  [{:keys [earnings-history earnings-dates]} zone after-close-hour]
  (let [from-history (keep (fn [{:keys [period-end reported-date reported-at eps-actual eps-estimate surprise-pct]}]
                             (when-let [a (announcement reported-at reported-date nil zone after-close-hour)]
                               {:report-date (:date a) :public-date (:public-date a) :period-end period-end
                                :eps-actual eps-actual :eps-estimate eps-estimate :surprise-pct surprise-pct}))
                           earnings-history)
        history-dates (set (map :report-date from-history))
        from-dates (keep (fn [{:keys [announced-epoch timing eps-actual eps-estimate surprise-pct] :as row}]
                           (when (earnings-with-eps? row)
                             (when-let [a (announcement announced-epoch nil timing zone after-close-hour)]
                               (when-not (history-dates (:date a))
                                 {:report-date (:date a) :public-date (:public-date a) :period-end nil
                                  :eps-actual eps-actual :eps-estimate eps-estimate :surprise-pct surprise-pct}))))
                         earnings-dates)]
    (vec (sort-by :public-date (latest-by :report-date (concat from-dates from-history))))))

(defn- chart-snapshot
  "The latest of `snapshots` whose download returned prices."
  [snapshots]
  (last (filter #(seq (get-in % [:chart :bars])) snapshots)))

(defn fundamentals-entry
  "The point-in-time store entry for one security from its snapshots
   (oldest first). Options: :report-lag-days, :after-close-hour, :vintage.
   Returns {:currency :trading-currency :reports :fields :earnings
   :dividends :splits :summary}: :currency is the main reporting currency,
   :trading-currency the quote currency, :reports maps [frequency
   period-end] to {:report-date :public-date :source :currency}, and
   :fields maps each field to {:statement :kind :yahoo-field :values
   {frequency [[period-end value] ...]}} with values oldest first."
  [snapshots {:keys [report-lag-days after-close-hour vintage]
              :or {report-lag-days default-report-lag-days vintage :first}}]
  (let [latest (or (chart-snapshot snapshots) (last snapshots))
        meta (get-in latest [:chart :meta])
        zone (exchange-zone meta)
        after-close-hour (close-hour zone after-close-hour)
        splits (split-events (get-in latest [:chart :events]) zone)
        as-of (instant->date (:fetched-at (last snapshots)))
        trading-dates (apply sorted-set (map :date (daily-bars (get-in latest [:chart :bars]) zone nil)))
        ;; a report public on a weekend or holiday is acted on the next
        ;; trading day; dates outside the price history stay as they are
        roll (fn [d] (or (when (and (seq trading-dates) (not (neg? (compare d (first trading-dates)))))
                           (first (subseq trading-dates >= d)))
                         d))
        ;; a row dated after its snapshot's download was only scheduled
        ;; then; if it happened, a later snapshot has it
        earnings {:earnings-history (latest-by :period-end (mapcat :earnings-history snapshots))
                  :earnings-dates (latest-by :announced-epoch
                                             (mapcat (fn [{:keys [fetched-at earnings-dates]}]
                                                       (let [fetched (.getEpochSecond (Instant/parse fetched-at))]
                                                         (filter #(some-> (:announced-epoch %) (< fetched)) earnings-dates)))
                                                     snapshots))}
        candidates (report-candidates earnings zone after-close-hour)
        values (statement-values snapshots splits vintage)
        periods (distinct (map (fn [[_ frequency period-end]] [frequency period-end]) (keys values)))
        currency-of (reduce (fn [m [[_ freq pe] {:keys [currency]}]] (assoc m [freq pe] currency)) {} values)
        annual-ends (keep (fn [[freq pe]] (when (= freq :annual) pe)) periods)
        reports (into {} (map (fn [[freq pe :as k]]
                                [k (-> (report-date freq pe candidates {:lag-days report-lag-days :as-of as-of
                                                                        :annual-ends annual-ends})
                                       (update :public-date roll)
                                       (assoc :currency (currency-of k)))])
                              periods))
        fields (reduce (fn [m [[field freq pe] {:keys [value statement kind yahoo-field]}]]
                         (-> m
                             (update field merge {:statement statement :kind kind :yahoo-field yahoo-field})
                             (update-in [field :values freq] (fnil conj []) [pe value])))
                       {}
                       values)
        fields (into {} (map (fn [[f e]] [f (update e :values #(into {} (map (fn [[freq vs]] [freq (vec (sort-by first vs))]) %)))])
                             fields))
        currencies (frequencies (keep :currency (vals values)))]
    {:currency (when (seq currencies) (key (apply max-key val currencies)))
     :trading-currency (:currency meta)
     :reports reports
     :fields fields
     :earnings (mapv #(update % :public-date roll) (earnings-events earnings zone after-close-hour))
     :dividends (mapv (juxt :date :amount) (dividend-events (get-in latest [:chart :events]) zone))
     :splits (mapv (juxt :date :ratio) splits)
     :summary {:periods (count reports)
               :values (count values)
               :restated (count (filter :revised? (vals values)))
               :report-sources (frequencies (map :source (vals reports)))}}))

;; ============ Reading snapshots ============

(defn encode-ticker
  "A file-system-safe directory name for a ticker."
  [ticker]
  (apply str (map (fn [c] (if (re-matches #"[A-Za-z0-9._=^-]" (str c)) c (format "%%%02X" (int c)))) ticker)))

(defn snapshot-file-name
  "20261031T100000Z.edn for a snapshot fetched at 2026-10-31T10:00:00Z."
  [fetched-at]
  (str (str/replace (str (.truncatedTo (Instant/parse fetched-at) ChronoUnit/SECONDS)) #"[-:]" "") ".edn"))

(def fx-dir-name "_fx")

(defn read-edn-file [f]
  (edn/read-string (slurp f)))

(defn write-edn-file! [f x]
  (io/make-parents f)
  (binding [*print-length* nil *print-level* nil]
    (spit f (pr-str x))))

(defn- snapshots-in [dir]
  (->> (.listFiles (io/file dir))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".edn"))
       (map read-edn-file)
       (sort-by :fetched-at)
       vec))

(defn read-snapshots
  "The snapshots under `raw-dir`, oldest first: {:securities {ticker
   [snapshot ...]} :fx {[from to] [snapshot ...]}}."
  [raw-dir]
  (let [dirs (filter #(.isDirectory ^java.io.File %) (.listFiles (io/file raw-dir)))
        {fx true securities false} (group-by #(= fx-dir-name (.getName ^java.io.File %)) dirs)]
    {:securities (into (sorted-map)
                       (keep (fn [d] (let [snaps (snapshots-in d)]
                                       (when (seq snaps) [(:ticker (last snaps)) snaps]))))
                       securities)
     :fx (into {}
               (keep (fn [d] (let [snaps (snapshots-in d)]
                               (when (seq snaps) [[(:from (last snaps)) (:to (last snaps))] snaps]))))
               (mapcat #(filter (fn [f] (.isDirectory ^java.io.File f)) (.listFiles ^java.io.File %)) fx))}))

;; ============ Building ============

(defn- delete-tree! [f]
  (doseq [x (reverse (file-seq (io/file f)))]
    (io/delete-file x true)))

(defn- within? [date start end]
  (and (or (nil? start) (not (neg? (compare date start))))
       (or (nil? end) (not (pos? (compare date end))))))

(defn build-dataset!
  "Builds a dataset under `out-dir` from the snapshots under `raw-dir`:
   main/ (prices, CRSP layout, furnished with APRC), fundamentals.edn (the
   point-in-time store and FX rates) and dataset.edn (provenance, coverage
   and warnings). Options:
     :start, :end       keep price rows within these yyyy-MM-dd dates
     :report-lag-days   {:quarterly 45 :annual 90 :trailing 45} by default
     :after-close-hour  local hour from which an announcement counts from
                        the next day (by default the exchange's close
                        hour, 16 for most)
     :vintage           :first (default) or :latest, for values Yahoo
                        restated between snapshots
     :allow-mixed-currencies?  build a main dataset whose securities are
                        quoted in different currencies, pence and pounds
                        included (the engine values them all as one
                        currency, so only for strategies that never hold
                        more than one of them)
   Returns the contents of dataset.edn."
  [raw-dir out-dir & {:keys [start end allow-mixed-currencies?] :as opts}]
  (let [{:keys [securities fx]} (read-snapshots raw-dir)
        usable (into (sorted-map)
                     (filter (fn [[_ snaps]] (chart-snapshot snaps)))
                     securities)
        warnings (vec (for [[ticker snaps] securities
                            :when (not (contains? usable ticker))]
                        {:type :no-prices :ticker ticker
                         :error (get-in (last snaps) [:errors :chart])}))
        currencies (into (sorted-map)
                         (map (fn [[t snaps]] [t (get-in (chart-snapshot snaps) [:chart :meta :currency])]))
                         usable)
        quoted-in (set (vals currencies))]
    (when (empty? usable)
      (throw (ex-info (str "No security under " raw-dir " has prices.")
                      {:type :no-data :raw-dir (str raw-dir) :warnings warnings})))
    (when (and (> (count quoted-in) 1) (not allow-mixed-currencies?))
      (throw (ex-info (str "The securities are quoted in different currencies " (sort quoted-in)
                           ", which the engine would add up as one. Build one dataset per currency,"
                           " or pass :allow-mixed-currencies? true.")
                      {:type :mixed-currencies :currencies currencies})))
    (let [rows (for [[ticker snaps] usable
                     :let [latest (chart-snapshot snaps)
                           before (instant->date (:fetched-at latest))]
                     row (price-rows ticker ticker (:chart latest) {:before-date before})
                     :when (within? (nth row 1) start end)]
                 row)
          rows-by-date (reduce (fn [m row] (update m (nth row 1) (fnil conj []) row)) (sorted-map) rows)
          _ (when (empty? rows-by-date)
              (throw (ex-info (str "No prices between " (or start "the start") " and " (or end "the end") ".")
                              {:type :no-data :start start :end end})))
          main-dir (io/file out-dir "main")
          store (into (sorted-map)
                      (map (fn [[ticker snaps]] [ticker (fundamentals-entry snaps opts)]))
                      usable)
          fx-table (into {} (keep (fn [[pair snaps]] (some->> (chart-snapshot snaps) :chart fx-series (vector pair)))) fx)
          needed-fx (set (for [[ticker entry] store
                               :let [[major _] (major-currency (currencies ticker))]
                               c (distinct (keep :currency (vals (:reports entry))))
                               :when (and c major (not= c major))]
                           [c major]))
          missing-fx (remove fx-table needed-fx)
          coverage (into (sorted-map)
                         (map (fn [[ticker dates]]
                                (let [entry (store ticker)]
                                  [ticker {:first-date (first dates) :last-date (last dates) :days (count dates)
                                           :currency (currencies ticker)
                                           :reporting-currency (:currency entry)
                                           :snapshots (count (usable ticker))
                                           :fundamentals (:summary entry)}])))
                         (reduce (fn [m row] (update m (first row) (fnil conj []) (nth row 1))) {} rows))
          info {:format 1
                :built-at (str (Instant/now))
                :raw-dir (str raw-dir)
                :options opts
                :date-range [(first (keys rows-by-date)) (last (keys rows-by-date))]
                :securities coverage
                :fx-pairs (vec (sort (keys fx-table)))
                :warnings (into warnings
                                (concat
                                 (for [pair missing-fx]
                                   {:type :missing-fx :pair pair
                                    :message "Fundamentals in this currency cannot be converted to the trading currency."})
                                 (for [[ticker {:keys [fundamentals]}] coverage
                                       :let [n (get-in fundamentals [:report-sources :lag] 0)]
                                       :when (pos? n)]
                                   {:type :estimated-report-dates :ticker ticker :periods n})))}]
      (delete-tree! main-dir)
      (data/write-grouped-dataset main-dir main-columns rows-by-date)
      (write-edn-file! (io/file out-dir "fundamentals.edn")
                       {:format 1 :store (into {} (map (fn [[t e]] [t (dissoc e :summary)])) store) :fx fx-table})
      (write-edn-file! (io/file out-dir "dataset.edn") info)
      info)))
