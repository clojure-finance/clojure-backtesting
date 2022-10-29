{:title "Counter"
 :date "2021-01-24"
 :layout :post
 :tags  []
 :toc true}


<style>
/* table styles */
table, th, td {
  border: 1px solid black;
  padding: 5px;
}
td {
  padding: 10px;
}
</style>

<br>

**`ns: clojure-backtesting.counter`**

This namespace features functions for getting basic configurations (date, list of available tickers) in the backtester.

---

## Date Configuration

**Important note:** *Only dates that have one or more entries in the dataset will be treated as valid dates.*


<br>

### `get-date`

Return the current date that the system is configured at.

**Parameters:**
- none

**Example:**

```
(get-date)

;; output:
"2020-12-10"
```

<br>

### `next-date`

Increment the current date to the nearest next valid date in the system.

**Parameters:**
- none

**Example:**

```
(next-date)

;; output:
"2020-12-11"
```

<br>

### `look-ahead-i-days`

Return the date i days after the given date

**Parameters**: 

- `date` - String type: "yyyy-mm-dd"
- `n`: int >= 0

**Example:**

```
(look-ahead-n-days "2020-01-23" 3)

;; output:
"2020-01-26"
```

<br>

### `look-n-days-ago`

Return the date n days before the given date (opposite of `look-ahead-n-days`)

**Parameters**: 

- `date` - String type: "yyyy-mm-dd"
- `n`: int >= 0

**Example:**

```
(look-i-days-ago "2020-01-23" 3)

;; output:
"2020-01-20"
```

<br>

---

## Ticker Information


<br>

### `tics-info`

An atom that contains a vector of maps. It stores the basic infomation needed for all the tickers available in the dataset.

**Explanation:**

| &nbsp;**Data**&emsp;| &nbsp;**Type**| &nbsp;**Meaning**  |
| ------------ | :-----------: | :----------|
| `:AAPL`      | &nbsp;keyword | Ticker name         |
| `:start-date`   | &nbsp;string | Earliest date of the ticker &emsp; |
| `:end-date`     | &nbsp;string | Latest date of the ticker &emsp; |
| `:pointer`    | &nbsp;atom of map   | Address of the ticker's row information; all pointers are initially pointing to the first line of the ticker &emsp; |
| `:num`        | &nbsp;int | Row number &emsp; |
| `:reference`        | &nbsp;lazy sequence | Lazy sequence of dateset starting at the pointed line &emsp; |

<br>

**Example:**

```clojure
(deref tics-info)

;; output:
{"AAPL" {:start-date "1980-12-15" :end-date "2000-01-12" :pointer (atom {:num 2934 :reference <lazy-seq>})} ... }
```


<br>

### `available-tics`

Print out a list of tickers that is available on the "current date" (refer to `get-date`). 

**Example:**

```clojure
(deref available-tics)

;; output:
{"AAPL" {:num 2934 :reference <lazy seq>} ... }
```

<br>

**Possible usages:**

1. *Get the list of tickers available on current date.*

    ```clojure
    (keys (deref available-tics))

    ;; output:
    ("AAPL" "F" "IBM")
    ```

<br>

2. *Get the reference of ticker "AAPL"*

    ```clojure
    (get (get (dref available-tics) :AAPL) :reference)

    ;; output:
    ({:ewretd "-0.03238", :HSICCD "7379", :CUM-RET 0.7749812652956042, :CFACSHR "4", :date "1981-01-07", :INIT-PRICE 350.0, :OPENPRC "", :SECSTAT "R", :SHROUT "583.807", :TICKER "IBM", :APRC 759.6930117568751, :COMNAM "INTERNATIONAL BUSINESS MACHS COR"...}...)
    ```

