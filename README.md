# Clojure Backtesting

[![Clojars Project](https://img.shields.io/clojars/v/com.github.clojure-finance/clojure-backtesting.svg)](https://clojars.org/com.github.clojure-finance/clojure-backtesting)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A day-stepping backtesting framework for quantitative trading strategies on
daily security data: CRSP-style files, or prices and fundamentals from
Yahoo Finance.

## Features

- **Day-by-day simulation** — step through trading days, place orders, and
  track portfolio performance
- **Technical indicators** — EMA, MACD, RSI, Rate of Change, Parabolic SAR,
  ATR and Keltner Channels, plus `moving-avg` and `moving-sd` for building
  SMA crossovers or Bollinger Bands
- **Margin and shorting** — borrow to buy or short sell, with configurable
  initial and maintenance margin requirements
- **Automation** — register condition/action pairs checked at the end of
  every trading day
- **Dividend handling** — reinvest automatically or pay to cash
- **Yahoo Finance data** — download prices, dividends, splits, financial
  statements and earnings dates for any tickers through
  [clj-yfinance](https://github.com/clojure-finance/clj-yfinance), no API key
- **Point-in-time fundamentals** — statement values become visible on their
  report date, per share of the day and in the trading currency, with
  valuation and quality ratios (P/E, book-to-market, dividend yield, ROE,
  Piotroski F-score, …)
- **Compustat-style data** — join a supplementary dataset onto each day's
  rows, respecting report dates
- **Strategy helpers** — monthly or quarterly triggers, ranking and
  screening, target-weight orders and rebalancing
- **Benchmark comparison** — beta, alpha, tracking error and information
  ratio against any security in the dataset
- **Transaction costs and interest** — optional commission on trades and
  interest on loans
- **Performance metrics** — Sharpe ratio, volatility, max drawdown, daily
  returns, and more
- **Plotting** — visualise portfolio and indicator time series in the browser
- **CSV output** — export orders, daily values, and evaluation reports

## Requirements

- Java 11+
- [Leiningen](https://leiningen.org/)

## Installation

As a library, from [Clojars](https://clojars.org/com.github.clojure-finance/clojure-backtesting):

```clojure
;; project.clj
[com.github.clojure-finance/clojure-backtesting "1.1.3"]

;; deps.edn
com.github.clojure-finance/clojure-backtesting {:mvn/version "1.1.3"}
```

To run the examples and the bundled sample data, clone the repository.

## Quick Start

Clone the repository and start a REPL:

```
lein repl
```

or a [Gorilla REPL](http://gorilla-repl.org/), which opens the example
worksheets as notebooks:

```
make start
```

The bundled sample dataset lets you try the engine without external data. It
holds three securities over January to March 1990 — 10001 pays a dividend on
1990-02-15, 10002 splits 2-for-1 on 1990-03-01, and 10003 has no prices from
1990-02-05 to 1990-02-09 — plus a matching Compustat-style dataset:

```clojure
(load-dataset "resources/sample-data/main" "main" add-aprc)
(load-dataset "resources/sample-data/compustat" "compustat") ; optional
(init-portfolio "1990-01-02" 10000)

;; Buy 50 shares — fills at the next close
(order "10001" 50)

;; Step forward 5 trading days
(dotimes [_ 5]
  (next-date)
  (update-eval-report))

;; Sell 20 shares
(order "10001" -20)

;; Continue, then close out all positions
(dotimes [_ 3] (next-date) (update-eval-report))
(end-order)

;; Review results
(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
(print-eval-report)
```

Run `lein run` for a quick strategy demo. Regenerate the sample dataset with
`lein run -m clojure-backtesting.sample-data`.

## How Orders Work

- Orders fill at the next close and stay pending for `ORDER-EXPIRATION`
  trading days (three by default); pass `:expiration n` to `order` to change
  it for a single order.
- On a given day, orders that reduce a position fill before orders that add
  to one, so sale proceeds can pay for purchases.
- Calling `init-portfolio` again restarts the backtest and drops any orders
  still pending, so they cannot fill in the new run.
- Buying beyond your cash borrows the shortfall; selling shares you do not
  hold borrows the shares.
- A holding whose security has had no price for `MISSING-DAYS-LIMIT` trading
  days in a row (ten by default) is treated as delisted and booked as cash at
  its last value.
- The margin is equity over the gross value of positions. New borrowing must
  respect `INITIAL-MARGIN`; if the margin falls below `MAINTENANCE-MARGIN`,
  every position is closed.

## Using Your Own Data

Preprocess your data with the included [`clojask-script`](clojask-script) tool,
which converts CRSP and Compustat-style CSVs into the partitioned layout the
backtester expects. See its [README](clojask-script/README.md) for column
mappings and parameters.

```clojure
(load-dataset "/path/to/preprocessed/crsp" "main" add-aprc)
(load-dataset "/path/to/preprocessed/compustat" "compustat")
```

A Compustat-style dataset is joined onto each day's rows using, per security,
the latest filing that was public on that day: its period end must be on or
before the day, and so must its report date when the data has an `rdq`
column.

Or download prices and fundamentals from Yahoo Finance, as the next section
describes.

## Yahoo Finance Data

`clojure-backtesting.yahoo` (referred in the REPL's `user` namespace, like
the rest of the API) downloads data from Yahoo Finance and turns it into a
dataset the backtester loads:

```clojure
(download-yahoo! "data/tech" ["AAPL" "MSFT" "NVDA" "GOOGL" "SPY"] :start "2015-01-01")
(build-yahoo-dataset! "data/tech")
(load-yahoo-dataset "data/tech")
(init-portfolio "2023-03-01" 100000)
```

To try it without a network connection, build the synthetic snapshots in
`resources/sample-yahoo` instead (see the Yahoo examples below):

```clojure
(build-yahoo-dataset! "resources/sample-yahoo" :out-dir "target/sample-yahoo")
(load-yahoo-dataset "target/sample-yahoo")
```

`download-yahoo!` saves Yahoo's answers unchanged under `data/tech/raw`, one
snapshot per security and download. It skips securities already downloaded
today without errors, so an interrupted or partly failed download can be run
again: it fetches the rest and retries the failures. It also fetches the
exchange rates needed when a company reports in another currency than it
trades in. Prices always run up to today (end them earlier with the build's
`:end`). Options: `:start`, `:fundamentals?` (true), `:fields`
(Yahoo statement fields, `default-fields` or `:all`), `:concurrency` (2),
`:pause-ms` (300) and `:force?`.

`build-yahoo-dataset!` works offline, so the dataset can be rebuilt with
other options without downloading again. It writes:

- `main/`: one row per security and trading day in the CRSP layout:
  `PRC`, `OPENPRC`, `BIDLO`, `ASKHI`, `VOL` and `DIVAMT` as traded on the
  day, `CFACPR`/`CFACSHR` (the later splits), `RET` (total return) and
  `RETX` (without dividends), `CURRENCY`, and the adjusted price columns
  `add-aprc` would add. Yahoo divides old prices by every later split; the
  dataset restores the traded prices, so share counts, splits and dividends
  work as with CRSP data. The security id (`PERMNO`) is the ticker.
- `fundamentals.edn`: the point-in-time fundamentals.
- `dataset.edn`: coverage per security and the warnings, also printed:
  tickers Yahoo did not find, missing exchange rates, and report dates
  that had to be estimated.

Options: `:out-dir`, `:start`, `:end`, `:report-lag-days`,
`:after-close-hour`, `:vintage` and `:allow-mixed-currencies?`; see
`clojure-backtesting.yahoo.build/build-dataset!`. `yahoo-dataset!` runs
all three steps in one call, and `dataset-info` reads `dataset.edn` back.

### Fundamentals

`clojure-backtesting.fundamentals` answers what was known on the backtest
date:

```clojure
(fundamental "AAPL" :net-income)              ; twelve months
(fundamental "AAPL" :net-income :quarterly)   ; latest quarter
(fundamental "AAPL" :stockholders-equity)     ; latest balance sheet
(fundamental-info "AAPL" :diluted-eps)        ; the value and where it comes from
(fundamental-history "AAPL" :total-revenue :annual 4)
```

Fields are Yahoo's statement fields in kebab case (`:total-revenue`,
`:operating-income`, `:diluted-eps`, `:total-assets`,
`:ordinary-shares-number`, `:free-cash-flow`, …; `available-fields` lists
them). Frequencies are `:quarterly`, `:annual`, `:trailing` (Yahoo's
twelve-month figure), `:ttm` (Yahoo's trailing figure, the last four
quarters or the last fiscal year, whichever ends latest) and `:latest`.
Income-statement and cash-flow fields default to `:ttm`, balance-sheet
fields to `:latest`.

- A value becomes visible on the first trading day on which its report
  was public. Report dates come from Yahoo's earnings history and
  announcement dates; an announcement after the exchange's close (16:00
  for most, earlier in Tokyo, Shanghai, Seoul, Taipei and Mumbai) or
  without a time counts from the next day. When Yahoo has no date, 45 days
  after a quarter or 90 after a fiscal year (also for the quarter that
  ends it) are assumed, and `fundamental-info` says so
  (`:report-date-source :lag`). For US large caps the assumption is later
  than the real report; markets with slower reporting may need a longer
  `:report-lag-days`.
- Per-share values and share counts are stated per share of the backtest
  date, matching the price, even when a split came later.
- Values are in the trading currency, converted at the previous day's
  exchange rate; `fundamental-info` keeps the reported value and currency.
- Share counts are checked against net income over EPS; a count more than
  twofold off (Yahoo counts some multi-class companies, such as Berkshire
  Hathaway, in another class than the one quoted) is refused with a
  warning, and ratios on market value are nil.
- A value whose period ended more than `FUNDAMENTALS-MAX-AGE-MONTHS` (18)
  months before the day is nil.

Ratios on the day's price: `market-cap`, `pe-ratio`, `earnings-yield`,
`book-to-market`, `price-to-book`, `sales-to-price`,
`free-cash-flow-yield`, `enterprise-value`, `ev-to-ebitda`,
`dividend-yield`, plus `return-on-equity`, `return-on-assets`,
`gross-margin`, `operating-margin`, `net-margin`, `debt-to-equity`,
`current-ratio`, `growth`, `piotroski-signals` and `piotroski-f-score`.
They return nil rather than a misleading number, for example a P/E on
losses. `latest-earnings` and `days-since-earnings` give the latest
earnings announcement with its EPS surprise; use `:surprise-pct`, since
Yahoo's EPS there is the adjusted figure, in an inconsistent share basis.

### Strategy helpers

`clojure-backtesting.strategy` has `new-period?` (first trading day of a
`:week`, `:month`, `:quarter` or `:year`, and the first day of the run),
`rank-by`, `top-n`,
`equal-weights`, `order-target-percent` and `rebalance`. A monthly value
strategy:

```clojure
(while (neg? (compare (get-date) "2024-12-31"))
  (when (new-period? :month)
    (rebalance (equal-weights (top-n earnings-yield 5))))
  (next-date)
  (update-eval-report))
(end-order)
(benchmark-stats "SPY")
```

`benchmark-stats` (in `clojure-backtesting.evaluate`) compares the
portfolio with any security in the dataset: returns, beta, alpha,
correlation, tracking error and information ratio.

### Caveats

- **Survivorship bias.** Yahoo only has securities that still trade, so a
  universe chosen today leaves out companies that failed or were taken
  over. Treat results as optimistic.
- **Short fundamentals history.** Yahoo shows about four fiscal years and
  five quarters at a time, so fundamentals start about three and a half
  years before the download (when the oldest of those fiscal years was
  reported); prices go back much further. Start a fundamentals strategy
  once its universe has data, or it trades on the few companies that
  report first. Snapshots accumulate: download again every quarter and the
  history grows. Values Yahoo restates between downloads keep their first
  version (`:vintage :latest` to change that).
- **Restated, not as first reported.** Within one download, values are as
  Yahoo has them today, which may include later restatements.
- **Unofficial API.** Yahoo does not document these endpoints and may
  change or rate-limit them; statements and earnings dates use clj-yfinance's
  experimental namespaces. Check Yahoo's terms before commercial use.
- **One currency per portfolio.** The engine adds up all positions as one
  currency, so a dataset mixing trading currencies is refused unless
  `:allow-mixed-currencies?` is set.

## Examples

The [`examples/`](examples/) folder contains Gorilla REPL worksheets that run
against the sample datasets; the Yahoo ones use the synthetic snapshots in
`resources/sample-yahoo` (regenerated by
`lein run -m clojure-backtesting.sample-yahoo`). Open them with
`make start`, or read them as plain Clojure files:

| Example                   | Description                                               |
|---------------------------|-----------------------------------------------------------|
| `simple_trading_strategy` | Basic buy/sell loop with order and portfolio output       |
| `golden_cross`            | 15/30-day moving average crossover                        |
| `bollinger_bands`         | Mean reversion using Bollinger Bands                      |
| `relative_strength_index` | RSI-based overbought/oversold signals                     |
| `rate_of_change`          | Momentum strategy using ROC                               |
| `buying_on_margin`        | Leveraged positions and margin mechanics                  |
| `bad_strategy`            | A losing short position through a dividend                |
| `automation`              | Condition/action rules dispatched each trading day        |
| `Plotting_frame`          | Line charts of security prices and returns                |
| `Fundamental analysis`    | Filing visibility by report date and a return-on-equity screen |
| `yahoo_finance_data`      | Building a Yahoo dataset: traded prices, point-in-time fundamentals, ratios |
| `value_strategy`          | Monthly earnings-yield strategy against an index         |
| `quality_and_earnings`    | Piotroski F-score and trading on earnings announcements  |

Regenerate their outputs with:

```
lein run -m clojure-backtesting.worksheet examples/*.clj
```

## Configuration

Call these before `init-portfolio`:

| Function                               | Effect                                              |
|----------------------------------------|-----------------------------------------------------|
| `(update-reinvest-dividends false)`    | Pay dividends to cash instead of reinvesting        |
| `(update-output-dir "out")`            | Write orders, values, and reports as CSV files      |
| `(update-initial-margin 0.5)`          | Equity required for new borrowing; `nil` for none   |
| `(update-maintenance-margin 0.25)`     | Margin floor below which all positions are closed   |
| `(update-transaction-cost 0.001)`      | Commission as a fraction of trade value (default 0) |
| `(update-interest-rate 0.05)`          | Annual interest rate charged on loans (default 0)   |
| `(update-fundamentals-max-age 12)`     | Months after its period end a fundamental stays usable (default 18) |

With dividends paid to cash, splits are recognised only if the main dataset
has the CRSP `CFACPR` column; otherwise a split is paid out as cash too.

`ORDER-EXPIRATION` and `MISSING-DAYS-LIMIT` in `clojure-backtesting.parameters`
have no setter; override them for a session with, for example,
`(alter-var-root #'MISSING-DAYS-LIMIT (constantly 20))`.

## Documentation

Guides, examples and the full API reference:
[clojure-finance.github.io/clojure-backtesting-website](https://clojure-finance.github.io/clojure-backtesting-website/)

## Development

Run the unit and end-to-end tests (no network):

```
lein test
```

and the tests that download from Yahoo Finance:

```
lein test :live
```

## License

MIT
