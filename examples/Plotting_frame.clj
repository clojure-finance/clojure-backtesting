;; gorilla-repl.fileformat = 1

;; **
;;; ## Plotting Frame
;;; 
;;; `plot` draws a line chart from a sequence of maps: one line per value of the series key, a date key on the x axis, and one value key (or two on independent axes). Charts open in the browser.
;; **

;; @@
(ns clojure-backtesting.examples.plotting
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
            [clojure-backtesting.direct :refer :all]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(load-dataset "resources/sample-data/main" "main" add-aprc)
(init-portfolio "1990-03-29" 10000)
;; @@
;; ->
;;; The dataset is already furnished by add-aprc. No more modification is needed.
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1990-03-29 Cash: $10000&quot;</span>","value":"\"Date: 1990-03-29 Cash: $10000\""}
;; <=

;; **
;;; ### Price history of one security
;;; 
;;; `get-permno-prev-n-days` returns the previous rows of a security, most recent first. Add a simple daily return and a ticker for the legend.
;; **

;; @@
(defn with-returns [permno]
  (let [rows (reverse (get-permno-prev-n-days permno 40))
        ticker (first (permno-tic permno))]
    (map (fn [prev row]
           {:date (:date row)
            :tic ticker
            :price (:PRC row)
            :return (/ (- (:PRC row) (:PRC prev)) (:PRC prev))})
         rows (rest rows))))

(def aaa (with-returns "10001"))
(first aaa)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>{:date &quot;1990-02-02&quot;, :tic &quot;AAA&quot;, :price 47.64, :return -2.0986358866732446E-4}</span>","value":"{:date \"1990-02-02\", :tic \"AAA\", :price 47.64, :return -2.0986358866732446E-4}"}
;; <=

;; @@
(plot aaa :tic :date :return true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; @@
(plot aaa :tic :date :price true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; **
;;; ### Several securities
;; **

;; @@
(def bbb (with-returns "10002"))
(def ccc (with-returns "10003"))
(plot (concat aaa bbb ccc) :tic :date :return true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; @@
(plot (concat aaa bbb ccc) :tic :date :price true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=

;; **
;;; ### Two variables on independent axes
;; **

;; @@
(plot aaa :tic :date :price :return true)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;The chart opens in the browser when this cell runs in the Gorilla REPL.&quot;</span>","value":"\"The chart opens in the browser when this cell runs in the Gorilla REPL.\""}
;; <=
