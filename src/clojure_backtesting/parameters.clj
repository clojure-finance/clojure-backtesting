(ns clojure-backtesting.parameters)

;; ============ CONFIGURABLE PARAMETERS ============

(def PRINT false)
(def DIRECT true)
(def ORDER-EXPIRATION 3)
(def LEVERAGE true)
(def PRICE-KEY :PRC) ;; trade at closing price
;; (def PRICE-KEY :OPENPRC) uncomment this if you want to trade at opening price instead

;; ============== Cache =================

(def CACHE-SIZE 60)
(defn CHANGE-CACHE-SIZE
  [size]
  (def CACHE-SIZE size))

;; =============== Indicator ===============
(def ^:dynamic EMA-CYCLE 20)
(def MACD-SIGNAL 9)
(def MACD-SHORT 12)
(def MACD-LONG 26)
(def ^:dynamic RSI-CYCLE 14)

(defn CHANGE-EMA-CYCLE
  [int]
  (def ^:dynamic EMA-CYCLE int)
  (def ^:dynamic EMA-K (/ 2 (+ EMA-CYCLE 1))))

(defn CHANGE-MACD-SIGNAL
  [int]
  (def MACD-SIGNAL int)
  (def MACD-SIGNAL-K (/ 2 (+ MACD-SIGNAL 1))))

(defn CHANGE-MACD-SHORT
  [int]
  (def MACD-SHORT int)
  (def MACD-SHORT-K (/ 2 (+ MACD-SHORT 1))))

(defn CHANGE-MACD-LONG
  [int]
  (def MACD-LONG int)
  (def MACD-LONG-K (/ 2 (+ MACD-LONG 1))))

(defn CHANGE-RSI-CYCLE
  [int]
  (def ^:dynamic RSI-CYCLE int))

(def SAR-AF-STEP 0.02) ; parabolic SAR acceleration factor: start and increment
(def SAR-AF-MAX 0.2) ; parabolic SAR acceleration factor: cap

;; ============ Parameters for margin requirements ============

(def INITIAL-MARGIN 0.5)
;; This is the min cash / order total
;; Set it to nil to enable infinite margin

(def MAINTENANCE-MARGIN 0.25)
;; When portfolio margin is < maintenance margin, all positions will be closed

;; ============ Parameters for interests & transaction costs ============

(def TRANSACTION-COST 0.0)
;; Commission fee (%) that needs to be paid when making a purchase, must be < 1.0

(def INTEREST-RATE 0.0)
;; The simple interest rate (p.a.) for making a loan
;; Usually between 3% and 12% per annum

;; ============ Functions for updating parameters ============

(defn update-initial-margin
  "This function updates the initial margin."
  [new-im]
  (if (or (= new-im nil) (> new-im 0))
    (def INITIAL-MARGIN new-im)
    (println "Failed: The initial margin needs to be greater than zero.")))

(defn update-maintenance-margin
  "This function updates the maintenance margin."
  [new-mm]
  (if (> new-mm 0)
    (def MAINTENANCE-MARGIN new-mm)
    (println "Failed: The maintenance margin needs to be greater than zero.")))

(defn update-interest-rate
  "This function updates the interest rate. 0 switches interest off."
  [new-ir]
  (if (and (>= new-ir 0) (< new-ir 1))
    (def INTEREST-RATE new-ir)
    (println "Failed: The interest rate needs to be within the range of [0,1).")))

(defn update-transaction-cost
  "This function updates the transaction cost. 0 switches commission off."
  [new-tc]
  (if (and (>= new-tc 0) (< new-tc 1))
    (def TRANSACTION-COST new-tc)
    (println "Failed: The transaction cost needs to be within the range of [0,1).")))

;; ============ Dividends ============

(def REINVEST-DIVIDENDS true)
;; When true (the default) a holding's value follows the security's total
;; return: dividends are reinvested in the same security, so the share count
;; grows on dividend days. When false, dividends are paid into cash and the
;; share count changes only on splits. Recognising a split needs the CRSP
;; CFACPR column; without it a split is paid out as cash too.

(defn update-reinvest-dividends
  "This function switches dividend reinvestment on or off."
  [flag]
  (def REINVEST-DIVIDENDS (boolean flag)))

;; ============ Output ============

(def OUTPUT-DIR nil)
;; Directory to write out_order_record.csv, out_portfolio_value_record.csv
;; and out_evaluation_report.csv into during a run. nil (the default) writes
;; no files.

(defn update-output-dir
  "This function sets the directory for the CSV records, or nil for none."
  [dir]
  (def OUTPUT-DIR dir))

;; ============ FIXED PARAMETERS ============

(def ^:dynamic EMA-K (/ 2 (+ EMA-CYCLE 1)))
(def MACD-SIGNAL-K (/ 2 (+ MACD-SIGNAL 1)))
(def MACD-SHORT-K (/ 2 (+ MACD-SHORT 1)))
(def MACD-LONG-K (/ 2 (+ MACD-LONG 1)))

(def TICKER-KEY :PERMNO)
