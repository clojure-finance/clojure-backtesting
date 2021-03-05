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
(def INITIAL-MARGIN 0.5)
;; This is the min cash / order total 
;; set it to nil to enable infinite margin

(def MAINTENANCE-MARGIN 0.25)
;; When portfolio margin is < maintenance margin, all positions will be closed