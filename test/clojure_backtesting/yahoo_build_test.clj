(ns clojure-backtesting.yahoo-build-test
  "Unit tests of the pure Yahoo conversion: as-traded prices from Yahoo's
   split-adjusted quotes, trading dates in the exchange's time zone, report
   dates, snapshot merging and the currency guard."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure-backtesting.yahoo.build :as build])
  (:import [java.time LocalDateTime ZoneOffset]))

(defn- close?
  ([a b] (close? a b 1e-9))
  ([a b tol] (< (Math/abs (- (double a) (double b))) tol)))

(defn- utc [s] (.toEpochSecond (LocalDateTime/parse s) ZoneOffset/UTC))

(def ^:private ny {:currency "USD" :exchange-timezone-name "America/New_York" :gmt-offset -14400})

(defn- bar [date close & {:keys [volume] :or {volume 1000}}]
  {:timestamp (utc (str date "T13:30:00")) :open close :high close :low close :close close :volume volume})

(defn- rows-by-date [chart & [opts]]
  (into {} (map (fn [r] [(nth r 1) (zipmap build/main-columns r)]))
        (build/price-rows "X" "X" chart (or opts {}))))

(deftest as-traded-prices-across-a-split
  ;; Yahoo's quotes on the download day: the 2-for-1 split of 2024-06-03 is
  ;; already divided out of the earlier bars, volumes multiplied in, and the
  ;; dividend of 2024-05-30 (1.00 as paid) quoted as 0.50.
  (let [chart {:meta ny
               :bars [(bar "2024-05-28" 50.0 :volume 2000) (bar "2024-05-29" 51.0 :volume 2000)
                      (bar "2024-05-30" 50.5 :volume 2000) (bar "2024-05-31" 52.0 :volume 2000)
                      (bar "2024-06-03" 53.0 :volume 1500) (bar "2024-06-04" 54.0 :volume 1500)]
               :events {:dividends [{:date (utc "2024-05-30T13:30:00") :amount 0.5}]
                        :splits [{:date (utc "2024-06-03T13:30:00") :numerator 2.0 :denominator 1.0}]}}
        rows (rows-by-date chart)]
    (testing "prices before the split are as traded, CFACPR is the later split"
      (is (= 100.0 (get-in rows ["2024-05-28" "PRC"])))
      (is (= 104.0 (get-in rows ["2024-05-31" "PRC"])))
      (is (= 53.0 (get-in rows ["2024-06-03" "PRC"])))
      (is (= 2.0 (get-in rows ["2024-05-31" "CFACPR"]) (get-in rows ["2024-05-31" "CFACSHR"])))
      (is (= 1.0 (get-in rows ["2024-06-03" "CFACPR"]))))
    (testing "volumes are in traded shares"
      (is (= 1000 (get-in rows ["2024-05-28" "VOL"])))
      (is (= 1500 (get-in rows ["2024-06-03" "VOL"]))))
    (testing "the dividend is paid on its ex-date, as paid per share then"
      (is (= 1.0 (get-in rows ["2024-05-30" "DIVAMT"])))
      (is (= 0.0 (get-in rows ["2024-05-29" "DIVAMT"]))))
    (testing "RET is the total return, RETX the price return, split or not"
      (is (nil? (get-in rows ["2024-05-28" "RET"])))
      (is (close? (- (/ (+ 50.5 0.5) 51.0) 1.0) (get-in rows ["2024-05-30" "RET"])))
      (is (close? (- (/ 50.5 51.0) 1.0) (get-in rows ["2024-05-30" "RETX"])))
      (is (close? (- (/ 53.0 52.0) 1.0) (get-in rows ["2024-06-03" "RET"])) "no -50% on the split day")
      (is (close? (- (* (get-in rows ["2024-06-03" "CFACPR"]) (get-in rows ["2024-06-03" "PRC"]) (/ (get-in rows ["2024-05-31" "CFACPR"]) (get-in rows ["2024-05-31" "PRC"]))) 1.0)
                  (get-in rows ["2024-06-03" "RETX"]))))
    (testing "the adjusted price chains the total return from the first close"
      (is (= 100.0 (get-in rows ["2024-05-28" "APRC"]) (get-in rows ["2024-05-28" "INIT-PRICE"])))
      (is (close? (+ 1.0 (get-in rows ["2024-06-04" "RET"]))
                  (/ (get-in rows ["2024-06-04" "APRC"]) (get-in rows ["2024-06-03" "APRC"]))))
      (is (close? (get-in rows ["2024-06-04" "CUM-RET"])
                  (Math/log (/ (get-in rows ["2024-06-04" "APRC"]) 100.0)))))))

(deftest bars-without-a-close-gaps-and-the-download-day
  (let [chart {:meta ny
               :bars [(bar "2024-03-01" 10.0) (assoc (bar "2024-03-04" 11.0) :close nil)
                      (bar "2024-03-05" 12.0) (bar "2024-03-06" 12.5) (bar "2024-03-06" 12.6)
                      (bar "2024-03-07" 13.0)]
               :events {:dividends [{:date (utc "2024-03-02T13:30:00") :amount 0.1}] :splits []}}
        rows (rows-by-date chart {:before-date "2024-03-07"})]
    (is (= ["2024-03-01" "2024-03-05" "2024-03-06"] (sort (keys rows)))
        "no row for a bar without a close, nor for the download day")
    (is (close? (- (/ (+ 12.0 0.1) 10.0) 1.0) (get-in rows ["2024-03-05" "RET"]))
        "the return spans the gap, with a weekend dividend credited on the next bar")
    (is (= 12.6 (get-in rows ["2024-03-06" "PRC"])) "of two bars on one date the last wins")))

(deftest trading-dates-are-in-the-exchange-time-zone
  ;; ASX opens at 10:00 Sydney time, 23:00 UTC the previous day in summer
  (let [chart {:meta {:currency "AUD" :exchange-timezone-name "Australia/Sydney" :gmt-offset 39600}
               :bars [{:timestamp (utc "2024-03-03T23:00:00") :open 1.0 :high 1.0 :low 1.0 :close 1.0 :volume 1}]
               :events {}}]
    (is (= ["2024-03-04"] (keys (rows-by-date chart)))))
  (is (= "2024-03-04" (build/epoch->date (utc "2024-03-03T23:00:00")
                                         (build/exchange-zone {:gmt-offset 39600})))
      "without a zone name the UTC offset is used"))

(deftest returns-agree-with-the-rounded-prices
  ;; Yahoo's single-precision quotes times a split factor: RET must match
  ;; the stored PRC, CFACPR and DIVAMT exactly, or holdings drift daily
  (let [chart {:meta ny
               :bars (map-indexed (fn [i d] (bar d (+ 106.46900177001953 (* i 0.123456789))))
                                  ["2024-06-03" "2024-06-04" "2024-06-05" "2024-06-06" "2024-06-07" "2024-06-10"])
               :events {:dividends [{:date (utc "2024-06-05T13:30:00") :amount 0.0123456}]
                        :splits [{:date (utc "2024-06-10T13:30:00") :numerator 10.0 :denominator 1.0}]}}
        rows (sort-by first (rows-by-date chart))]
    (doseq [[[_ prev] [d curr]] (partition 2 1 rows)]
      (let [basis #(/ (get % %2) (get % "CFACPR"))]
        (is (close? (+ 1.0 (curr "RET"))
                    (/ (+ (basis curr "PRC") (basis curr "DIVAMT")) (basis prev "PRC"))
                    1e-14)
            d)))))

(deftest field-kinds
  (is (= :per-share (build/field-kind "DilutedEPS")))
  (is (= :per-share (build/field-kind "DilutedContinuousOperations")))
  (is (= :shares (build/field-kind "OrdinarySharesNumber")))
  (is (= :shares (build/field-kind "DilutedAverageShares")))
  (is (= :shares (build/field-kind "ShareIssued")))
  (is (= :amount (build/field-kind "TotalRevenue")))
  (is (= :amount (build/field-kind "StockBasedCompensation")))
  (is (= :ratio (build/field-kind "TaxRateForCalcs"))))

(def ^:private zone (build/exchange-zone ny))

(defn- report-of [freq period-end earnings & [opts]]
  (build/report-date freq period-end (build/report-candidates earnings zone 16)
                     (merge {:lag-days build/default-report-lag-days :as-of "2025-01-01"} opts)))

(deftest report-dates
  (let [after-close (utc "2024-07-30T20:30:00") ; 16:30 New York
        history [{:period-end "2024-06-30" :reported-date "2024-07-30" :reported-at after-close}]
        announcements [{:announced-epoch (utc "2024-04-25T12:00:00") :timing "BMO" :eps-actual 1.0}
                       {:announced-epoch (utc "2024-04-02T13:00:00") :timing "TAS" :event-name " Annual Shareholders Meeting" :eps-estimate 1.0 :eps-actual 1.0}
                       {:announced-epoch (utc "2024-10-29T00:00:00") :timing "TNS" :eps-actual 1.1}
                       {:announced-epoch (utc "2025-01-28T21:00:00") :timing "TAS" :eps-estimate 1.2}
                       {:announced-epoch (utc "2024-01-05T21:00:00") :timing "TAS" :eps-estimate 1.0}]]
    (testing "Yahoo's earnings history pairs a quarter with its report; after the close it counts from the next day"
      (is (= {:report-date "2024-07-30" :public-date "2024-07-31" :source :earnings-history}
             (report-of :quarterly "2024-06-30" {:earnings-history history}))))
    (testing "otherwise the first earnings announcement after the period end is its report"
      (is (= {:report-date "2024-04-25" :public-date "2024-04-25" :source :announcement}
             (report-of :quarterly "2024-03-31" {:earnings-dates announcements}))
          "before the open it counts the same day; the shareholder meeting is not a report"))
    (testing "an announcement without a time counts from the next day"
      (is (= {:report-date "2024-10-29" :public-date "2024-10-30" :source :announcement}
             (report-of :quarterly "2024-09-30" {:earnings-dates announcements}))))
    (testing "a scheduled announcement after the download is not a report: the lag is assumed"
      (is (= {:report-date "2025-02-14" :public-date "2025-02-14" :source :lag}
             (report-of :quarterly "2024-12-31" {:earnings-dates announcements}))))
    (testing "the fiscal year shares the report of its last quarter; the annual lag is longer"
      (is (= :earnings-history (:source (report-of :annual "2024-06-30" {:earnings-history history}))))
      (is (= "2024-03-30" (:public-date (report-of :annual "2023-12-31" {}))))
      (is (= "2023-11-14" (:public-date (report-of :quarterly "2023-09-30" {})))))
    (testing "a report date at midnight UTC has no time: it counts from the next day"
      (is (= "2024-08-02" (:public-date (report-of :quarterly "2024-06-30"
                                                {:earnings-history [{:period-end "2024-06-30" :reported-date "2024-08-01"
                                                                     :reported-at (utc "2024-08-01T00:00:00")}]})))))
    (testing "a date only scheduled (no actual EPS) is not a report"
      (is (= :lag (:source (report-of :quarterly "2023-12-01" {:earnings-dates announcements})))))
    (testing "an earnings call is held once the results are out"
      (is (= {:report-date "2025-07-15" :public-date "2025-07-15" :source :announcement}
             (report-of :quarterly "2025-06-30" {:earnings-dates [{:announced-epoch (utc "2025-07-15T12:30:00") :timing "TAS"
                                                                   :event-name "Q2 2025  Earnings Call"}]}
                        {:as-of "2025-10-31"}))))
    (testing "a quarter that ends the fiscal year takes the annual lag"
      (is (= "2025-03-31" (:public-date (report-of :quarterly "2024-12-31" {} {:annual-ends ["2024-12-31"]}))))
      (is (= "2025-03-31" (:public-date (report-of :trailing "2024-12-31" {} {:annual-ends ["2024-12-31"]})))))
    (testing "a timestamp at local midnight carries a date only"
      ;; Yahoo's older rows: 04:00Z is midnight in New York in summer
      (is (= {:report-date "2014-07-22" :public-date "2014-07-23" :source :announcement}
             (report-of :quarterly "2014-06-30" {:earnings-dates [{:announced-epoch (utc "2014-07-22T04:00:00")
                                                                   :timing "TAS" :eps-actual 1.28}]}))))
    (testing "a Tokyo announcement after its 15:30 close counts from the next day"
      (let [tokyo (build/exchange-zone {:exchange-timezone-name "Asia/Tokyo"})]
        (is (= "2024-05-11"
               (:public-date (build/report-date :quarterly "2024-03-31"
                                                (build/report-candidates {:earnings-dates [{:announced-epoch (utc "2024-05-10T06:30:00")
                                                                                            :timing "TAS" :eps-actual 1.0}]}
                                                                         tokyo (build/close-hour tokyo nil))
                                                {:lag-days build/default-report-lag-days})))
            "15:30 in Tokyo")))
    (testing "an announcement too long after the period end is not its report"
      (is (= :lag (:source (report-of :quarterly "2023-12-31" {:earnings-dates announcements})))))))

(deftest snapshots-merge-in-the-latest-share-basis
  ;; The first snapshot predates a 4-for-1 split, so its EPS is four times
  ;; and its share count a quarter of the second's.
  (let [splits [{:date "2024-08-01" :ratio 4.0}]
        row (fn [field yahoo value pe] {:field field :yahoo-field yahoo :frequency :quarterly :period-end pe
                                        :value value :currency "USD" :statement :income-statement})
        early {:fetched-at "2024-06-01T00:00:00Z"
               :statements [(row :diluted-eps "DilutedEPS" 4.0 "2024-03-31")
                            (row :diluted-average-shares "DilutedAverageShares" 100.0 "2024-03-31")
                            (row :net-income "NetIncome" 400.0 "2024-03-31")]}
        late {:fetched-at "2025-01-02T00:00:00Z"
              :statements [(row :diluted-eps "DilutedEPS" 1.0 "2024-03-31")
                           (row :diluted-average-shares "DilutedAverageShares" 400.0 "2024-03-31")
                           (row :net-income "NetIncome" 410.0 "2024-03-31")
                           (row :net-income "NetIncome" 420.0 "2024-06-30")]}
        first-vintage (build/statement-values [early late] splits :first)
        latest-vintage (build/statement-values [early late] splits :latest)
        k (fn [f pe] [f :quarterly pe])]
    (is (= 1.0 (:value (first-vintage (k :diluted-eps "2024-03-31")))))
    (is (= 400.0 (:value (first-vintage (k :diluted-average-shares "2024-03-31")))))
    (is (not (:revised? (first-vintage (k :diluted-eps "2024-03-31")))) "a split is not a restatement")
    (is (= 400.0 (:value (first-vintage (k :net-income "2024-03-31")))) ":first keeps the earliest version")
    (is (= 410.0 (:value (latest-vintage (k :net-income "2024-03-31")))) ":latest takes the restatement")
    (is (:revised? (first-vintage (k :net-income "2024-03-31"))))
    (is (= 420.0 (:value (first-vintage (k :net-income "2024-06-30")))) "periods only in later snapshots are added")))

(deftest currencies
  (is (= ["GBP" 0.01] (build/major-currency "GBp")))
  (is (= ["USD" 1.0] (build/major-currency "USD")))
  (is (= "EURUSD=X" (build/fx-ticker "EUR" "USD")))
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "yahoo-build-test-" (System/nanoTime)))
        snap (fn [ticker currency]
               {:format 1 :ticker ticker :fetched-at "2025-01-02T00:00:00Z"
                :chart {:meta (assoc ny :currency currency) :bars [(bar "2024-01-02" 1.0)] :events {}}
                :statements [] :earnings-history [] :earnings-dates [] :errors {}})
        put! (fn [s] (build/write-edn-file! (io/file dir "raw" (:ticker s) "20250102T000000Z.edn") s))]
    (try
      (put! (snap "AAA.L" "GBP"))
      (put! (snap "BBB.L" "GBp"))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different currencies"
                            (build/build-dataset! (io/file dir "raw") dir))
          "pence and pounds would be added up as one currency")
      (is (= ["2024-01-02" "2024-01-02"]
             (:date-range (build/build-dataset! (io/file dir "raw") dir :allow-mixed-currencies? true))))
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest trailing-per-share-values-are-not-trusted
  ;; Yahoo does not restate its trailing per-share series for splits consistently
  (let [row (fn [field yahoo value] {:field field :yahoo-field yahoo :frequency :trailing :period-end "2024-01-31"
                                     :value value :currency "USD" :statement :income-statement})
        values (build/statement-values [{:fetched-at "2025-01-02T00:00:00Z"
                                         :statements [(row :diluted-eps "DilutedEPS" 11.93)
                                                      (row :diluted-average-shares "DilutedAverageShares" 2.5e9)
                                                      (row :net-income "NetIncome" 2.976e10)
                                                      (assoc (row :diluted-eps "DilutedEPS" 2.0) :period-end "2024-07-31")]}]
                                       [{:date "2024-06-10" :ratio 10.0}] :first)]
    (is (= #{[:net-income :trailing "2024-01-31"] [:diluted-eps :trailing "2024-07-31"]} (set (keys values)))
        "per-share values before the split are dropped, those after it kept")))

(deftest a-failed-download-keeps-the-earlier-prices
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "yahoo-build-test-" (System/nanoTime)))
        good {:format 1 :ticker "AAA" :fetched-at "2025-01-02T00:00:00Z"
              :chart {:meta ny :bars [(bar "2024-01-02" 1.0) (bar "2024-01-03" 1.1)] :events {}}
              :statements [] :earnings-history [] :earnings-dates [] :errors {}}
        failed (assoc good :fetched-at "2025-02-03T00:00:00Z" :chart nil :errors {:chart {:type :rate-limited}})]
    (try
      (build/write-edn-file! (io/file dir "raw" "AAA" "20250102T000000Z.edn") good)
      (build/write-edn-file! (io/file dir "raw" "AAA" "20250203T000000Z.edn") failed)
      (is (= ["2024-01-02" "2024-01-03"] (:date-range (build/build-dataset! (io/file dir "raw") dir))))
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest public-dates-fall-on-trading-days
  ;; reported after the close on Friday 2024-04-26: acted on Monday
  (let [snap {:fetched-at "2025-01-02T00:00:00Z"
              :chart {:meta ny :bars (map #(bar % 1.0) ["2024-04-25" "2024-04-26" "2024-04-29"]) :events {}}
              :statements [{:field :net-income :yahoo-field "NetIncome" :frequency :quarterly :period-end "2024-03-31"
                            :value 1.0 :currency "USD" :statement :income-statement}]
              :earnings-history [{:period-end "2024-03-31" :reported-date "2024-04-26" :reported-at (utc "2024-04-26T20:30:00")
                                  :eps-actual 1.0}]
              :earnings-dates []}
        entry (build/fundamentals-entry [snap] {})]
    (is (= "2024-04-29" (get-in entry [:reports [:quarterly "2024-03-31"] :public-date])))
    (is (= "2024-04-26" (get-in entry [:reports [:quarterly "2024-03-31"] :report-date])))
    (is (= "2024-04-29" (:public-date (first (:earnings entry)))))
    (is (= "2024-02-14" (get-in (build/fundamentals-entry [(-> snap
                                                               (assoc-in [:statements 0 :period-end] "2023-12-31")
                                                               (assoc :earnings-history []))] {})
                                [:reports [:quarterly "2023-12-31"] :public-date]))
        "a date before the price history is not moved")))

(deftest a-scheduled-date-in-an-old-snapshot-is-not-a-report
  ;; the first snapshot lists a date that was only scheduled then and later moved
  (let [row (fn [at] {:announced-epoch (utc at) :timing "TAS" :event-name "Q4 2024 Earnings Call"})
        snap (fn [fetched dates] {:fetched-at fetched
                                  :chart {:meta ny :bars [(bar "2025-01-02" 1.0)] :events {}}
                                  :statements [{:field :net-income :yahoo-field "NetIncome" :frequency :quarterly
                                                :period-end "2024-12-31" :value 1.0 :currency "USD"
                                                :statement :income-statement}]
                                  :earnings-history [] :earnings-dates dates})
        entry (build/fundamentals-entry [(snap "2025-01-10T00:00:00Z" [(row "2025-01-28T21:00:00")])
                                         (snap "2025-06-30T00:00:00Z" [(row "2025-02-04T21:00:00")])]
                                        {})]
    (is (= "2025-02-04" (get-in entry [:reports [:quarterly "2024-12-31"] :report-date])))))

(deftest empty-ranges-and-failed-fx-downloads
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "yahoo-build-test-" (System/nanoTime)))
        snap {:format 1 :ticker "AAA" :fetched-at "2025-01-02T00:00:00Z"
              :chart {:meta ny :bars [(bar "2024-01-02" 1.0) (bar "2024-01-03" 1.1)] :events {}}
              :statements [] :earnings-history [] :earnings-dates [] :errors {}}
        fx {:format 1 :ticker "EURUSD=X" :from "EUR" :to "USD" :fetched-at "2025-01-02T00:00:00Z"
            :chart {:meta {:exchange-timezone-name "Europe/London"} :bars [(bar "2024-01-02" 1.1)] :events {}}}]
    (try
      (build/write-edn-file! (io/file dir "raw" "AAA" "20250102T000000Z.edn") snap)
      (build/write-edn-file! (io/file dir "raw" "_fx" "EURUSD=X" "20250102T000000Z.edn") fx)
      (build/write-edn-file! (io/file dir "raw" "_fx" "EURUSD=X" "20250203T000000Z.edn")
                             (assoc fx :fetched-at "2025-02-03T00:00:00Z" :chart nil))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"No prices between"
                            (build/build-dataset! (io/file dir "raw") dir :start "2025-01-01")))
      (is (= [["EUR" "USD"]] (:fx-pairs (build/build-dataset! (io/file dir "raw") dir)))
          "a failed FX download keeps the earlier rates")
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))
