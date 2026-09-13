(ns clojure-backtesting.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.order]
            [clojure-backtesting.indicators]))

(defn- close?
  ([a b] (close? a b 1e-9))
  ([a b tol] (< (Math/abs (- (double a) (double b))) tol)))

(deftest math-calculation
  (is (= 2.0 (mean [1 2 3])))
  (is (close? 1.0 (sample-sd [1 2 3])))
  (is (close? 1.5811388 (sample-sd [1 2 3 4 5]) 1e-6))
  (is (= 0.0 (sample-sd [7])))
  (is (close? 0.0 (log-return 100 100)))
  (is (close? (Math/log 1.05) (log-return 105 100)))
  (is (= 0.0 (log-return -5 100)) "non-positive equity gives a zero return instead of NaN"))

(deftest aprc-chains-daily-returns
  ;; rows are [PERMNO PRC RET]; two securities over three days
  (reset! initial-price {})
  (reset! cum-ret {})
  (let [day1 (add-aprc-file [["A" 10.0 0.02] ["B" 50.0 -0.01]] 1 2 0)
        day2 (add-aprc-file [["A" 11.0 0.10] ["B" 45.0 -0.10]] 1 2 0)
        ;; day 3: A has a 2-for-1 split (price halves, zero return), B gains 10%
        day3 (add-aprc-file [["A" 5.5 0.0] ["B" 49.5 0.10]] 1 2 0)
        row (fn [rows id] (first (filter #(= id (first %)) rows)))
        aprc (fn [rows id] (nth (row rows id) 4))]
    (testing "first day: INIT-PRICE = APRC = PRC and CUM-RET = 0"
      (is (= [10.0 10.0 0.0] (subvec (row day1 "A") 3)))
      (is (= [50.0 50.0 0.0] (subvec (row day1 "B") 3))))
    (testing "APRC(t) / APRC(t-1) = 1 + RET(t)"
      (is (close? 1.10 (/ (aprc day2 "A") (aprc day1 "A"))))
      (is (close? 0.90 (/ (aprc day2 "B") (aprc day1 "B"))))
      (is (close? 1.10 (/ (aprc day3 "B") (aprc day2 "B")))))
    (testing "a split leaves the adjusted price continuous"
      (is (close? 11.0 (aprc day3 "A")))
      (is (close? 5.5 (nth (row day3 "A") 1))))
    (testing "CUM-RET is the cumulative natural-log return"
      (is (close? (+ (Math/log 1.10) (Math/log 1.0)) (nth (row day3 "A") 5)))
      (is (close? (* 1.10 0.90 50.0) (aprc day3 "B"))))))

(deftest evaluation-metrics
  (with-redefs [portfolio-value (atom [{:date "d0" :tot-value 100 :daily-ret 0.0}
                                       {:date "d1" :tot-value 110 :daily-ret (Math/log 1.1)}
                                       {:date "d2" :tot-value 88 :daily-ret (Math/log 0.8)}
                                       {:date "d3" :tot-value 99 :daily-ret (Math/log 1.125)}])]
    (testing "max drawdown is peak-to-trough on total value"
      (is (close? 0.2 (max-drawdown))))
    (testing "sharpe is annualised mean/sd of daily returns"
      (let [rets (get-daily-returns)
            expected (* (/ (mean rets) (sample-sd rets)) (Math/sqrt 252))]
        (is (close? expected (sharpe-ratio)))
        (is (close? expected (rolling-sharpe-ratio)) "window larger than history uses all of it"))))
  (testing "a single observation gives zeros rather than NaN or an exception"
    (with-redefs [portfolio-value (atom [{:date "d0" :tot-value 100 :daily-ret 0.0}])]
      (is (= 0.0 (volatility)))
      (is (= 0.0 (rolling-volatility)))
      (is (= 0.0 (sharpe-ratio)))
      (is (= 0.0 (max-drawdown))))))

(deftest loan-balance-and-interest
  (with-redefs [portfolio (atom {:cash {:tot-val -500.0} "A" {:tot-val 1500.0}})
                portfolio-value (atom [{:date "d0" :tot-value 1000.0 :daily-ret 0.0 :tot-ret 0.0
                                        :loan 0.0 :leverage 0.0 :margin 0.0}])
                LOAN-EXIST (atom false)
                portvalue-wrtr (java.io.StringWriter.)]
    (testing "the loan is the negative cash balance"
      (update-loan "d1" 500.0 false)
      (let [e (last @portfolio-value)]
        (is (close? 500.0 (:loan e)))
        (is (close? 0.5 (:leverage e)))
        (is (close? (/ 1000.0 1500.0) (:margin e)))
        (is (close? 0.0 (:daily-ret e)) "equity return carries no leverage multiplier")
        (is (true? @LOAN-EXIST))))
    (testing "interest is charged once: equity falls by it and the loan grows by it"
      (update-loan "d2" 10.0 true)
      (let [e (last @portfolio-value)]
        (is (close? -510.0 (get-in @portfolio [:cash :tot-val])))
        (is (close? 990.0 (:tot-value e)))
        (is (close? 510.0 (:loan e)))
        (is (close? (Math/log 0.99) (:daily-ret e)))))
    (testing "selling the position repays the loan"
      (swap! portfolio assoc :cash {:tot-val 990.0} "A" {:tot-val 0.0})
      (update-loan "d3" 0 false)
      (let [e (last @portfolio-value)]
        (is (close? 0.0 (:loan e)))
        (is (close? 0.0 (:leverage e)))
        (is (close? 1.0 (:margin e)))))))

(deftest transaction-cost-charged-on-sells
  (let [incur #'clojure-backtesting.order/incur-transaction-cost]
    (with-redefs [portfolio (atom {:cash {:tot-val 1000.0}})
                  TRANSACTION-COST 0.01]
      (incur 10 5.0)
      (is (close? 999.5 (get-in @portfolio [:cash :tot-val])) "buy of 50 costs 0.5")
      (incur -10 5.0)
      (is (close? 999.0 (get-in @portfolio [:cash :tot-val])) "sell of 50 also costs 0.5"))))

(deftest compustat-join-uses-latest-prior-filing
  (let [dir (doto (io/file (System/getProperty "java.io.tmpdir")
                           (str "clojure-backtesting-test-" (System/nanoTime)))
              .mkdirs)
        write-filing (fn [datadate rows]
                       (let [f (io/file dir datadate)]
                         (spit f (str/join "\n" (map str rows)))
                         f))
        q1 (write-filing "2020-03-31" [["10001" "2020-03-31" "q1-a"] ["10002" "2020-03-31" "q1-b"]])
        q2 (write-filing "2020-06-30" [["10001" "2020-06-30" "q2-a"]])
        crsp [{:PERMNO "10001" :date "x" :PRC 1.0}
              {:PERMNO "10002" :date "x" :PRC 2.0}
              {:PERMNO "10003" :date "x" :PRC 3.0}]]
    (try
      (with-redefs [headers2 [:PERMNO :datadate :atq]
                    data-files2 (sorted-map "2020-03-31" q1 "2020-06-30" q2)]
        (testing "the latest filing at or before the date is used, never a later one"
          (let [joined (merge-data crsp "2020-05-15")]
            (is (= "q1-a" (:atq (first joined))))
            (is (= "q1-b" (:atq (second joined))))
            (is (nil? (:atq (nth joined 2))) "unmatched security is left alone")
            (is (= 3.0 (:PRC (nth joined 2))))))
        (testing "a filing dated exactly on the date is used"
          (is (= "q2-a" (:atq (first (merge-data crsp "2020-06-30"))))))
        (testing "once a newer filing exists the older one is no longer used"
          (let [joined (merge-data crsp "2020-07-15")]
            (is (= "q2-a" (:atq (first joined))))
            (is (nil? (:atq (second joined))) "security absent from the latest filing gets nothing")))
        (testing "no filing before the date means no join"
          (is (= crsp (merge-data crsp "2020-01-15"))))
        (testing "a filing more than three months old is not joined"
          (is (= crsp (merge-data crsp "2020-12-15")))))
      (finally
        (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest rsi-wilder-smoothing
  (let [wilder #'clojure-backtesting.indicators/wilder-update
        rsi-value #'clojure-backtesting.indicators/rsi-value]
    (testing "a loss increases the average loss"
      (let [[g l] (wilder [1.0 1.0] -3.0 14)]
        (is (close? (/ 13.0 14) g))
        (is (close? (/ 16.0 14) l))))
    (testing "a gain increases the average gain"
      (let [[g l] (wilder [1.0 1.0] 3.0 14)]
        (is (close? (/ 16.0 14) g))
        (is (close? (/ 13.0 14) l))))
    (is (= 100 (rsi-value [1.0 0.0])))
    (is (close? 50.0 (rsi-value [1.0 1.0])))
    (is (close? 75.0 (rsi-value [3.0 1.0])))))

(deftest rs-seed-is-an-average
  (with-redefs [clojure-backtesting.data-management/get-permno-prev-n-days
                (fn [_ n] (map (fn [p] {:PRC p}) [12.0 11.0 10.0]))
                clojure-backtesting.data-management/get-permno-price
                (fn [_] 13.0)]
    ;; prices oldest to newest: 10 11 12 13 -> three gains of 1, no losses
    (is (= [1.0 0.0] (clojure-backtesting.indicators/RS "X" 4))))
  (with-redefs [clojure-backtesting.data-management/get-permno-prev-n-days
                (fn [_ n] (map (fn [p] {:PRC p}) [9.0 12.0 10.0]))
                clojure-backtesting.data-management/get-permno-price
                (fn [_] 15.0)]
    ;; 10 12 9 15 -> gains 2 and 6, loss 3, over three changes
    (let [[g l] (clojure-backtesting.indicators/RS "X" 4)]
      (is (close? (/ 8.0 3) g))
      (is (close? 1.0 l)))))

(deftest atr-uses-true-range-and-window
  (with-redefs [clojure-backtesting.data-management/get-permno-by-key
                (fn [_ k] (get {:BIDLO "98.0" :ASKHI 103.0} k))
                clojure-backtesting.data-management/get-permno-prev-n-days
                (fn [_ n] [{:PRC 95.0}])]
    ;; high-low 5, high-prev 8, low-prev 3 -> true range 8; window 10 from prev-atr 4
    (is (close? (/ (+ (* 4.0 9) 8.0) 10) (clojure-backtesting.indicators/ATR "X" 10 4.0))))
  (with-redefs [clojure-backtesting.data-management/get-permno-by-key
                (fn [_ k] (get {:BIDLO 98.0 :ASKHI 103.0} k))
                clojure-backtesting.data-management/get-permno-prev-n-days
                (fn [_ n] [])]
    (is (close? (/ (+ (* 4.0 13) 5.0) 14) (clojure-backtesting.indicators/ATR "X" 14 4.0)))))
