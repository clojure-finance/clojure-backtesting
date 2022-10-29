{:title "Example - Golden Cross"
 :date "2020-12-31"
 :layout :post
 :tags  []
 :toc true}
 
<br>

**`Examples/Golden cross.ipynb`**

This file features the demo code for implementing the Golden Cross strategy.

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

<br>

## Writing the Strategy

The following code implements a trading strategy called Golden Rule:

- MA 50 cross above the MA 200 (golden cross)

- MA 200 cross below the MA 50 (death cross)

So in the codes, MA50 and MA200 are compared on a daily basis, if golden cross occurs, then you set a buy order; if death cross occurs, then you set a sell order first.

```clojure
(time (do (def MA50-vec-aapl [])
          (def MA200-vec-aapl [])
          (def MA50-vec-f [])
          (def MA200-vec-f [])
          (while (not= (get-date) "2016-12-29")
            (do
    ;; write your trading strategy here
              (def tics (deref available-tics)) ;20 ms
              (def MA50-vec-aapl (get-prev-n-days :PRC 50 "AAPL" MA50-vec-aapl (get (get tics "AAPL"):reference)))
              (def MA200-vec-aapl (get-prev-n-days :PRC 200 "AAPL" MA200-vec-aapl (get (get tics "AAPL") :reference)))
              (def MA50-vec-f (get-prev-n-days :PRC 50 "F" MA50-vec-f (get (get tics "F"):reference)))
              (def MA200-vec-f (get-prev-n-days :PRC 200 "F" MA200-vec-f (get (get tics "F") :reference)))
              (let [[MA50 MA200] [(moving-average :PRC MA50-vec-aapl) (moving-average :PRC MA200-vec-aapl)]]
                (if (> MA50 MA200)
                  (order "AAPL" 1 :reference (get (get tics "AAPL") :reference) :print false) 
                  (order "AAPL" 0 :remaining true :reference (get (get tics "AAPL") :reference))))
              (let [[MA50 MA200] [(moving-average :PRC MA50-vec-f) (moving-average :PRC MA200-vec-f)]]
                (if (> MA50 MA200)
                  (order "F" 1 :reference (get (get tics "F") :reference) :print false) 
                  (order "F" 0 :remaining true :reference (get (get tics "F") :reference))))
              ;(update-eval-report (get-date))
              (next-date)))))
(.close wrtr)
```

<br>

## Check Number of Orders Made

```
(count (deref order-record))

;; output:
9502
```

<br>

## Check Records & Performance

```clojure
;; view final portfolio
(view-portfolio)

;; output:
| :asset | :price |  :aprc | :quantity | :tot-val |
|--------+--------+--------+-----------+----------|
|   cash |    N/A |    N/A |       N/A |   135759 |
|   AAPL | 116.73 | 312.11 |        81 |    25280 |
|      F |  14.87 | 605.11 |         0 |        0 |
```

