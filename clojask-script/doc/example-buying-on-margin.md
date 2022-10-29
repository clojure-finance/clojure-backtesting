{:title "Example - Buying on Margin"
:date "2020-12-29"
:layout :post
:tags  []
:toc true}
 
<br>

**`Examples/Buying on margin.ipynb`**

This file features the demo code for enabling or disabling leverage in the backtester.

---

## Initialisation

```clojure
; import libraries from kernel
(ns clojure-backtesting.demo
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.specs :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.large-data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojupyter.kernel.version :as ver]
            [clojupyter.misc.helper :as helper]
  ) ;; require all libriaries from core
  (:use clojure.pprint)
)

; import dataset
(reset! data-set (add-aprc (read-csv-row "../resources/CRSP-extract.csv")));

```

<br>

## Trade without Leverage

The trade would be allowed if you possess enough cash to pay.

**Example:**

```clojure
(init-portfolio "1980-12-15" 400)
(order "AAPL" 10 :leverage false :remaining true :print true) ;without leverage, remaining value

(next-date)
(next-date)
(next-date)

(pprint/print-table (deref order-record))
(view-portfolio)
(view-portfolio-record -1)
```

```clojure
;; output:

Order: 1980-12-16 | AAPL | 10.000000.

|      :date | :tic |  :price | :aprc | :quantity |
|------------+------+---------+-------+-----------|
| 1980-12-16 | AAPL | 25.3125 | 25.82 |        10 |

| :asset |  :price |   :aprc | :quantity | :tot-val |
|--------+---------+---------+-----------+----------|
|   cash |     N/A |     N/A |       N/A |   141.81 |
|   AAPL | 26.6875 | 26.4187 |        10 |   264.19 |

|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
|------------+------------+------------+----------+-------+-----------+---------|
| 1980-12-15 |    $400.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
| 1980-12-16 |    $400.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
| 1980-12-17 |    $402.75 |      0.30% |    0.30% | $0.00 |      0.00 |   0.00% |
| 1980-12-18 |    $406.00 |      0.35% |    0.65% | $0.00 |      0.00 |   0.00% |
```
<br>

However, the trade would not be allowed if you do not have sufficient cash.

**Example:**

```clojure
(init-portfolio "1980-12-15" 100)
(order "AAPL" 10 :leverage false :remaining true :print true) ;without leverage, remaining value

(next-date)
(next-date)
(next-date)

(pprint/print-table (deref order-record))
(view-portfolio)
(view-portfolio-record -1)
```
```clojure
;; output:

Order request 1980-12-15 | AAPL | 10 fails.
Failure reason: You do not have enough money to buy or have enough stock to sell. Try to solve by enabling leverage.

| :asset | :price | :aprc | :quantity | :tot-val |
|--------+--------+-------+-----------+----------|
|   cash |    N/A |   N/A |       N/A |   100.00 |

|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
|------------+------------+------------+----------+-------+-----------+---------|
| 1980-12-15 |    $100.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
```

<br>

## Trade with Leverage

**Example:**

```clojure
(init-portfolio "1980-12-15" 20)

(order "AAPL" 1 :print true) ;with leverage, exact value trade

(next-date)
(next-date)
(next-date)

(pprint/print-table (deref order-record))
(view-portfolio)
(view-portfolio-record -1)
```
```clojure
;; output:

Order: 1980-12-16 | AAPL | 1.000000.

|      :date | :tic |  :price | :aprc | :quantity |
|------------+------+---------+-------+-----------|
| 1980-12-16 | AAPL | 25.3125 | 25.82 |         1 |

| :asset |  :price |   :aprc | :quantity | :tot-val |
|--------+---------+---------+-----------+----------|
|   cash |     N/A |     N/A |       N/A |    -5.82 |
|   AAPL | 26.6875 | 26.4187 |         1 |    26.42 |

|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
|------------+------------+------------+----------+-------+-----------+---------|
| 1980-12-15 |     $20.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
| 1980-12-16 |     $20.00 |      0.00% |    0.00% | $5.82 |      0.29 |  77.46% |
| 1980-12-17 |     $20.27 |      0.17% |    0.17% | $5.82 |      0.29 |  77.70% |
| 1980-12-18 |     $20.60 |      0.20% |    0.37% | $5.82 |      0.28 |  77.98% |
```

<br>

## Initial Margin

The purchase would not be allowed if the ratio of cash to total value of assets bought on margin goes below the initial margin.

```clojure
; check variable
(println INITIAL-MARGIN)
```

<br>

**Example:** order failed, since cash is insufficient.

```clojure
(init-portfolio "1980-12-15" 100)
(order "AAPL" 10 :remaining true :print true) ;with leverage, remaining value

(next-date)
(next-date)
(next-date)

(pprint/print-table (deref order-record))
(view-portfolio)
(view-portfolio-record -1)
```
```
;; output:

Order request 1980-12-15 | AAPL | 10 fails due to initial margin exceeding.

| :asset | :price | :aprc | :quantity | :tot-val |
|--------+--------+-------+-----------+----------|
|   cash |    N/A |   N/A |       N/A |   100.00 |

|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage | :margin |
|------------+------------+------------+----------+-------+-----------+---------|
| 1980-12-15 |    $100.00 |      0.00% |    0.00% | $0.00 |      0.00 |   0.00% |
```

<br>

Alternatively, you could manually **update the initial margin** to enable such a case. You could set it to nil in order to disable the initial margin requirement.

```clojure
(update-initial-margin 0.1)
(println INITIAL-MARGIN)

;; output:
0.1
```

<br>

**Example:** with initial margin udpated to 0.1

```clojure
(init-portfolio "1980-12-15" 100)
(order "AAPL" 10 :remaining true :print true) ;with leverage, remaining value

(next-date)
(next-date)
(next-date)

(pprint/print-table (deref order-record))
(view-portfolio)
(view-portfolio-record -1)
```
```clojure
;; output:

Order: 1980-12-16 | AAPL | 10.000000.

|      :date | :tic |  :price | :aprc | :quantity |
|------------+------+---------+-------+-----------|
| 1980-12-16 | AAPL | 25.3125 | 25.82 |        10 |

| :asset |  :price |   :aprc | :quantity | :tot-val |
|--------+---------+---------+-----------+----------|
|   cash |     N/A |     N/A |       N/A |  -158.19 |
|   AAPL | 26.6875 | 26.4187 |        10 |   264.19 |

|      :date | :tot-value | :daily-ret | :tot-ret |   :loan | :leverage | :margin |
|------------+------------+------------+----------+---------+-----------+---------|
| 1980-12-15 |    $100.00 |      0.00% |    0.00% |   $0.00 |      0.00 |   0.00% |
| 1980-12-16 |    $100.00 |      0.00% |    0.00% | $158.19 |      1.58 |  38.73% |
| 1980-12-17 |    $102.75 |      1.81% |    1.81% | $158.19 |      1.54 |  39.38% |
| 1980-12-18 |    $106.00 |      2.02% |    3.83% | $158.19 |      1.49 |  40.12% |
```

<br>

<br>

## Maintenance Margin

All positions will be automatically closed if the portfolio margin goes below the maintenace margin.

<br>

**Example:**

```clojure
(init-portfolio "1980-12-15" 100)
(order "AAPL" -10 :remaining true :print true) ;with leverage, remaining value
(order "F" 20 :remaining true :print true)

(next-date)
(next-date)
(next-date)

(pprint/print-table (deref order-record))
(view-portfolio)
(view-portfolio-record -1)
```
```clojure
;; output:

Order: 1980-12-16 | AAPL | -10.000000.
Order: 1980-12-16 | F | 20.000000.
1980-12-15: You have lost all cash. Closing all positions.
Please reset the dataset and call init-portfolio again.

|      :date | :tic |  :price |  :aprc | :quantity |
|------------+------+---------+--------+-----------|
| 1980-12-16 | AAPL | 25.3125 |  25.82 |       -10 |
| 1980-12-16 |    F |   18.75 | 100.64 |        20 |
| 1980-12-16 |    F |   18.75 | 100.64 |       -20 |

| :asset |  :price |    :aprc | :quantity | :tot-val |
|--------+---------+----------+-----------+----------|
|   cash |     N/A |      N/A |       N/A | -1654.57 |
|   AAPL | 25.3125 |  25.8187 |       -10 |  -258.19 |
|      F |   18.75 | 100.6380 |        20 |  2012.76 |

|      :date | :tot-value | :daily-ret | :tot-ret |    :loan | :leverage | :margin |
|------------+------------+------------+----------+----------+-----------+---------|
| 1980-12-15 |    $100.00 |      0.00% |    0.00% |    $0.00 |      0.00 |   0.00% |
| 1980-12-16 |    $100.00 |      0.00% |    0.00% | $1654.57 |     16.55 |   5.70% |
```
