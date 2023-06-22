;; gorilla-repl.fileformat = 1

;; **
;;; # Plotting Frame
;; **

;; @@
(ns clojure-backtesting.examples.plottingFrame
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
            [clojure.edn :as edn]
            [java-time :as t]
  ) ;; require all libriaries from core
)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
; path to dataset = "/Volumes/T7/CRSP"
; change it to the relative path to your own dataset
;
(load-dataset "./Volumes/T7/CRSP" "main" add-aprc)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 1972-01-03 ~ 2017-02-10&quot;</span>","value":"\"Date range: 1972-01-03 ~ 2017-02-10\""}
;; <=

;; @@
; path to dataset = "/Volumes/T7/CRSP"
; change it to the relative path to your own dataset
;
(load-dataset "./Volumes/T7/Compustat" "compustat")
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date range: 1962-01-31 ~ 2017-12-31&quot;</span>","value":"\"Date range: 1962-01-31 ~ 2017-12-31\""}
;; <=

;; @@
(init-portfolio "2016-12-30" 100000)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 2016-12-30 Cash: $100000&quot;</span>","value":"\"Date: 2016-12-30 Cash: $100000\""}
;; <=

;; @@
(def data (get-permno-prev-n-days "14593" 100))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/data</span>","value":"#'clojure-backtesting.demo/data"}
;; <=

;; @@
(defn get-return [data tic]
  (loop [remaining (rest data)
         firstline (first data)
         result-set []]
    (if (empty? remaining)
      result-set
      (let [nextline (first remaining)
            next-remaining (rest remaining)
            PRC1 (get firstline :PRC)
            PRC2 (get nextline :PRC)
            return (/ (- PRC1 PRC2) PRC2)]
        (recur next-remaining nextline (conj result-set {:PRC (get firstline :PRC) :tic tic :DATE (get firstline :DATE) :RETURN return}))))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/get-return</span>","value":"#'clojure-backtesting.demo/get-return"}
;; <=

;; @@
(def singlestock (get-return data "AAPL"))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/singlestock</span>","value":"#'clojure-backtesting.demo/singlestock"}
;; <=

;; @@
(plot singlestock :tic :DATE :RETURN true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-string'>&quot;AAPL&quot;</span>","value":"\"AAPL\""},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"},{"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}],"value":"(\"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" \"AAPL\" nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil nil)"}
;; <=

;; @@
(def data2 (get-permno-prev-n-days (first (tic-permno "F")) 100))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/data2</span>","value":"#'clojure-backtesting.demo/data2"}
;; <=

;; @@
(def data2mul (get-return data2 "F"))
;; @@

;; @@
(plot data2mul :tic :DATE :RETURN true)
;; @@

;; @@
(def data3 (get-permno-prev-n-days (first (tic-permno "IBM")) 100))
;; @@

;; @@
(def data3mul (get-return data3 "IBM"))
;; @@

;; @@
(plot data3mul :tic :DATE :RETURN true)
;; @@

;; @@
(def data4 (get-permno-prev-n-days (first (tic-permno "TSLA")) 100))
;; @@

;; @@
(def data4mul (get-return data4 "TSLA"))
;; @@

;; @@
(plot data4mul :tic :DATE :RETURN true)
;; @@

;; @@
(plot (concat singlestock data4mul data2mul data3mul) :tic :DATE :RETURN true)
;; @@

;; @@
(plot (concat singlestock data4mul data2mul data3mul) :tic :DATE :PRC true)
;; @@

;; @@
(plot singlestock :tic :DATE :PRC true)
;; @@

;; @@
(plot singlestock :tic :DATE :PRC :RETURN true)
;; @@

;; @@
(plot  (concat singlestock data4mul data2mul data3mul) :tic :DATE :PRC :RETURN true)
;; @@

;; @@

;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.demo/multistocks</span>","value":"#'clojure-backtesting.demo/multistocks"}
;; <=

;; @@

;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@

;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(plot singlestock :tic :date :price :return true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(plot multistocks :tic :date :price :return true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=
