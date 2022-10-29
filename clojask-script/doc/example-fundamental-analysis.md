{:title "Example - Fundamental Analysis"
:date "2020-12-30"
:layout :post
:tags  []
:toc true}
 
<br>

**`Examples/Fundamental Analysis.ipynb`**

This file features the demo code for implementing a simple fundamental analysis strategy.

---

## Initialisation

```
; import libraries from kernel
(ns clojure-backtesting.demo
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            ;;[clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojupyter.kernel.version :as ver]
            [clojure.edn :as edn]
            [clojupyter.misc.helper :as helper]
  ) ;; require all libriaries from core
  (:use clojure.pprint)
)

; import dataset
(reset! data-set (add-aprc (read-csv-row "../resources/CRSP-extract.csv")));

```

## Compute Return on Equity (ROE)

```clojure
;; Use merged database CRSP + COMPUSTAT
;; Compute the latest quarter ROE by using: niq / (share price * cshoq) in the merged dataset
;; output (a list of maps, followed by a map with the list of ROEs)
;; {:data {:tic "AAPL" :year "1980" :ROE 0.02}{...} :ROE [0.02 0.53 ...]}

(defn get-set-roe
    "return a set of maps of tickers and datadate" ;;{:tic "AAPL", :datadate "1981/3/31", :ROE x.x}
    [file date]
    (loop [remaining file
            result-set []
            ROE-list [] ]
        (if (empty? remaining)
            ;; how to change the output here?
            ;;(into #{} (conj result-set {:ROE-set ROE-list}))
            {:data result-set :ROE-set ROE-list}
            (let [first-line (first remaining)
                next-remaining (rest remaining)
                ;;next-result-set (conj result-set (get first-line :datadate)
                [year month day] (map parse-int (str/split date #"-"))]
                ;;merged file use :datadate as key
                (if (= date (get first-line :datadate))
                    (let [[niq PRC cshoq] (map edn/read-string [(get first-line :niq)(get first-line :PRC)(get first-line :cshoq)])
                          ROE (/ niq (* PRC cshoq))]
                        (recur next-remaining (conj result-set {:tic (get first-line :tic) :year year :ROE ROE}) (conj ROE-list ROE))
                    )
                    (recur next-remaining result-set ROE-list)
                )  
            )
        )
    )
)
```
<br>

Making use of the above function, we write another function to only retrieve the set of ROE from the output of `get-set-roe`.

```clojure
(defn get-ROE
            "return a set of ROE"  ;;{10.2 1.8 x.x ...}
        [dataset]
        (into #{} (get dataset :ROE-set))
        )
```

## Get List of Tickers to Buy

Suppose that we want to get the list of tickers with ROE >= 20th percentile. It could be done with the following steps.

```clojure
;; load data
(def data (get-set-roe (read-csv-row "../resources/data-testing-merged.csv") "1980-12-18"))

;; get the set of ROE
(def roe-list (get-ROE data))

;; get a sorted set of ROE, using the bulit-in "sorted-set" function
(def roe-sorted-set (apply sorted-set roe-list))

;; cast the sorted set into a vector
(def roe-sorted-vec (into '[] roe-sorted-set))

;; find the 20th percentile ROE
(def roe-20 (nth roe-sorted-vec (int (* 0.8 (count roe-sorted-vec)))))
```

With the 20th percentile ROE obtained, we can write a function to get only tickers with ROE >= this value (i.e. `roe-20`).

```clojure
(defn get-roe-20
    "return a set tickers" ;;{"AAPL" "GM"}
    [data roe-20]
    (loop [remaining (get data :data)
            result-set []]
        (if (empty? remaining)
            (into #{} result-set)
            (let [first-line (first remaining)
                next-remaining (rest remaining)]
                ;;conj the stock ticker whose ROE >= roe-20
                ;;(if (not= -1 (compare roe-20 (get first-line :ROE)))
                (if (>= (get first-line :ROE) roe-20)
                    (recur next-remaining (conj result-set (get first-line :tic)))           
                    (recur next-remaining result-set)            
                )  
            )
        )
    )
)
```

Eventually, we get the list of tickers that we want:

```clojure
;; get the list of tickers to buy
(def stock-to-buy-test (get-roe-20 data roe-20)
)
(println stock-to-buy-test)
;; output: #{IBM}
```

<br>

## Writing the Strategy

Given all the essential functions, we could write the loop for making orders based on the ROE of companies on a certain date.

You could read the **Code Walkthrough** in the *"Get Started"* section if you are unfamiliar with the date configuration or update logic in the backtester.

<br>


```clojure
(init-portfolio "1980-12-18" 100000)

(def year-count 3) ;; hold the stock for 3 years
(def start-year 1980)
(def rebalance-md (subs (get-date) 4)) ; = -12-18

(def rebalance-years (into [] (range (+ start-year 1) (+ (+ start-year 1) year-count) 1))) ; rebalance every year

(def rebalance-dates []) ; [1981-12-18, 1982-12-18, 1983-12-18]
(doseq [year rebalance-years]
  (def rebalance-dates (conj rebalance-dates (str year rebalance-md)))
)

(def end-date (last rebalance-dates)) ; 1983-12-18

;; get stock tickers and ROE data
(def stock-data (get-set-roe (read-csv-row "../resources/data-testing-merged.csv") "1980-12-18"))

;; get a set of ROE
(def roe-list (get-ROE stock-data))

;; sort the set
(def roe-sorted (into '[] (apply sorted-set roe-list)))

;; find the 20% cut-off ROE value
(let [roe-20 (nth roe-sorted (int (* 0.8 (count roe-sorted))))]    
    ;;get the tickers of the top 20 with function get-roe-20
    (def stocks-to-buy (get-roe-20 stock-data roe-20))
)

;; buy only the top 20% stocks    
(def stocks-to-buy-list (into [] stocks-to-buy))

;; buy stocks 
(doseq [stock stocks-to-buy-list]        
  (order stock 10)
)

(update-eval-report (get-date)) ; update evaluation metrics
(next-date)

(while (not= (empty? rebalance-dates) true)
  ;(println (get-date)) ; debug
  (if (t/after? (t/local-date (get-date)) (t/local-date (first rebalance-dates))) ; check if (get-date) has passed first date in rebalance-dates
    (do
      (def rebalance-dates (rest rebalance-dates)) ; pop the first date in rebalance-dates
      ;(println (rest rebalance-dates)) ; debug
      (while (empty? (get-set-roe (read-csv-row "../resources/data-testing-merged.csv") (get-date)))
          (next-date) ;; move on to next date til data is not empty
      )
      (def stock-data (get-set-roe (read-csv-row "../resources/data-testing-merged.csv") (get-date)))
      (def roe-list (get-ROE stock-data))
      (def roe-sorted (into '[] (apply sorted-set roe-list)))
      (let [roe-20 (nth roe-sorted (int (* 0.8 (count roe-sorted))))]    
          (def stocks-to-buy (get-roe-20 stock-data roe-20))
      )
      (def stocks-to-buy-list (into [] stocks-to-buy))

      ;; sell stocks held in portfolio
      (doseq [[ticker row] (deref portfolio)]
        (if (not= ticker :cash)      
          (order ticker -10)
        )
      )

      ;; buy stocks
      (doseq [stock stocks-to-buy-list]       
        (order stock 10)
      )

      (update-eval-report (get-date)) ; update evaluation metrics
    )
  )
  (next-date) ; move on to the next trading day
)

;; sell stocks held in portfolio (if ticker != "cash" && quantity > 0)
(doseq [[ticker row] (deref portfolio)]
  (if (and (not= ticker :cash) (= (compare (get row :quantity) 0) 1))
      (order ticker -10)
  )
)
```

## Check Records & Performance

```clojure
(pprint/print-table (deref order-record))

;; output:
|      :date | :tic | :price |   :aprc | :quantity |
|------------+------+--------+---------+-----------|
| 1980-12-19 |  IBM | 64.625 |  736.07 |        10 |
| 1981-12-22 |  IBM | 56.875 |  714.58 |       -10 |
| 1981-12-22 |  IBM | 56.875 |  714.58 |        10 |
| 1982-12-21 |  IBM |  95.75 |  915.74 |       -10 |
| 1982-12-21 |  IBM |  95.75 |  915.74 |        10 |
| 1983-12-20 |  IBM | 121.75 | 1030.91 |       -10 |
```

```clojure
;; view final portfolio
(view-portfolio)

;; output:
| :asset | :price |   :aprc | :quantity | :tot-val |
|--------+--------+---------+-----------+----------|
|   cash |    N/A |     N/A |       N/A |   102948 |
|    IBM | 121.75 | 1030.91 |         0 |        0 |
```

```clojure
;; view portfolio value and return
(view-portfolio-record)

;; output:
|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage |
|------------+------------+------------+----------+-------+-----------|
| 1980-12-18 |    $100000 |      0.00% |    0.00% | $0.00 |     0.00% |
| 1980-12-19 |    $100000 |      0.00% |    0.00% | $0.00 |     0.00% |
| 1980-12-22 |    $100122 |      0.12% |    0.12% | $0.00 |     0.00% |
| 1980-12-23 |    $100128 |      0.01% |    0.13% | $0.00 |     0.00% |
| 1980-12-24 |    $100188 |      0.06% |    0.19% | $0.00 |     0.00% |
| 1980-12-26 |    $100212 |      0.02% |    0.21% | $0.00 |     0.00% |
...(to be continued)
```

```clojure
(eval-report)

;; output:
|      :date | :pnl-pt | :sharpe | :tot-val |  :vol |
|------------+---------+---------+----------+-------|
| 1980-12-18 |      $0 |   0.00% |  $100000 | 0.00% |
| 1981-12-21 |    $-71 |  -5.04% |   $99785 | 0.04% |
| 1982-12-20 |    $359 |  36.79% |  $101796 | 0.05% |
| 1983-12-19 |    $491 |  57.40% |  $102948 | 0.05% |
```