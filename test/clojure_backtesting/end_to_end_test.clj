(ns clojure-backtesting.end-to-end-test
  "Runs the whole engine against the synthetic sample dataset: loading and
   furnishing, T+1 fills, dollar valuation, dividends with and without
   reinvestment, splits, gaps, order expiry, margin, and the evaluation
   report. Expected values are read back from the data rather than
   hard-coded, so regenerating the dataset does not break the tests."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure-backtesting.sample-data :as sample]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.indicators :refer :all]))

(def ^:private aaa "10001")
(def ^:private bbb "10002")
(def ^:private ccc "10003")

(defn- close?
  ([a b] (close? a b 1e-6))
  ([a b tol] (< (Math/abs (- (double a) (double b))) tol)))

(defn- with-sample-dataset [f]
  (let [dir (io/file (System/getProperty "java.io.tmpdir")
                     (str "clojure-backtesting-e2e-" (System/nanoTime)))]
    (sample/write-sample-dataset dir)
    (load-dataset (str (io/file dir "main")) "main" add-aprc)
    (load-dataset (str (io/file dir "compustat")) "compustat")
    (try
      (f)
      (finally
        (when-not (deref TERMINATED) (end-order))
        (alter-var-root #'clojure-backtesting.data/headers2 (constantly nil))
        (alter-var-root #'clojure-backtesting.data/data-files2 (constantly {}))
        (doseq [x (reverse (file-seq dir))] (io/delete-file x true))))))

(use-fixtures :once with-sample-dataset)

(defn- price
  ([permno] (get-permno-price permno))
  ([date permno] (get-permno-price date permno)))

(defn- cash [] (get-in (deref portfolio) [:cash :tot-val]))
(defn- quantity [permno] (get-in (deref portfolio) [permno :quantity]))
(defn- holding-value [permno] (get-in (deref portfolio) [permno :tot-val]))
(defn- step-to! [date] (while (neg? (compare (get-date) date)) (next-date)))

(deftest dataset-loads-and-is-furnished
  (is (= (count sample/trading-days) (count data-files)))
  (is (= "1990-01-02" (first (first data-files))))
  (is (= "1990-03-30" (first (last data-files))))
  (is (some #{:APRC} headers) "add-aprc added the adjusted price column")
  (testing "adjusted price chains the total return across the split"
    (let [before (get-permno-info "1990-02-28" bbb)
          after (get-permno-info "1990-03-01" bbb)]
      (is (close? (+ 1.0 (:RET after)) (/ (:APRC after) (:APRC before))))
      (is (close? 0.5 (/ (:PRC after) (:PRC before)) 0.03) "the quoted price halves")
      (is (= 2.0 (:CFACPR before)))
      (is (= 1.0 (:CFACPR after)))))
  (testing "the gap security has no rows during the gap"
    (is (nil? (get-permno-info "1990-02-07" ccc)))
    (is (some? (get-permno-info "1990-02-12" ccc))))
  (testing "the supplementary dataset joins the latest filing that was public on the day"
    (is (= "1500.0" (get-permno-by-key "1990-01-02" aaa :atq)) "Q4 is not reported until 1990-01-24")
    (is (= "1550.0" (get-permno-by-key "1990-01-24" aaa :atq)))
    (is (= "5200.0" (get-permno-by-key "1990-01-26" bbb :atq)) "BBB reports Q4 on 1990-01-29")
    (is (= "5300.0" (get-permno-by-key "1990-01-29" bbb :atq)))
    (is (= "5300.0" (get-permno-by-key "1990-03-30" bbb :atq)))
    (is (nil? (get-permno-by-key "1990-01-02" ccc :atq)))))

(deftest orders-fill-next-day-in-dollars
  (init-portfolio "1990-01-02" 100000)
  (order aaa 100)
  (is (= 1 (count (deref pending-order))))
  (is (nil? (quantity aaa)) "nothing is held until the next close")
  (next-date)
  (is (= "1990-01-03" (get-date)))
  (is (empty? (deref pending-order)))
  (is (close? 100.0 (quantity aaa)))
  (is (close? (- 100000 (* 100 (price aaa))) (cash)) "cash falls by shares times the fill price")
  (is (close? (* 100 (price aaa)) (holding-value aaa)))
  (is (close? 100000 (total-value)) "a fill does not change total value")
  (let [record (first (deref order-record))]
    (is (= (price aaa) (:price record)))
    (is (= 100 (:quantity record))))
  (next-date)
  (testing "a day without distributions leaves the share count alone"
    (is (close? 100.0 (quantity aaa)))
    (is (close? (+ (cash) (* 100 (price aaa))) (total-value))))
  (testing "selling returns shares times price to cash"
    (let [cash-before (cash)]
      (order aaa -40)
      (next-date)
      (is (close? 60.0 (quantity aaa)))
      (is (close? (+ cash-before (* 40 (price aaa))) (cash)))))
  (end-order))

(deftest dividends-are-reinvested-by-default
  (init-portfolio "1990-02-13" 100000)
  (order aaa 100)
  (next-date) ; fills 1990-02-14
  (let [cash-after-fill (cash)
        fill-price (price aaa)]
    (next-date) ; 1990-02-15 pays $0.50
    (is (= "1990-02-15" (get-date)))
    (let [today (get-permno-info aaa)]
      (is (close? (+ 100 (/ 50.0 (:PRC today))) (quantity aaa)) "the dividend bought more shares")
      (is (close? cash-after-fill (cash)) "no cash was paid out")
      (is (close? (* 100 fill-price (+ 1.0 (:RET today))) (holding-value aaa)) "value follows total return")))
  (end-order))

(deftest dividends-can-be-paid-to-cash
  (with-redefs [REINVEST-DIVIDENDS false]
    (init-portfolio "1990-02-13" 100000)
    (order aaa 100)
    (next-date)
    (let [cash-after-fill (cash)
          fill-price (price aaa)]
      (next-date)
      (let [today (get-permno-info aaa)]
        (is (close? 100.0 (quantity aaa)) "the share count is unchanged")
        (is (close? (+ cash-after-fill 50.0) (cash)) "100 shares times $0.50 arrived in cash")
        (is (close? (* 100 (:PRC today)) (holding-value aaa)))
        (is (close? (+ (cash) (* 100 (:PRC today))) (total-value)))
        (is (close? (+ cash-after-fill (* 100 fill-price (+ 1.0 (:RET today)))) (total-value))
            "total value is the same as with reinvestment"))))
  (end-order))

(deftest splits-double-the-share-count
  (testing "with reinvestment"
    (init-portfolio "1990-02-27" 100000)
    (order bbb 100)
    (next-date) ; fills 1990-02-28
    (let [cash-after-fill (cash)]
      (next-date) ; 1990-03-01 is the split day
      (is (close? 200.0 (quantity bbb)))
      (is (close? cash-after-fill (cash)))
      (is (close? (* 200 (price bbb)) (holding-value bbb))))
    (end-order))
  (testing "without reinvestment, using CFACPR"
    (with-redefs [REINVEST-DIVIDENDS false]
      (init-portfolio "1990-02-27" 100000)
      (order bbb 100)
      (next-date)
      (let [cash-after-fill (cash)]
        (next-date)
        (is (close? 200.0 (quantity bbb)))
        (is (close? cash-after-fill (cash)) "a split pays nothing out")
        (is (close? (* 200 (price bbb)) (holding-value bbb)))))
    (end-order)))

(deftest missing-days-and-order-expiry
  (testing "a holding is carried at its last price through a gap and revalued on return"
    (init-portfolio "1990-02-01" 100000)
    (order ccc 100)
    (next-date) ; fills 1990-02-02
    (let [fill-price (price ccc)]
      (step-to! "1990-02-07")
      (is (nil? (get-permno-info ccc)))
      (is (close? (* 100 fill-price) (holding-value ccc)))
      (step-to! "1990-02-12")
      (is (close? 100.0 (quantity ccc)) "no dividends, so still 100 shares")
      (is (close? (* 100 (price ccc)) (holding-value ccc))))
    (end-order))
  (testing "an order that cannot fill within its expiry in trading days is dropped"
    (init-portfolio "1990-02-01" 100000)
    (next-date) ; 1990-02-02, the last day before the gap
    (order ccc 100 :expiration 1)
    (is (= "1990-02-05" (:expire (val (first (deref pending-order))))))
    (next-date) ; 1990-02-05: no row, still pending
    (is (= 1 (count (deref pending-order))))
    (next-date) ; 1990-02-06: past the expiry date
    (is (empty? (deref pending-order)))
    (is (nil? (quantity ccc)))
    (is (close? 100000 (cash)))
    (end-order)))

(deftest margin-interest-and-liquidation
  (with-redefs [INTEREST-RATE 0.10
                TRANSACTION-COST 0.01]
    (init-portfolio "1990-01-02" 10000)
    (order aaa 300) ; about $15,000 of stock on $10,000 of cash
    (next-date)
    (let [p (price aaa)
          entry (last (deref portfolio-value))
          expected-cash (- 10000 (* 300 p) (* 300 p 0.01))]
      (is (close? 300.0 (quantity aaa)))
      (is (close? expected-cash (cash)) "cash went negative by the loan plus commission")
      (is (close? (- expected-cash) (:loan entry)) "the loan is the negative cash balance")
      (is (close? (/ (:tot-value entry) (+ (:tot-value entry) (:loan entry))) (:margin entry)))
      (is (true? (deref LOAN-EXIST)))
      (next-date)
      (is (close? (- expected-cash (* (:loan entry) (/ 0.10 252))) (cash))
          "one day of interest was debited from cash")
      (is (> (:loan (last (deref portfolio-value))) (:loan entry)) "and the loan grew by it"))
    (with-redefs [MAINTENANCE-MARGIN 0.99]
      (next-date)
      (is (true? (deref TERMINATED)) "the margin call closed the account")
      (is (= [:cash] (keys (deref portfolio))))
      (is (empty? (deref pending-order)))))
  (is (nil? (next-date)) "nothing moves after termination"))

(deftest csv-records-are-written-only-on-request
  (let [dir (io/file (System/getProperty "java.io.tmpdir") (str "clojure-backtesting-out-" (System/nanoTime)))]
    (try
      (with-redefs [OUTPUT-DIR (str dir)]
        (init-portfolio "1990-01-02" 100000)
        (order aaa 100)
        (next-date)
        (update-eval-report)
        (end-order))
      (doseq [[f header-cols rows] [["out_order_record.csv" 4 2]
                                    ["out_portfolio_value_record.csv" 8 3]
                                    ["out_evaluation_report.csv" 8 2]]]
        (let [lines (clojure.string/split-lines (slurp (io/file dir f)))]
          (is (= header-cols (count (clojure.string/split (first lines) #","))) f)
          (is (= rows (count (rest lines))) f)))
      (finally
        (doseq [x (reverse (file-seq dir))] (io/delete-file x true)))))
  (init-portfolio "1990-01-02" 100000)
  (is (nil? (deref order-wrtr)) "no record is open without OUTPUT-DIR")
  (is (not (.exists (io/file "out_order_record.csv"))) "nothing is written to the working directory")
  (end-order))

(deftest short-positions-are-margined
  (init-portfolio "1990-01-02" 10000)
  (order aaa -100) ; about $5,000 short on $10,000 of equity
  (next-date)
  (let [p (price aaa)
        entry (last (deref portfolio-value))]
    (is (close? -100.0 (quantity aaa)))
    (is (close? (+ 10000 (* 100 p)) (cash)) "the sale proceeds arrive in cash")
    (is (close? 10000 (:tot-value entry)) "equity is unchanged by the trade")
    (is (close? (* 100 p) (:short entry)) "the borrowed stock is recorded")
    (is (close? 0.0 (:loan entry)) "no cash is borrowed")
    (is (close? (/ 10000 (* 100 p)) (:margin entry)) "margin is equity over gross position value")
    (is (true? (deref LOAN-EXIST)))
    (testing "a short that would breach the initial margin is rejected"
      (order aaa -400)
      (next-date)
      (is (close? -100.0 (quantity aaa)))
      (is (= 1 (count (deref order-record)))))
    (testing "a short pays dividends and its value follows total return"
      (init-portfolio "1990-02-13" 10000)
      (order aaa -100)
      (next-date)
      (let [fill-price (price aaa)]
        (next-date) ; 1990-02-15 dividend
        (is (close? (- (* 100 fill-price (+ 1.0 (:RET (get-permno-info aaa))))) (holding-value aaa)))))
    (testing "the maintenance margin liquidates a short"
      (init-portfolio "1990-01-02" 10000)
      (order aaa -100)
      (next-date)
      (with-redefs [MAINTENANCE-MARGIN 3.0]
        (next-date)
        (is (true? (deref TERMINATED)))
        (is (= [:cash] (keys (deref portfolio))))))))

(deftest automations-expire-in-trading-days
  (init-portfolio "1990-01-02" 100000)
  (set-automation (condition (fn [] true)) (action (fn [] (order aaa 1))) :expiration 1)
  (dotimes [_ 3] (next-date))
  (is (= 1 (count (deref dispatch-history))) "active on the next trading day only")
  (is (empty? (deref automated-conditions)))
  (end-order))

(deftest end-order-closes-positions-and-reports
  (init-portfolio "1990-01-02" 100000)
  (order aaa 100)
  (order bbb 50)
  (dotimes [_ 20]
    (next-date)
    (update-eval-report))
  (is (= 2 (count (deref order-record))))
  (is (nil? (MACD aaa)) "the 26-day EMA needs 26 closes")
  (dotimes [_ 10]
    (next-date)
    (update-eval-report))
  (is (< 0 (RSI aaa) 100))
  (is (pos? (EMA aaa)))
  (is (= 4 (count (MACD aaa))))
  (is (pos? (ATR aaa 14 1.0)))
  (is (number? (ROC aaa 5)))
  (let [value-before (total-value)]
    (end-order)
    (is (true? (deref TERMINATED)))
    (is (= [:cash] (keys (deref portfolio))))
    (is (close? value-before (cash) 1e-6) "closing out at the current price keeps total value")
    (is (= 4 (count (deref order-record))))
    (let [report (last (deref eval-record))]
      (is (every? #(and (number? %) (not (Double/isNaN (double %))))
                  ((juxt :tot-value :vol :sharpe :max-drawdown :pnl-pt) report)))
      (is (<= 0.0 (:max-drawdown report) 1.0)))))
