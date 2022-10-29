(ns clojure-backtesting.parameters)

;; ============ CONFIGURABLE PARAMETERS ============

(def PRINT false)
(def DIRECT true)
(def MAXLOOKAHEAD 10) ; This parameter is used in `get-pre-date-and-content`
(def ORDER-EXPIRATION 3)
(def LEVERAGE true)
(def MAXDISCONTINUITY 10)
(def MAXRANGE 50000)
(def MAXMERGERANGE "1 year")
(def PRICE-KEY :PRC) ;; trade at closing price
;; (def PRICE-KEY :OPENPRC) uncomment this if you want to trade at opening price instead

;; ============== Cache =================

(def CACHE-SIZE 30)
(defn CHANGE-CACHE-SIZE
  [size]
  (def CACHE-SIZE size))

;; =============== Indicator ===============
(def EMA-CYCLE 20)
(def MACD-SIGNAL 9)
(def MACD-SHORT 12)
(def MACD-LONG 26)
(def RSI-CYCLE 14)

(defn CHANGE-EMA-CYCLE
  [int]
  (def EMA-CYCLE int)
  (def EMA-K (/ 2 (+ EMA-CYCLE 1)))
  )

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
    (if (or (> new-im 0) (= new-im nil))
        (def INITIAL-MARGIN new-im)
        (println "Failed: The initial margin needs to be greater than zero.")))

(defn update-maintenance-margin 
    "This function updates the maintenance margin."
	[new-mm]
    (if (> new-mm 0)
        (def MAINTENANCE-MARGIN new-mm)
        (println "Failed: The maintenance margin needs to be greater than zero.")))

(defn update-interest-rate
    "This function updates the interest rate."
	[new-ir]
    (if (and (pos? new-ir) (< new-ir 1))
        (def INTEREST-RATE new-ir)
        (println "Failed: The interest rate needs to be within the range of [0,1).")))

(defn update-transaction-cost
    "This function updates the transaction cost."
	[new-tc]
    (if (and (pos? new-tc) (< new-tc 1))
        (def TRANSACTION-COST new-tc)
        (println "Failed: The transaction cost needs to be within the range of [0,1).")))

;; ============ FIXED PARAMETERS ============

;; (def NOMATCH "2oif94ksdajf09934")
(def EMA-K (/ 2 (+ EMA-CYCLE 1)))
(def MACD-SIGNAL-K (/ 2 (+ MACD-SIGNAL 1)))
(def MACD-SHORT-K (/ 2 (+ MACD-SHORT 1)))
(def MACD-LONG-K (/ 2 (+ MACD-LONG 1)))

(def TICKER-KEY :PERMNO)