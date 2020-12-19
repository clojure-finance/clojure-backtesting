(ns clojure-backtesting.parameters)

;; FIX PARAMETERS
(def NOMATCH "2oif94ksdajf09934")


;; CONFIGURABLE PARAMETERS
; This parameter is used in:
; get-pre-date-and-content
; 
(def MAXLOOKAHEAD 5)

(def LEVERAGE 1)

; This parameter is used in:
; next-date
(def MAXDISCONTINUITY 10)
