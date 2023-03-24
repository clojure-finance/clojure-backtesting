;; gorilla-repl.fileformat = 1

;; **
;;; ## Golden Cross Example	
;; **

;; @@
; import libraries from kernel
(ns clojure-backtesting.updated_examples.goldencross
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            ;[clojure-backtesting.plot :refer :all] - OZ BROKEN (22/03/23)
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [clojure-backtesting.direct :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [gorilla-plot.core :as plot :refer :all]
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
;; initialise with current date and initial capital (= $1000)
(init-portfolio "1981-12-15" 1000);
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;Date: 1981-12-15 Cash: $1000&quot;</span>","value":"\"Date: 1981-12-15 Cash: $1000\""}
;; <=

;; @@
(get-date)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-string'>&quot;1981-12-15&quot;</span>","value":"\"1981-12-15\""}
;; <=

;; **
;;; ### Write a strategy
;;; 
;;; The following code implements a trading strategy called Golden Rule:
;;; 
;;; MA 15 cross above the MA 30 (golden cross)
;;; 
;;; MA 15 cross below the MA 30 (death cross)
;;; 
;;; So in the codes, MA15 and MA30 are compared on a daily basis, if golden cross occurs, then you set a buy order; if death cross occurs, then you set a sell order first 
;;; 
;;; 
;; **

;; **
;;; Should increase the cache size first to reduce repeatitive File IO
;; **

;; @@
(CHANGE-CACHE-SIZE 30)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.parameters/CACHE-SIZE</span>","value":"#'clojure-backtesting.parameters/CACHE-SIZE"}
;; <=

;; @@
(while (< (compare (get-date) "1981-12-24") 0)
  ;; run for about a week
  (do
    (let [[MA15 MA30] [(moving-avg "14593" 15) (moving-avg "14593" 30)]]
      (if (> MA15 MA30)
        (order "14593" 0.1 :print false)
        (order "14593" 0 :remaining true))))
  (let [[MA15 MA30] [(moving-avg "25785" 15) (moving-avg "25785" 30)]]
    (if (> MA15 MA30)
      (order "25785" 1  :print false)
      (order "25785" 0 :remaining true)))
  (update-eval-report)
  (next-date))
(end-order)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>true</span>","value":"true"}
;; <=

;; @@
;(pprint/print-table (take 200 (deref portfolio-value)))
(print-order-record 10)
;; @@
;; ->
;;; 
;;; |      :date | :permno |  :price | :aprc | :quantity |
;;; |------------+---------+---------+-------+-----------|
;;; | 1981-12-16 |   14593 | 19.5625 | 22.43 |       0.1 |
;;; | 1981-12-16 |   25785 |   17.25 | 55.94 |         1 |
;;; | 1981-12-17 |   14593 | 21.1875 | 23.22 |       0.1 |
;;; | 1981-12-17 |   25785 |  17.625 | 56.46 |         1 |
;;; | 1981-12-18 |   14593 | 22.9375 | 24.03 |       0.1 |
;;; | 1981-12-18 |   25785 |    18.0 | 56.98 |         1 |
;;; | 1981-12-21 |   14593 | 21.9375 | 23.57 |       0.1 |
;;; | 1981-12-21 |   25785 |    18.0 | 56.98 |         1 |
;;; | 1981-12-22 |   14593 | 22.3125 | 23.75 |       0.1 |
;;; | 1981-12-22 |   25785 |    17.5 | 56.29 |         1 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Check portfolio record
;; **

;; @@
;; view final portfolio
(print-portfolio)
;; @@
;; ->
;;; 
;;; | :asset | :price | :aprc | :quantity | :tot-val |
;;; |--------+--------+-------+-----------+----------|
;;; |   cash |    N/A |   N/A |       N/A |   996.42 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
;; view portfolio value and return
(print-portfolio-record 10)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
;;; |------------+------------+------------+----------+-------+-----------+---------|
;;; | 1981-12-15 |   $1000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-16 |   $1000.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-17 |   $1000.60 |     -0.00% |    0.03% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-18 |   $1001.80 |      0.00% |    0.08% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-21 |   $1001.67 |      0.00% |    0.07% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-22 |    $998.96 |     -0.00% |   -0.05% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-23 |    $995.33 |      0.00% |   -0.20% | $0.00 |      0.00 |   0.00% |
;;; | 1981-12-24 |    $996.42 |      0.00% |   -0.16% | $0.00 |      0.00 |   0.00% |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Generate evaluation report
;; **

;; @@
(print-eval-report 10)
;; @@
;; ->
;;; 
;;; |      :date | :tot-value |    :vol |  :r-vol |               :sharpe |             :r-sharpe | :pnl-pt | :max-drawdown |
;;; |------------+------------+---------+---------+-----------------------+-----------------------+---------+---------------|
;;; | 1981-12-16 |      $1000 | 0.0000% | 0.0000% |               0.0000% |               0.0000% |      $0 |        0.0000 |
;;; | 1981-12-17 |      $1000 | 0.0000% | 0.0000% |   9419676466259.1450% |   9419676466259.1450% |      $0 |        0.0000 |
;;; | 1981-12-18 |      $1001 | 0.0000% | 0.0000% |  32469602626935.1760% |  32469602626935.1760% |      $0 |        0.0000 |
;;; | 1981-12-21 |      $1001 | 0.0000% | 0.0000% |  33522685497906.6330% |  33522685497906.6330% |      $0 |        0.0000 |
;;; | 1981-12-22 |       $998 | 0.0000% | 0.0000% | -18079259030964.3000% | -18079259030964.3000% |      $0 |        0.0000 |
;;; | 1981-12-23 |       $995 | 0.0000% | 0.0000% | -86461715180573.6600% | -86461715180573.6600% |      $0 |        0.0000 |
;;; | 1981-12-24 |       $996 | 0.0000% | 0.0000% | -69689219714318.2400% | -69689219714318.2400% |      $0 |        0.0000 |
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; ### Plot variables
;; **

;; @@
(def data (deref portfolio-value))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.goldencross/data</span>","value":"#'clojure-backtesting.updated_examples.goldencross/data"}
;; <=

;; @@
; Add legend name to series
(def data-to-plot
 (map #(assoc % :plot "portfolio")
  data))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;clojure-backtesting.updated_examples.goldencross/data-to-plot</span>","value":"#'clojure-backtesting.updated_examples.goldencross/data-to-plot"}
;; <=

;; @@
(first data-to-plot)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:date</span>","value":":date"},{"type":"html","content":"<span class='clj-string'>&quot;1981-12-15&quot;</span>","value":"\"1981-12-15\""}],"value":"[:date \"1981-12-15\"]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-value</span>","value":":tot-value"},{"type":"html","content":"<span class='clj-long'>1000</span>","value":"1000"}],"value":"[:tot-value 1000]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:daily-ret</span>","value":":daily-ret"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:daily-ret 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:tot-ret</span>","value":":tot-ret"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:tot-ret 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:loan</span>","value":":loan"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:loan 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:leverage</span>","value":":leverage"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:leverage 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:margin</span>","value":":margin"},{"type":"html","content":"<span class='clj-double'>0.0</span>","value":"0.0"}],"value":"[:margin 0.0]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-keyword'>:plot</span>","value":":plot"},{"type":"html","content":"<span class='clj-string'>&quot;portfolio&quot;</span>","value":"\"portfolio\""}],"value":"[:plot \"portfolio\"]"}],"value":"{:date \"1981-12-15\", :tot-value 1000, :daily-ret 0.0, :tot-ret 0.0, :loan 0.0, :leverage 0.0, :margin 0.0, :plot \"portfolio\"}"}
;; <=

;; @@
; ORIGINAL
;;(plot data-to-plot :plot :date :daily-ret true)
;; @@

;; @@
; Bootstrapped plotting using gorilla-plot
(def plotting-data (map #(% :daily-ret) data-to-plot))
(println plotting-data)
;; @@
;; ->
;;; (0.0 0.0 -4.821637332766435E-17 0.0 0.0 -4.821637332766435E-17 0.0 0.0)
;;; 
;; <-
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; @@
(list-plot plotting-data :joined true)
;; @@
;; =>
;;; {"type":"vega","content":{"width":400,"height":247.2188,"padding":{"top":10,"left":55,"bottom":40,"right":10},"data":[{"name":"e7e6751d-5e27-482c-981b-041bbae4c8aa","values":[{"x":0,"y":0.0},{"x":1,"y":0.0},{"x":2,"y":-4.821637332766435E-17},{"x":3,"y":0.0},{"x":4,"y":0.0},{"x":5,"y":-4.821637332766435E-17},{"x":6,"y":0.0},{"x":7,"y":0.0}]}],"marks":[{"type":"line","from":{"data":"e7e6751d-5e27-482c-981b-041bbae4c8aa"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"stroke":{"value":"#FF29D2"},"strokeWidth":{"value":2},"strokeOpacity":{"value":1}}}}],"scales":[{"name":"x","type":"linear","range":"width","zero":false,"domain":{"data":"e7e6751d-5e27-482c-981b-041bbae4c8aa","field":"data.x"}},{"name":"y","type":"linear","range":"height","nice":true,"zero":false,"domain":{"data":"e7e6751d-5e27-482c-981b-041bbae4c8aa","field":"data.y"}}],"axes":[{"type":"x","scale":"x"},{"type":"y","scale":"y"}]},"value":"#gorilla_repl.vega.VegaView{:content {:width 400, :height 247.2188, :padding {:top 10, :left 55, :bottom 40, :right 10}, :data [{:name \"e7e6751d-5e27-482c-981b-041bbae4c8aa\", :values ({:x 0, :y 0.0} {:x 1, :y 0.0} {:x 2, :y -4.821637332766435E-17} {:x 3, :y 0.0} {:x 4, :y 0.0} {:x 5, :y -4.821637332766435E-17} {:x 6, :y 0.0} {:x 7, :y 0.0})}], :marks [{:type \"line\", :from {:data \"e7e6751d-5e27-482c-981b-041bbae4c8aa\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :stroke {:value \"#FF29D2\"}, :strokeWidth {:value 2}, :strokeOpacity {:value 1}}}}], :scales [{:name \"x\", :type \"linear\", :range \"width\", :zero false, :domain {:data \"e7e6751d-5e27-482c-981b-041bbae4c8aa\", :field \"data.x\"}} {:name \"y\", :type \"linear\", :range \"height\", :nice true, :zero false, :domain {:data \"e7e6751d-5e27-482c-981b-041bbae4c8aa\", :field \"data.y\"}}], :axes [{:type \"x\", :scale \"x\"} {:type \"y\", :scale \"y\"}]}}"}
;; <=
