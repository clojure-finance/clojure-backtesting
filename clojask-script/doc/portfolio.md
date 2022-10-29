{:title "Portfolio"
:date "2021-01-21"
:layout :post
:tags []
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


**`ns: clojure-backtesting.portfolio`**

This namespace features functions associated with manipulating and viewing the portfolio.


---
## Portfolio Initialisation

### `init_portfolio`

This function initialises the portfolio with cash and a date. Note that is is a **must** to call the function before executing functions e.g. `available-tics` and those in the `counter` namespace.

**Parameters:**

- `date` - the starting date of the portfolio, in format "YYYY-MM-DD"
- `init-capital` - the desired initial capital for the portfolio (non-negative integer)

**Example**:

```clojure
(init_portfolio "1980-12-1" 1000000)

;; output:
null
```

---

## Portfolio Inspection

### `view_portfolio`

This function prints the portfolio in a table format.

**Parameters:**

- none

**Ouput explanation:**

| &nbsp;**Column**&emsp; | &nbsp;**Format** &emsp; | &nbsp;**Meaning**                            |
| ------------------ | :-----------------: | :--------------------------------------- |
| `asset`            |    &nbsp;string     | Cash or ticker of the stock        |
| `price`            |   &nbsp;float, $    | Price of the stock &emsp;          |
| `aprc`             |   &nbsp;float, %    | Adjusted price of the stock &emsp; |
| `quantity`         | &nbsp;int or float  | Quantity of the stock owned &emsp; |
| `tot_val`          |    &nbsp;int, $     | Total value of the stock &emsp;    |

<br>

**Example**:

```clojure
(view_portfolio)

;; output:
| :asset |  :price | :aprc | :quantity | :tot_val |
|--------+---------+-------+-----------+----------|
|   cash |     N/A |   N/A |       N/A |    10295 |
|   AAPL |   34.19 | 29.42 |         0 |        0 |
```

<br>

### `view_portfolio_record`

This function prints the historical values and daily returns of the portfolio in a table format.

**Parameters:**

- `n` - no. of rows to print, if n <= 0, print entire record

**Ouput explanation:**

| &nbsp;**Column**&emsp; | &nbsp;**Format** &emsp; | &nbsp;**Meaning**                                                    |
| ------------------ | :-----------------: | :--------------------------------------------------------------- |
| `date`             |  &nbsp;YYYY-MM-DD   | Date of record                                             |
| `tot_value`        |    &nbsp;int, $     | Total value of the portfolio &emsp;                        |
| `daily_ret`        |   &nbsp;float, %    | Daily return of the portfolio &emsp;                       |
| `tot_ret`          |   &nbsp;float, %    | Total return of the portfolio &emsp;                       |
| `loan`             |    &nbsp;int, $     | Amount of loan made (cumulative) &emsp;                    |
| `leverage`         |   &nbsp;float, %    | Leverage ratio given by (total debt / total equity) &emsp; |
| `margin`           |   &nbsp;float, %    | Portfolio margin given by (cash / total value of securities), > 0 if there is leverage &emsp;|

<br>

**Example**:

```clojure
(view_portfolio_record -1)

;; output:
|      :date | :tot-value | :daily-ret | :tot-ret |  :loan | :leverage | :margin |
|------------+------------+------------+----------+--------+-----------+---------|
| 1980-12-15 |    $210.00 |      0.00% |    0.00% |  $0.00 |      0.00 |   0.00% |
| 1980-12-16 |    $210.00 |      0.00% |    0.00% | $48.19 |      0.23 |  81.34% |
| 1980-12-17 |    $212.75 |      0.13% |    0.13% | $48.19 |      0.23 |  81.53% |
| 1980-12-18 |    $216.00 |      0.15% |    0.27% | $48.19 |      0.22 |  81.76% |
| 1980-12-19 |    $222.87 |      0.29% |    0.57% | $48.19 |      0.22 |  82.22% |
| 1980-12-22 |    $228.51 |      0.23% |    0.80% | $48.19 |      0.21 |  82.58% |
| 1980-12-23 |    $233.51 |      0.19% |    0.99% | $48.19 |      0.21 |  82.89% |
| 1980-12-24 |    $239.84 |      0.23% |    1.23% | $48.19 |      0.20 |  83.27% |
| 1980-12-26 |    $251.08 |      0.38% |    1.61% | $48.19 |      0.19 |  83.90% |
```

```clojure
(view_portfolio_record 3)

;; output:
|      :date | :tot-value | :daily-ret | :tot-ret |  :loan | :leverage | :margin |
|------------+------------+------------+----------+--------+-----------+---------|
| 1980-12-15 |    $210.00 |      0.00% |    0.00% |  $0.00 |      0.00 |   0.00% |
| 1980-12-16 |    $210.00 |      0.00% |    0.00% | $48.19 |      0.23 |  81.34% |
| 1980-12-17 |    $212.75 |      0.13% |    0.13% | $48.19 |      0.23 |  81.53% |
```
