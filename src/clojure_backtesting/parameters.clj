(ns clojure-backtesting.parameters)

;; ============ FIXED PARAMETERS ============

(def NOMATCH "2oif94ksdajf09934")


;; ============ CONFIGURABLE PARAMETERS ============

(def MAXLOOKAHEAD 10) ; This parameter is used in `get-pre-date-and-content`
(def ORDER-EXPIRATION 3)
(def LEVERAGE true)
(def MAXDISCONTINUITY 10)
(def MAXRANGE 50000)
(def MAXMERGERANGE "1 year")
(def PRICE-KEY :PRC) ;; trade at closing price
;; (def PRICE-KEY :OPENPRC) uncomment this if you want to trade at opening price instead


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