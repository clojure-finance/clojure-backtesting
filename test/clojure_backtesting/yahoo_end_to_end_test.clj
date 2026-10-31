(ns clojure-backtesting.yahoo-end-to-end-test
  "Builds the synthetic Yahoo snapshots in resources/sample-yahoo, loads
   them and runs strategies on prices and point-in-time fundamentals.
   Expected values are read from the snapshots, not hard-coded."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.fundamentals :refer :all]
            [clojure-backtesting.strategy :refer :all]
            [clojure-backtesting.yahoo :as yahoo]
            [clojure-backtesting.yahoo.build :as build]))

(def ^:private raw-dir "resources/sample-yahoo/raw")

(defn- close?
  ([a b] (close? a b 1e-9))
  ([a b tol] (<= (Math/abs (- (double a) (double b))) (* tol (max 1.0 (Math/abs (double b)))))))

(def ^:private built-dir (atom nil))

(defn- load-built! []
  (load-dataset (str (io/file (deref built-dir) "main")) "main")
  (load-fundamentals (io/file (deref built-dir) "fundamentals.edn")))

(defn- with-yahoo-dataset [f]
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "clojure-backtesting-yahoo-" (System/nanoTime)))]
    (alter-var-root #'clojure-backtesting.data/headers2 (constantly nil))
    (alter-var-root #'clojure-backtesting.data/data-files2 (constantly {}))
    (build/build-dataset! raw-dir dir)
    (reset! built-dir dir)
    (load-built!)
    (try
      (f)
      (finally
        (when-not (deref TERMINATED) (end-order))
        (clear-fundamentals!)
        (doseq [x (reverse (file-seq dir))] (io/delete-file x true))))))

(use-fixtures :once with-yahoo-dataset)

(defn- snapshot [ticker n]
  (let [files (sort (.listFiles (io/file raw-dir ticker)))]
    (build/read-edn-file (nth files n))))

(defn- statement-value [snap field frequency period-end]
  (some #(when (and (= field (:field %)) (= frequency (:frequency %)) (= period-end (:period-end %))) (:value %))
        (:statements snap)))

(defn- quantity [permno] (get-in (deref portfolio) [permno :quantity]))
(defn- cash [] (get-in (deref portfolio) [:cash :tot-val]))
(defn- step-to! [date] (while (neg? (compare (get-date) date)) (next-date)))

(deftest prices-load-as-traded
  (is (some #{:APRC} headers) "the dataset arrives furnished, no add-aprc needed")
  (is (= "2022-01-03" (first (first data-files))))
  (is (= "2025-01-01" (first (last data-files))) "the download day is left out")
  (is (nil? (get-permno-info "2022-01-03" "ZZZZ")) "a ticker Yahoo did not find has no rows")
  (is (nil? (get-permno-info "2023-07-03" "DLTA")) "a bar without a close has no row")
  (let [before (get-permno-info "2024-07-31" "ALFA")
        after (get-permno-info "2024-08-01" "ALFA")]
    (is (= 4.0 (:CFACPR before)))
    (is (= 1.0 (:CFACPR after)))
    (is (close? 0.25 (/ (:PRC after) (:PRC before)) 0.05) "the quoted price quarters on the split")
    (is (close? (+ 1.0 (:RET after)) (/ (:APRC after) (:APRC before)) 1e-9))
    (is (< (Math/abs (double (:RET after))) 0.1) "the split is not a loss")))

(deftest a-holding-follows-the-split-and-the-dividend
  (update-reinvest-dividends false)
  (try
    (init-portfolio "2024-07-29" 100000)
    (order "ALFA" 100)
    (next-date)
    (is (close? 100.0 (quantity "ALFA")))
    (step-to! "2024-08-01")
    (is (close? 400.0 (quantity "ALFA")) "4-for-1: the share count follows CFACPR")
    (let [cash-before (cash)]
      (step-to! "2024-08-15")
      (is (close? (+ cash-before (* 400 (get-permno-by-key "ALFA" :DIVAMT))) (cash) 1e-9)
          "the dividend is paid on the ex-date, per post-split share")
      (is (close? 0.10 (get-permno-by-key "ALFA" :DIVAMT) 1e-6)))
    (end-order)
    (finally
      (update-reinvest-dividends true))))

(deftest fundamentals-become-public-on-their-report-date
  ;; ALFA reports Q2 2024 on 2024-07-30 after the close
  (is (= "2024-03-31" (:period-end (fundamental-info-as-of "2024-07-30" "ALFA" :net-income :quarterly))))
  (let [i (fundamental-info-as-of "2024-07-31" "ALFA" :net-income :quarterly)]
    (is (= "2024-06-30" (:period-end i)))
    (is (= "2024-07-30" (:report-date i)))
    (is (= :earnings-history (:report-date-source i)))
    (is (= (statement-value (snapshot "ALFA" 1) :net-income :quarterly "2024-06-30") (:value i))))
  (testing "before the first report there is nothing"
    (is (nil? (fundamental-info-as-of "2020-01-02" "ALFA" :net-income :annual)))))

(deftest per-share-values-follow-the-share-basis
  (let [final (statement-value (snapshot "ALFA" 1) :diluted-eps :quarterly "2024-06-30")
        shares (statement-value (snapshot "ALFA" 1) :ordinary-shares-number :quarterly "2024-06-30")
        pre (fundamental-info-as-of "2024-07-31" "ALFA" :diluted-eps :quarterly)
        post (fundamental-info-as-of "2024-08-01" "ALFA" :diluted-eps :quarterly)]
    (is (= "2024-06-30" (:period-end pre) (:period-end post)))
    (is (close? (* 4 final) (:value pre)) "before the split EPS is per pre-split share")
    (is (close? final (:value post)))
    (is (close? (/ shares 4) (fundamental-as-of "2024-07-31" "ALFA" :ordinary-shares-number)))
    (is (close? shares (fundamental-as-of "2024-08-01" "ALFA" :ordinary-shares-number)))))

(deftest restated-values-keep-their-first-version
  (let [early (statement-value (snapshot "ALFA" 0) :net-income :quarterly "2023-12-31")
        late (statement-value (snapshot "ALFA" 1) :net-income :quarterly "2023-12-31")]
    (is (close? 1.02 (/ late early)) "the fixture restates this quarter")
    (is (= early (:value (fundamental-info-as-of "2024-03-01" "ALFA" :net-income :quarterly))))))

(deftest foreign-currency-fundamentals-convert-at-the-day-rate
  (let [date "2024-06-03"
        i (fundamental-info-as-of date "GAMA" :total-revenue :quarterly)
        fx (->> (:bars (:chart (build/read-edn-file (first (.listFiles (io/file raw-dir "_fx" "EURUSD=X"))))))
                (map (fn [{:keys [timestamp close]}] [(build/epoch->date timestamp java.time.ZoneOffset/UTC) close]))
                (filter #(neg? (compare (first %) date))) ; the rate known that morning
                last
                second)]
    (is (= "EUR" (:reported-currency i)))
    (is (= "USD" (:currency i)))
    (is (close? (* fx (:reported-value i)) (:value i)))
    (is (= (:reported-value i) (statement-value (snapshot "GAMA" 0) :total-revenue :quarterly (:period-end i))))))

(deftest report-dates-fall-back-to-the-reporting-lag
  (let [lagged (fundamental-info-as-of "2024-12-02" "BETA" :net-income :quarterly)
        announced (fundamental-info-as-of "2023-02-21" "BETA" :net-income :annual)]
    (is (= "2024-09-30" (:period-end lagged)))
    (is (= :lag (:report-date-source lagged)) "BETA's announcement dates stop in May 2023")
    (is (= "2024-11-14" (:public-date lagged)))
    (is (= "2022-12-31" (:period-end announced)))
    (is (= :announcement (:report-date-source announced)))
    (is (= "2023-02-18" (:report-date announced)))
    (is (= "2023-02-20" (:public-date announced))
        "without a time the report counts from the next day, a Sunday, so from Monday")
    (is (nil? (fundamental-info-as-of "2023-05-08" "BETA" :net-income :quarterly))
        "Yahoo keeps five quarters, so early 2023 has no quarterly figures")))

(deftest twelve-month-values
  (testing "Yahoo's trailing figure when it is the latest"
    (is (= :trailing (:source (fundamental-info-as-of "2024-11-01" "ALFA" :net-income nil)))))
  (testing "else the sum of four consecutive quarters"
    (let [i (fundamental-info-as-of "2024-11-01" "BETA" :net-income nil)
          snap (snapshot "BETA" 0)]
      (is (= :sum-of-quarters (:source i)))
      (is (= "2024-06-30" (:period-end i)))
      (is (close? (reduce + (map #(statement-value snap :net-income :quarterly %)
                                 ["2023-09-30" "2023-12-31" "2024-03-31" "2024-06-30"]))
                  (:value i)))))
  (testing "else the fiscal year"
    (is (= :annual (:source (fundamental-info-as-of "2023-03-01" "ALFA" :net-income :ttm)))))
  (testing "share counts are not summed over quarters"
    (let [i (fundamental-info-as-of "2024-11-01" "ALFA" :diluted-average-shares nil)]
      (is (= :quarterly (:source i)))
      (is (close? (statement-value (snapshot "ALFA" 1) :diluted-average-shares :quarterly "2024-09-30") (:value i)))))
  (testing "balance-sheet fields take the latest quarter or year"
    (is (= :quarterly (:frequency (fundamental-info-as-of "2024-11-01" "ALFA" :total-assets nil))))
    (is (nil? (fundamental-info-as-of "2024-11-01" "ALFA" :total-assets :ttm)) "no twelve-month sum of a stock")))

(deftest old-values-expire
  (let [age FUNDAMENTALS-MAX-AGE-MONTHS]
    (try
      (update-fundamentals-max-age 1)
      (is (nil? (fundamental-info-as-of "2024-12-02" "ALFA" :net-income :quarterly)))
      (finally
        (update-fundamentals-max-age age))))
  (is (some? (fundamental-info-as-of "2024-12-02" "ALFA" :net-income :quarterly))))

(deftest valuation-ratios
  (init-portfolio "2024-11-01" 100000)
  (let [price (get-permno-price "ALFA")
        eps (fundamental "ALFA" :diluted-eps)
        shares (shares-outstanding "ALFA")
        equity (fundamental "ALFA" :common-stock-equity)]
    (is (close? (/ price eps) (pe-ratio "ALFA")))
    (is (close? (/ eps price) (earnings-yield "ALFA")))
    (is (close? (* price shares) (market-cap "ALFA")))
    (is (close? (/ equity (* price shares)) (book-to-market "ALFA")))
    (is (close? (/ 0.40 price) (dividend-yield "ALFA") 1e-6) "four dividends of 0.10 in the last year"))
  (is (nil? (pe-ratio "DLTA")) "no fundamentals, no ratio")
  (is (= 0.0 (dividend-yield "DLTA")))
  (is (<= 0 (piotroski-f-score "ALFA") 9))
  (is (= 9 (count (piotroski-signals "GAMA"))))
  (is (some? (growth "ALFA" :total-revenue)))
  (is (close? 0.10 (growth "ALFA" :total-revenue) 1e-6) "the fixture grows revenue 10% a year")
  (testing "the dividend yield is the same before the split, per pre-split share"
    (init-portfolio "2024-06-03" 100000)
    (is (close? (/ 1.60 (get-permno-price "ALFA")) (dividend-yield "ALFA") 1e-6)))
  (testing "a loss has no P/E but an earnings yield"
    (init-portfolio "2022-06-01" 100000)
    (is (nil? (pe-ratio "BETA")))
    (is (neg? (earnings-yield "BETA")))))

(deftest equal-ratios-neither-rise-nor-fall
  ;; GAMA's ratios are the same every year and its values are converted
  ;; from euros, so without a tolerance the exchange rate decided its score
  (let [scores (for [d ["2024-06-03" "2024-06-04" "2024-08-01" "2024-09-02" "2024-11-15"]]
                 (do (init-portfolio d 100000) (piotroski-f-score "GAMA")))]
    (is (= [4] (distinct scores)) "the same score on every date"))
  (let [signals (piotroski-signals "GAMA")]
    (is (false? (:delta-margin signals)))
    (is (false? (:delta-turnover signals)))
    (is (true? (:no-dilution signals)) "an unchanged share count is no dilution"))
  (let [above? #'clojure-backtesting.fundamentals/above?]
    (is (not (above? 0.5200000000000001 0.52)))
    (is (above? 0.5201 0.52))
    (is (above? -0.1 -0.2))
    (is (not (above? -0.2 -0.2)))))

(deftest earnings-announcements
  (init-portfolio "2024-07-31" 100000)
  (let [e (latest-earnings "ALFA")]
    (is (= "2024-07-30" (:report-date e)))
    (is (= "2024-06-30" (:period-end e)))
    (is (number? (:surprise-pct e)))
    (is (= 0 (days-since-earnings "ALFA")) "public from the day after the after-close report")
    (is (nil? (latest-earnings "NOPE")) "an unknown security has none")
    (is (nil? (days-since-earnings "NOPE")))))

(deftest a-monthly-value-strategy
  (init-portfolio "2024-01-02" 100000)
  (let [universe ["ALFA" "BETA" "GAMA"]
        rebalances (atom [])]
    (while (neg? (compare (get-date) "2024-12-02"))
      (when (new-period? :month)
        (let [picks (top-n earnings-yield 2 :universe universe)]
          (swap! rebalances conj [(get-date) picks])
          (rebalance (equal-weights picks))))
      (next-date))
    (is (= 11 (count (deref rebalances))) "the run's first day, then the first trading day of each month")
    (is (= "2024-01-02" (first (first (deref rebalances)))))
    (is (every? #(= "01" (subs (first %) 8 10)) (take 3 (rest (deref rebalances)))))
    (let [[_ picks] (last (deref rebalances))
          held (set (remove #{:cash} (keys (deref portfolio))))]
      (is (= (set picks) held) "the portfolio holds the last picks")
      (doseq [p picks]
        (is (close? 0.5 (/ (get-in (deref portfolio) [p :tot-val]) (:tot-value (peek (deref portfolio-value)))) 0.05))))
    (is (= (mapv first (rank-by earnings-yield :universe universe))
           (vec (reverse (mapv first (rank-by earnings-yield :universe universe :ascending true))))))
    (end-order)))

(deftest benchmark-comparison
  (init-portfolio "2023-01-03" 100000)
  (order-target-percent "IDX" 1.0)
  (dotimes [_ 120] (next-date))
  (let [stats (benchmark-stats "IDX")]
    (is (= 120 (:days stats)))
    (is (close? 1.0 (:correlation stats) 0.05) "holding the index tracks it")
    (is (close? 1.0 (:beta stats) 0.1))
    (is (< (:tracking-error stats) 0.05)))
  (end-order))

(deftest an-implausible-share-count-is-not-used
  ;; Yahoo counts some multi-class companies in another class than the one
  ;; quoted; a count far from net income over EPS is refused
  (init-portfolio "2024-11-01" 100000)
  (let [shares (shares-outstanding "ALFA")
        original (deref fundamentals-store)]
    (is (some? shares))
    (try
      (swap! fundamentals-store update-in ["ALFA" :fields :ordinary-shares-number :values]
             (fn [vs] (into {} (map (fn [[f xs]] [f (mapv (fn [[pe v]] [pe (/ v 1500)]) xs)])) vs)))
      (is (re-find #"implies" (with-out-str (is (nil? (shares-outstanding "ALFA"))))) "and says why")
      (is (nil? (market-cap "ALFA")))
      (is (nil? (book-to-market "ALFA")))
      (finally
        (reset! fundamentals-store original)))
    (is (= shares (shares-outstanding "ALFA")))))

(deftest yahoo-dataset-builds-and-loads-into-out-dir
  ;; the download is stubbed: the snapshots are copied into place instead
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "clojure-backtesting-yahoo-all-" (System/nanoTime)))
        out (io/file dir "built")]
    (try
      (doseq [f (file-seq (io/file raw-dir)) :when (.isFile ^java.io.File f)]
        (let [target (io/file dir "raw" (subs (str f) (inc (count raw-dir))))]
          (io/make-parents target)
          (io/copy f target)))
      (with-redefs [yahoo/download-yahoo! (fn [& _] {:downloaded [] :skipped [] :failed {} :fx []})]
        (with-out-str (yahoo/yahoo-dataset! (str dir) ["ALFA"] :out-dir (str out))))
      (is (.exists (io/file out "fundamentals.edn")))
      (is (= "2025-01-01" (first (last data-files))) "the built dataset is the one loaded")
      (is (some? (fundamental-info-as-of "2024-07-31" "ALFA" :net-income :quarterly)))
      (finally
        (doseq [x (reverse (file-seq dir))] (io/delete-file x true))
        (load-built!)))))

(deftest a-rerun-retries-failed-downloads
  ;; the download is stubbed: "BAD" fails on the first run only
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "clojure-backtesting-yahoo-rerun-" (System/nanoTime)))
        runs (atom 0)
        fetched (atom [])
        fetch (fn [ticker _]
                (swap! fetched conj ticker)
                {:format 1 :ticker ticker
                 :fetched-at (str (.truncatedTo (java.time.Instant/now) java.time.temporal.ChronoUnit/SECONDS))
                 :chart {:meta {} :bars [] :events {}}
                 :errors (if (and (= ticker "BAD") (= 1 @runs)) {:chart {:message "rate limited"}} {})})
        download! #(do (swap! runs inc)
                       (with-out-str (yahoo/download-yahoo! (str dir) ["GOOD" "BAD"] :fundamentals? false :pause-ms 0)))]
    (try
      (with-redefs [yahoo/fetch-snapshot fetch]
        (download!)
        (is (= #{"GOOD" "BAD"} (set @fetched)))
        (reset! fetched [])
        (download!)
        (is (= ["BAD"] @fetched) "only the failure is downloaded again")
        (reset! fetched [])
        (download!)
        (is (empty? @fetched) "both are done for today"))
      (testing "a snapshot cut off while being written counts as not downloaded"
        (let [f (first (.listFiles (io/file dir "raw" "GOOD")))]
          (spit f "{:format 1 :ticker")
          (is (not (#'clojure-backtesting.yahoo/downloaded-today? (.getParentFile f))))))
      (finally
        (doseq [x (reverse (file-seq dir))] (io/delete-file x true))))))
