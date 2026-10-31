;; gorilla-repl.fileformat = 1

;; **
;;; ## Value Strategy on Yahoo Data
;;;
;;; On the first trading day of every month, hold the two securities with the highest earnings yield (twelve-month EPS over price), equally weighted, and compare the result with an index. It runs on the synthetic snapshots in `resources/sample-yahoo`; see `yahoo_finance_data.clj` for how to download real data.
;; **

;; @@
(ns clojure-backtesting.examples.value-strategy
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.direct :refer :all]
            [clojure-backtesting.fundamentals :refer :all]
            [clojure-backtesting.strategy :refer :all]
            [clojure-backtesting.yahoo :refer :all]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(build-yahoo-dataset! "resources/sample-yahoo" :out-dir "target/sample-yahoo")
(load-yahoo-dataset "target/sample-yahoo")
;; @@
;; ->
;;; Warning: ZZZZ has no prices: No data found, symbol may be delisted
;;; Warning: BETA: 7 report dates are estimated from the reporting lag
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 2022-01-03 ~ 2025-01-01; Fundamentals: 5 securities, 33 reports, 1 FX series&quot;</span>","value":"\"Date range: 2022-01-03 ~ 2025-01-01; Fundamentals: 5 securities, 33 reports, 1 FX series\""}
;; <=

;; **
;;; ### The strategy
;;;
;;; `top-n` ranks the universe by any function of a security, skipping those without a value (DLTA has no fundamentals). `rebalance` sells what is no longer wanted and targets a fraction of equity for each pick; the orders fill at the next close. A commission of 0.1% is charged on every trade.
;; **

;; @@
(update-transaction-cost 0.001)
(init-portfolio "2023-01-03" 100000)

(def universe ["ALFA" "BETA" "GAMA" "DLTA"])
(def picks (atom []))

(while (neg? (compare (get-date) "2024-12-31"))
  (when (new-period? :month)
    (let [top (top-n earnings-yield 2 :universe universe)]
      (swap! picks conj {:date (get-date) :picks top})
      (rebalance (equal-weights top))))
  (next-date)
  (update-eval-report))
(end-order)
(update-transaction-cost 0.0) ; back to the default for other runs in this session
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#'clojure-backtesting.parameters/TRANSACTION-COST</span>","value":"#'clojure-backtesting.parameters/TRANSACTION-COST"}
;; <=

;; **
;;; The picks change as new reports become public:
;; **

;; @@
(->> (deref picks)
     (partition-by :picks)
     (map first)
     (clojure.pprint/print-table))
;; @@
;; ->
;;; 
;;; |      :date |          :picks |
;;; |------------+-----------------|
;;; | 2023-01-03 | ["ALFA" "GAMA"] |
;;; | 2024-04-01 | ["ALFA" "BETA"] |
;;; | 2024-11-01 | ["ALFA" "GAMA"] |
;;; | 2024-12-02 | ["ALFA" "BETA"] |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(last (deref eval-report-data))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:date &quot;2024-12-31&quot;, :tot-value &quot;$162866&quot;, :vol &quot;1.3020%&quot;, :r-vol &quot;1.4015%&quot;, :sharpe &quot;1.1415&quot;, :r-sharpe &quot;-3.2197&quot;, :pnl-pt &quot;$1232&quot;, :max-drawdown &quot;16.7536%&quot;}</span>","value":"{:date \"2024-12-31\", :tot-value \"$162866\", :vol \"1.3020%\", :r-vol \"1.4015%\", :sharpe \"1.1415\", :r-sharpe \"-3.2197\", :pnl-pt \"$1232\", :max-drawdown \"16.7536%\"}"}
;; <=

;; **
;;; ### Against the index
;;;
;;; `benchmark-stats` compares the daily returns with those of any security in the dataset, here the IDX index:
;; **

;; @@
(into (sorted-map)
      (map (fn [[k v]] [k (if (double? v) (format "%.4f" v) v)]))
      (benchmark-stats "IDX"))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:alpha &quot;0.1483&quot;, :benchmark-return &quot;0.1938&quot;, :beta &quot;1.0258&quot;, :correlation &quot;0.6613&quot;, :days 520, :excess-return &quot;0.4349&quot;, :information-ratio &quot;0.9698&quot;, :portfolio-return &quot;0.6287&quot;, :tracking-error &quot;0.1552&quot;}</span>","value":"{:alpha \"0.1483\", :benchmark-return \"0.1938\", :beta \"1.0258\", :correlation \"0.6613\", :days 520, :excess-return \"0.4349\", :information-ratio \"0.9698\", :portfolio-return \"0.6287\", :tracking-error \"0.1552\"}"}
;; <=
