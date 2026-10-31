;; gorilla-repl.fileformat = 1

;; **
;;; ## Yahoo Finance Data
;;;
;;; Prices and point-in-time fundamentals from Yahoo Finance, through [clj-yfinance](https://github.com/clojure-finance/clj-yfinance). A real download looks like this (it needs a network connection, and Yahoo rate-limits, so it takes a few seconds per security):
;;;
;;; ```clojure
;;; (download-yahoo! "data/tech" ["AAPL" "MSFT" "NVDA" "SPY"] :start "2015-01-01")
;;; (build-yahoo-dataset! "data/tech")
;;; (load-yahoo-dataset "data/tech")
;;; ```
;;;
;;; This worksheet uses the synthetic snapshots in `resources/sample-yahoo`, which have the same shape as a real download, so it runs offline. Their securities are invented: ALFA pays quarterly dividends and splits 4-for-1 on 2024-08-01, BETA lost money until 2022, GAMA reports in euros but trades in dollars, DLTA has prices only, IDX is an index.
;; **

;; @@
(ns clojure-backtesting.examples.yahoo-finance-data
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

;; **
;;; ### Build and load
;;;
;;; Building is offline: it reads the raw snapshots and writes the dataset. The warnings say what could not be done exactly: ZZZZ was not found by Yahoo (delisted), and some of BETA's report dates are estimated from a reporting lag because Yahoo has no date for them.
;; **

;; @@
(def info (build-yahoo-dataset! "resources/sample-yahoo" :out-dir "target/sample-yahoo"))
(:date-range info)
;; @@
;; ->
;;; Warning: ZZZZ has no prices: No data found, symbol may be delisted
;;; Warning: BETA: 7 report dates are estimated from the reporting lag
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>[&quot;2022-01-03&quot; &quot;2025-01-01&quot;]</span>","value":"[\"2022-01-03\" \"2025-01-01\"]"}
;; <=

;; @@
(load-yahoo-dataset "target/sample-yahoo")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 2022-01-03 ~ 2025-01-01; Fundamentals: 5 securities, 33 reports, 1 FX series&quot;</span>","value":"\"Date range: 2022-01-03 ~ 2025-01-01; Fundamentals: 5 securities, 33 reports, 1 FX series\""}
;; <=

;; **
;;; ### Prices as traded
;;;
;;; Yahoo quotes old prices divided by every later split. The dataset restores the prices as they were traded, with CRSP's `CFACPR` (the later splits) and `RET` (total return), so a split is not a loss:
;; **

;; @@
(for [date ["2024-07-30" "2024-07-31" "2024-08-01" "2024-08-02"]]
  (select-keys (get-permno-info date "ALFA") [:date :PRC :CFACPR :RET :DIVAMT :APRC]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>({:date &quot;2024-07-30&quot;, :PRC 393.5576, :CFACPR 4.0, :RET -0.008683018442094403, :DIVAMT 0.0, :APRC 397.58620611357276} {:date &quot;2024-07-31&quot;, :PRC 382.4636, :CFACPR 4.0, :RET -0.02818901223099235, :DIVAMT 0.0, :APRC 386.37864368656335} {:date &quot;2024-08-01&quot;, :PRC 93.9474, :CFACPR 1.0, :RET -0.017450026616911973, :DIVAMT 0.0, :APRC 379.6363260700265} {:date &quot;2024-08-02&quot;, :PRC 93.4705, :CFACPR 1.0, :RET -0.005076244792298623, :DIVAMT 0.0, :APRC 377.7091991468461})</span>","value":"({:date \"2024-07-30\", :PRC 393.5576, :CFACPR 4.0, :RET -0.008683018442094403, :DIVAMT 0.0, :APRC 397.58620611357276} {:date \"2024-07-31\", :PRC 382.4636, :CFACPR 4.0, :RET -0.02818901223099235, :DIVAMT 0.0, :APRC 386.37864368656335} {:date \"2024-08-01\", :PRC 93.9474, :CFACPR 1.0, :RET -0.017450026616911973, :DIVAMT 0.0, :APRC 379.6363260700265} {:date \"2024-08-02\", :PRC 93.4705, :CFACPR 1.0, :RET -0.005076244792298623, :DIVAMT 0.0, :APRC 377.7091991468461})"}
;; <=

;; **
;;; ### Fundamentals as they were known
;;;
;;; `fundamental` returns the latest value that was public on the backtest date. ALFA reports its second quarter on 2024-07-30 after the close, so the numbers are usable from the next day:
;; **

;; @@
(init-portfolio "2024-07-30" 100000)
(fundamental-info "ALFA" :net-income :quarterly)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:period-end &quot;2024-03-31&quot;, :reported-value 6.0394125000000015E7, :reported-currency &quot;USD&quot;, :value 6.0394125000000015E7, :frequency :quarterly, :field :net-income, :source :quarterly, :currency &quot;USD&quot;, :report-date-source :earnings-history, :public-date &quot;2024-05-01&quot;, :report-date &quot;2024-04-30&quot;}</span>","value":"{:period-end \"2024-03-31\", :reported-value 6.0394125000000015E7, :reported-currency \"USD\", :value 6.0394125000000015E7, :frequency :quarterly, :field :net-income, :source :quarterly, :currency \"USD\", :report-date-source :earnings-history, :public-date \"2024-05-01\", :report-date \"2024-04-30\"}"}
;; <=

;; @@
(next-date)
(fundamental-info "ALFA" :net-income :quarterly)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:period-end &quot;2024-06-30&quot;, :reported-value 5.7374418750000015E7, :reported-currency &quot;USD&quot;, :value 5.7374418750000015E7, :frequency :quarterly, :field :net-income, :source :quarterly, :currency &quot;USD&quot;, :report-date-source :earnings-history, :public-date &quot;2024-07-31&quot;, :report-date &quot;2024-07-30&quot;}</span>","value":"{:period-end \"2024-06-30\", :reported-value 5.7374418750000015E7, :reported-currency \"USD\", :value 5.7374418750000015E7, :frequency :quarterly, :field :net-income, :source :quarterly, :currency \"USD\", :report-date-source :earnings-history, :public-date \"2024-07-31\", :report-date \"2024-07-30\"}"}
;; <=

;; **
;;; Per-share figures are stated per share of the day. Before the split, ALFA's EPS is four times what Yahoo shows today; afterwards the figure is the same as Yahoo's. The market capitalisation does not jump:
;; **

;; @@
(defn per-share-view [permno]
  {:date (get-date)
   :price (get-permno-price permno)
   :eps (fundamental permno :diluted-eps)
   :shares (shares-outstanding permno)
   :market-cap (market-cap permno)})

(per-share-view "ALFA")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:date &quot;2024-07-31&quot;, :price 382.4636, :eps 24.100746837372363, :shares 9319702.48902, :market-cap 3.564446964879549E9}</span>","value":"{:date \"2024-07-31\", :price 382.4636, :eps 24.100746837372363, :shares 9319702.48902, :market-cap 3.564446964879549E9}"}
;; <=

;; @@
(next-date)
(per-share-view "ALFA")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:date &quot;2024-08-01&quot;, :price 93.9474, :eps 6.025186709343091, :shares 3.727880995608E7, :market-cap 3.50224727046783E9}</span>","value":"{:date \"2024-08-01\", :price 93.9474, :eps 6.025186709343091, :shares 3.727880995608E7, :market-cap 3.50224727046783E9}"}
;; <=

;; **
;;; GAMA reports in euros. Its figures are converted to its trading currency at the day's exchange rate, and the original stays available:
;; **

;; @@
(select-keys (fundamental-info "GAMA" :total-revenue) [:value :currency :reported-value :reported-currency :source :period-end])
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:value 2.737584998479053E9, :currency &quot;USD&quot;, :reported-value 2.38534160384E9, :reported-currency &quot;EUR&quot;, :source :sum-of-quarters, :period-end &quot;2024-06-30&quot;}</span>","value":"{:value 2.737584998479053E9, :currency \"USD\", :reported-value 2.38534160384E9, :reported-currency \"EUR\", :source :sum-of-quarters, :period-end \"2024-06-30\"}"}
;; <=

;; **
;;; Where Yahoo has no report date, the date is assumed from a reporting lag (45 days after a quarter, 90 after a fiscal year), and says so:
;; **

;; @@
(select-keys (fundamental-info "BETA" :net-income :quarterly) [:period-end :public-date :report-date-source])
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:period-end &quot;2024-03-31&quot;, :public-date &quot;2024-05-15&quot;, :report-date-source :lag}</span>","value":"{:period-end \"2024-03-31\", :public-date \"2024-05-15\", :report-date-source :lag}"}
;; <=

;; **
;;; ### Ratios
;;;
;;; Ratios combine the day's price with the fundamentals public that day:
;; **

;; @@
(for [p ["ALFA" "BETA" "GAMA" "DLTA"]]
  {:security p
   :pe (some->> (pe-ratio p) (format "%.1f"))
   :book-to-market (some->> (book-to-market p) (format "%.2f"))
   :dividend-yield (some->> (dividend-yield p) (* 100) (format "%.2f%%"))
   :roe (some->> (return-on-equity p) (* 100) (format "%.1f%%"))
   :f-score (piotroski-f-score p)})
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>({:security &quot;ALFA&quot;, :pe &quot;15.6&quot;, :book-to-market &quot;0.41&quot;, :dividend-yield &quot;0.43%&quot;, :roe &quot;16.2%&quot;, :f-score 4} {:security &quot;BETA&quot;, :pe &quot;25.2&quot;, :book-to-market &quot;0.54&quot;, :dividend-yield &quot;0.00%&quot;, :roe &quot;7.3%&quot;, :f-score 4} {:security &quot;GAMA&quot;, :pe &quot;31.7&quot;, :book-to-market &quot;0.24&quot;, :dividend-yield &quot;0.00%&quot;, :roe &quot;13.3%&quot;, :f-score 4} {:security &quot;DLTA&quot;, :pe nil, :book-to-market nil, :dividend-yield &quot;0.00%&quot;, :roe nil, :f-score nil})</span>","value":"({:security \"ALFA\", :pe \"15.6\", :book-to-market \"0.41\", :dividend-yield \"0.43%\", :roe \"16.2%\", :f-score 4} {:security \"BETA\", :pe \"25.2\", :book-to-market \"0.54\", :dividend-yield \"0.00%\", :roe \"7.3%\", :f-score 4} {:security \"GAMA\", :pe \"31.7\", :book-to-market \"0.24\", :dividend-yield \"0.00%\", :roe \"13.3%\", :f-score 4} {:security \"DLTA\", :pe nil, :book-to-market nil, :dividend-yield \"0.00%\", :roe nil, :f-score nil})"}
;; <=
