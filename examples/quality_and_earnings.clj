;; gorilla-repl.fileformat = 1

;; **
;;; ## Quality Screen and Earnings Announcements
;;;
;;; Two more uses of point-in-time fundamentals: Piotroski's F-score as a quality screen, and trading on earnings announcements. Both run on the synthetic snapshots in `resources/sample-yahoo`.
;; **

;; @@
(ns clojure-backtesting.examples.quality-and-earnings
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
(init-portfolio "2024-06-03" 100000)
;; @@
;; ->
;;; Warning: ZZZZ has no prices: No data found, symbol may be delisted
;;; Warning: BETA: 7 report dates are estimated from the reporting lag
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 2024-06-03 Cash: $100000&quot;</span>","value":"\"Date: 2024-06-03 Cash: $100000\""}
;; <=

;; **
;;; ### The F-score
;;;
;;; Nine yes/no signals from the last two fiscal years (profitability, cash flow, leverage, liquidity, dilution, margin, turnover). `piotroski-signals` shows each one; `piotroski-f-score` counts them, or returns nil when an input is missing.
;; **

;; @@
(piotroski-signals "GAMA")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:delta-liquidity false, :no-dilution true, :accrual true, :delta-margin true, :delta-leverage false, :cfo true, :delta-turnover false, :delta-roa false, :roa true}</span>","value":"{:delta-liquidity false, :no-dilution true, :accrual true, :delta-margin true, :delta-leverage false, :cfo true, :delta-turnover false, :delta-roa false, :roa true}"}
;; <=

;; @@
(for [p ["ALFA" "BETA" "GAMA"]] {:security p :f-score (piotroski-f-score p)})
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>({:security &quot;ALFA&quot;, :f-score 4} {:security &quot;BETA&quot;, :f-score 4} {:security &quot;GAMA&quot;, :f-score 5})</span>","value":"({:security \"ALFA\", :f-score 4} {:security \"BETA\", :f-score 4} {:security \"GAMA\", :f-score 5})"}
;; <=

;; **
;;; ### History of a field
;;;
;;; `fundamental-history` lists what was public, newest first. Growth rates compare a value with the one a year earlier:
;; **

;; @@
(map #(select-keys % [:period-end :value :public-date]) (fundamental-history "ALFA" :total-revenue :annual 4))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>({:period-end &quot;2023-12-31&quot;, :value 1.4641000000000005E9, :public-date &quot;2024-02-12&quot;} {:period-end &quot;2022-12-31&quot;, :value 1.3310000000000005E9, :public-date &quot;2023-02-10&quot;} {:period-end &quot;2021-12-31&quot;, :value 1.2100000000000002E9, :public-date &quot;2022-02-10&quot;} {:period-end &quot;2020-12-31&quot;, :value 1.1E9, :public-date &quot;2021-02-10&quot;})</span>","value":"({:period-end \"2023-12-31\", :value 1.4641000000000005E9, :public-date \"2024-02-12\"} {:period-end \"2022-12-31\", :value 1.3310000000000005E9, :public-date \"2023-02-10\"} {:period-end \"2021-12-31\", :value 1.2100000000000002E9, :public-date \"2022-02-10\"} {:period-end \"2020-12-31\", :value 1.1E9, :public-date \"2021-02-10\"})"}
;; <=

;; @@
{:revenue-growth (growth "ALFA" :total-revenue)
 :eps-growth (growth "ALFA" :diluted-eps)}
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:revenue-growth 0.09999999999999987, :eps-growth 0.11111111111111116}</span>","value":"{:revenue-growth 0.09999999999999987, :eps-growth 0.11111111111111116}"}
;; <=

;; **
;;; ### Buying after good news
;;;
;;; `latest-earnings` gives the latest announcement public on the day, with the EPS surprise. This strategy buys a security the day an announcement beats the estimate and sells it 20 trading days later.
;; **

;; @@
(init-portfolio "2023-01-03" 100000)

(def bought (atom {})) ; permno -> date bought

(while (neg? (compare (get-date) "2024-12-31"))
  (doseq [p ["ALFA" "BETA" "GAMA"]]
    (let [e (latest-earnings p)]
      (when (and e (= (:public-date e) (get-date)) (pos? (or (:surprise-pct e) 0.0))
                 (not (contains? (deref bought) p)))
        (order-target-percent p 0.3)
        (swap! bought assoc p (get-date)))))
  (doseq [[p d] (deref bought)]
    (when (= d (get-prev-n-date 20))
      (order p 0 :remaining true)
      (swap! bought dissoc p)))
  (next-date)
  (update-eval-report))
(end-order)
(print-order-record 10)
;; @@
;; ->
;;; 
;;; |      :date | :permno |   :price |  :aprc |  :quantity |
;;; |------------+---------+----------+--------+------------|
;;; | 2023-02-13 |    ALFA |  367.336 | 368.72 |    81.2456 |
;;; | 2023-02-15 |    GAMA |  55.6942 |  55.69 |   547.6275 |
;;; | 2023-02-21 |    BETA |   8.0031 |   8.00 |  3729.5818 |
;;; | 2023-03-13 |    ALFA |  334.448 | 336.09 |   -81.3361 |
;;; | 2023-03-15 |    GAMA |  55.0107 |  55.01 |  -547.6275 |
;;; | 2023-03-21 |    BETA |   8.0405 |   8.04 | -3729.5818 |
;;; | 2023-05-01 |    GAMA |  59.1238 |  59.12 |   491.1352 |
;;; | 2023-05-02 |    ALFA | 362.5512 | 364.33 |    81.5209 |
;;; | 2023-05-09 |    BETA |    9.184 |   9.18 |  3157.1325 |
;;; | 2023-05-29 |    GAMA |  60.8056 |  60.81 |  -491.1352 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(last (deref eval-report-data))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:date &quot;2024-12-31&quot;, :tot-value &quot;$92246&quot;, :vol &quot;0.4041%&quot;, :r-vol &quot;0.1472%&quot;, :sharpe &quot;-0.6085&quot;, :r-sharpe &quot;-5.6701&quot;, :pnl-pt &quot;$-215&quot;, :max-drawdown &quot;8.1014%&quot;}</span>","value":"{:date \"2024-12-31\", :tot-value \"$92246\", :vol \"0.4041%\", :r-vol \"0.1472%\", :sharpe \"-0.6085\", :r-sharpe \"-5.6701\", :pnl-pt \"$-215\", :max-drawdown \"8.1014%\"}"}
;; <=
