(ns clojure-backtesting.yahoo-live-test
  "Downloads real data from Yahoo Finance and checks it against known
   facts. Needs the network, so it only runs with `lein test :live`."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-yfinance.experimental.fundamentals :as yff]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.fundamentals :refer :all]
            [clojure-backtesting.yahoo :refer :all]))

(def ^:private dir (io/file (System/getProperty "java.io.tmpdir") (str "clojure-backtesting-live-" (System/nanoTime))))

;; A delay rather than a :once fixture: clojure.test runs fixtures even when
;; the selector leaves none of the namespace's tests to run.
(def ^:private dataset
  (delay
    (let [result (download-yahoo! dir ["NVDA" "AAPL" "MSFT" "SPY"] :start "2023-01-01")]
      (build-yahoo-dataset! dir)
      result)))

(defn- load! []
  (is (empty? (:failed (deref dataset))) (str "download failures: " (:failed (deref dataset))))
  (load-yahoo-dataset dir))

;; Cleanup only: the download itself is the delay above. With no live test
;; selected the directory does not exist and there is nothing to delete.
(use-fixtures :once
  (fn [f]
    (try
      (f)
      (finally
        (doseq [x (reverse (file-seq dir))] (io/delete-file x true))))))

(deftest ^:live nvda-trades-at-its-pre-split-price
  (load!)
  ;; NVDA closed at 1208.88 on 2024-06-07, then split 10-for-1 on 2024-06-10
  (is (< (Math/abs (- 1208.88 (get-permno-price "2024-06-07" "NVDA"))) 0.01))
  (is (= 10.0 (get-permno-by-key "2024-06-07" "NVDA" :CFACPR)))
  (is (= 1.0 (get-permno-by-key "2024-06-10" "NVDA" :CFACPR)))
  (is (< (Math/abs (double (get-permno-by-key "2024-06-10" "NVDA" :RET))) 0.1)))

(deftest ^:live trailing-eps-matches-yahoo
  (load!)
  (init-portfolio (first (first (rsubseq data-files < (first (last data-files))))) 100000)
  (doseq [t ["AAPL" "MSFT"]]
    (let [ours (fundamental t :diluted-eps)
          yahoos (get-in (yff/fetch-fundamentals t) [:defaultKeyStatistics :trailingEps :raw])]
      (is (number? ours) t)
      (when (and ours yahoos)
        (is (< (Math/abs (- 1.0 (/ ours yahoos))) 0.1)
            (str t ": twelve-month EPS " ours " vs Yahoo's " yahoos)))))
  (is (< 5 (pe-ratio "AAPL") 100))
  (is (nil? (fundamental "SPY" :net-income)) "an ETF has no statements")
  (is (some? (latest-earnings "AAPL")))
  (end-order)
  (clear-fundamentals!))
