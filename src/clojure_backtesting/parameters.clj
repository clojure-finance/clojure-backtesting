(ns clojure-backtesting.parameters)

;; FIXED PARAMETERS
(def NOMATCH "2oif94ksdajf09934")


;; CONFIGURABLE PARAMETERS
; This parameter is used in:
; get-pre-date-and-content
; 
(def MAXLOOKAHEAD 10)
(def ORDER-EXPIRATION 3)
(def LEVERAGE true)
(def MAXDISCONTINUITY 10)
(def MAXRANGE 50000)
(def MAXMERGERANGE "1 year")
(def PRICE-KEY :PRC)
;; (def PRICE-KEY :OPENPRC) uncomment this if you want to use the opening price instead of the closing price
;; 

;; ============ Parameters for margin requirements ============
(def INITIAL-MARGIN 0.5) 
;; This is the min cash / order total 
;; Set it to nil to enable infinite margin

(def MAINTENANCE-MARGIN 0.25)
;; When portfolio margin is < maintenance margin, all positions will be closed

;; ============ Parameters for interests & transaction costs ============

(def INTEREST-RATE 0.0)
;; The interest rate to for making a loan

(def TRANSACTION-COST 0.0)
;; Front-end load (%) that needs to be paid when making a purchase, must be < 1.0
;; Typically falls within a range of 3.75% to 5.75%


;; ============ Functions for updating parameters ============

(defn update-initial-margin 
    "This function updates the initial margin."
	[new-im]
    (if (> new-im 0)
        (def INITIAL-MARGIN new-im)
        (println "Failed: The initial margin needs to be greater than zero.")
    )
)


(defn update-maintenance-margin 
    "This function updates the maintenance margin."
	[new-mm]
    (if (> new-mm 0)
        (def MAINTENANCE-MARGIN new-mm)
        (println "Failed: The maintenance margin needs to be greater than zero.")
    )
)

(defn update-transaction-cost
    "This function updates the transaction cost."
	[new-tc]
    (if (and (pos new-tc) (< new-tc 1))
        (def TRANSACTION-COST new-tt)
        (println "Failed: The transaction cost needs to be within the range of [0,1).")
    )
)