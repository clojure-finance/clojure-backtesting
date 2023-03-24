;; gorilla-repl.fileformat = 1

;; **
;;; ## Rate of Change example
;; **

;; @@
; import libraries from kernel
(ns clojure-backtesting.demo
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [clojure-backtesting.direct :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
  ) ;; require all libriaries from core
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Import dataset
;; **

;; @@
; path to dataset = "/Volumes/T7/CRSP"
; change it to the relative path to your own dataset
;
(load-dataset "./CRSP" "main" add-aprc)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 1972-01-03 ~ 2017-02-10&quot;</span>","value":"\"Date range: 1972-01-03 ~ 2017-02-10\""}
;; <=

;; **
;;; ### Initialise portfolio （Go back here everytime you want to restart.）
;; **

;; @@
;; initialise with current date and initial capital (= $10000)
(init-portfolio "1981-12-15" 1000);
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1981-12-15 Cash: $1000&quot;</span>","value":"\"Date: 1981-12-15 Cash: $1000\""}
;; <=

;; **
;;; ### Rate of Change (ROC) strategy
;;; 
;;; ROC = (Closing price - Closing n periods ago) / Closing n periods ago * 100
;;; 
;;; We'll trade according to the following rules:
;;; - **Buy signal**: when ROC crosses the lower threshold (e.g. -30)
;;; - **Sell signal**: when ROC crosses the upper threshold (e.g. +30
;; **

;; @@
(def n 10)
(def lower-threshold -30)
(def upper-threshold 30)
    
(while (> (compare (get-date) "1981-12-31") 0)
  (print "loop ran")

  (let [roc (ROC "14593" 10)]
    (if (< roc lower-threshold)
      (order "14593" 1 :print false)) ; buy signal
    (if (> roc upper-threshold)
      (order "14593" -1 :print false))) ; sell signal
  (update-eval-report)
  (next-date))
(end-order)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(print-order-record 10)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
