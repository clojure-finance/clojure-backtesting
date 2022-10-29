{:title "Evaluate"
:date "2021-01-20"
:layout :post
:tags []
:toc true}

<style>
/* table styles */
table, th, td {
  border: 1px solid black;
  padding: 5px;
}
</style>

<br>

**`ns: clojure-backtesting.evaluate`**

This namespace features functions for computing metrics to evaluate portfolio performance and printing the evaluation report.

---
## Calculating Evalaution Metrics

### `update-eval-report`

This function updates the evaluation metrics in the record. (see `eval-report` for the list of evaluation metrics computed)

**Parameters:**

- `date` - date to update the evaluation metrics, in format "YYYY-MM-DD"

**Example**:

```clojure
;; update the evaluation metrics today
(update-eval-report (get-date))

;; output:
null
```

---
## Inspection of Evaluation Report

### `eval-report`

This function prints the evaluation report that includes all summary statiscs in a table format.

**Parameters:**

- `n` - no. of rows to print, if n <= 0, print entire record

**Ouput explanation:**

| &nbsp;Column&emsp; | &nbsp;Format &emsp; | &nbsp;Meaning                                                                      |
| ------------------ | :-----------------: | :--------------------------------------------------------------------------------- |
| `date`             |  &nbsp;YYYY-MM-DD   | &nbsp;Date of record                                                               |
| `tot-val`          |    &nbsp;int, %     | &nbsp;Total value of the portfolio, including cash and all purchased stocks &emsp; |
| `vol`              |   &nbsp;float, %    | &nbsp;Volatility of the portfolio caculated with an expanding window &emsp;        |
| `r-vol`            |   &nbsp;float, %    | &nbsp;Volatility of the portfolio caculated with a rolling window &emsp;           |
| `sharpe`           |   &nbsp;float, %    | &nbsp;Sharpe ratio of the portfolio caculated with an expanding window &emsp;      |
| `r-sharpe`         |   &nbsp;float, %    | &nbsp;Sharpe ratio of the portfolio caculated with a rolling window &emsp;         |
| `pnl-pt`           |   &nbsp;float, $    | &nbsp;Profit and loss per trade &emsp;                                             |
| `max-drawdown`     |   &nbsp;float, $    | &nbsp;Maximum drawdown of the portfolio &emsp;                                     |




<br>

**Example**:

```clojure
(eval-report -1)

;; output:

|      :date | :tot-value |    :vol |  :r-vol |  :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
|------------+------------+---------+---------+----------+-----------+---------+---------------|
| 1980-12-15 |     $10000 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $0 |        0.0000 |
| 1980-12-16 |     $10000 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $0 |        0.0000 |
| 1980-12-17 |     $10002 | 0.0069% | 0.0069% |  1.7321% |   1.7321% |      $2 |      100.0000 |
| 1980-12-23 |     $10023 | 0.0116% | 0.0116% |  8.7665% |   8.7665% |     $23 |      100.0000 |
| 1980-12-24 |     $10029 | 0.0117% | 0.0117% | 11.0696% |  11.0696% |     $29 |      100.0000 |
| 1980-12-26 |     $10041 | 0.0154% | 0.0154% | 11.5742% |  11.5742% |     $41 |      100.0000 |
| 1980-12-29 |     $10042 | 0.0150% | 0.0150% | 12.4075% |  12.4075% |     $42 |      100.0000 |
| 1980-12-30 |     $10039 | 0.0173% | 0.0173% |  9.9769% |   9.9769% |     $39 |      128.4113 |
```

<br>

### `portfolio-total`

This function returns the current total value of the portfolio.

**Parameters:**

- none

**Example**:

```clojure
(portfolio-total)

;; output:
10039.706976028898
```

<br>

### `portfolio-daily-ret`

This function returns the current daily return of the portfolio.

**Parameters:**

- none

**Example**:

```clojure
(portfolio-daily-ret)

;; output:
-1.381737451156456E-4
```

<br>

### `portfolio-total-ret`

This function returns the total daily return of the portfolio.

**Parameters:**

- none

**Example**:

```clojure
(portfolio-total-ret)

;; output:
0.0017210374553336212
```

---

## Updating Configuration
### `update-rolling-window`

This function updates the time window for computing the rolling functions (i.e. `r-vol` and `r-sharpe`). By default, the time window is set as `30`.

**Parameters:**

- `n` - new time window for computing the rolling functions in the evaluation report

**Example**:

```clojure
(update-rolling-window 3)

;; output:
Time window is updated as 3. ; success message
```

---

## Plotting Graphs

Functions for generating line plots on chosen variables.

<br>

### `plot`

This function allows users to plot line charts. This plotting function works the best with the Jupyter Notebook.

**Parameters:**

- dataset - contains a map of data to be plotted. Each map should be in the following format: `{:tic "AAPL" :date "1980-12-15" :price "27.00" :return "-0.5 }`
- `series` - series name of the lines to appear in the legend
- `x` - key that contains that x-axis data in the dataset, e.g. `:date`
- `y` - key that contains that y-axis data in the datset, e.g. `:portfolio-value`
- `full-date` - boolean, set to true if you want to have full date (i.e. month, day, year) as labels in the x-axis; if set as false the function would automatically choose the appropariate labels

**Note**: pass full-date as `true` when plotting variables in the **evaluation report**.

<br>

**Example**:

```clojure
;; data to print: an atom of maps

(first data) ;; print first row
;; output
;; {:date "1980-12-16", :pnl-pt 7.806415917975755, :sharpe 1.4142135623730954, :tot-val 10007.806415917976, :vol 0.05517816194058409}

;; Add legend name to series
(def data-to-plot
 (map #(assoc % :plot "sharpe")
  data))

;; Call plotting function
(plot data-to-plot :plot :date :vol true)
```

<br>

Output:

<br>

![image](/img/plot-sharpe.png)
