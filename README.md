# Clojure Backtesting

A day-stepping backtesting framework for quantitative trading strategies,
designed for CRSP-style daily security data.

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
- **Fundamental data** — join Compustat-style datasets, respecting report dates
- **Transaction costs and interest** — optional commission on trades and
  interest on loans
- **Performance metrics** — Sharpe ratio, volatility, max drawdown, daily
  returns, and more
- **Plotting** — visualise portfolio and indicator time series in the browser
- **CSV output** — export orders, daily values, and evaluation reports

## Requirements

- Java 11+
- [Leiningen](https://leiningen.org/)

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

The bundled sample dataset lets you run every API without external data. It
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

## Examples

The [`examples/`](examples/) folder contains Gorilla REPL worksheets that run
against the sample dataset. Open them with `make start`, or read them as
plain Clojure files:

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

With dividends paid to cash, splits are recognised only if the main dataset
has the CRSP `CFACPR` column; otherwise a split is paid out as cash too.

`ORDER-EXPIRATION` and `MISSING-DAYS-LIMIT` in `clojure-backtesting.parameters`
have no setter; override them for a session with, for example,
`(alter-var-root #'MISSING-DAYS-LIMIT (constantly 20))`.

## Documentation

Full API documentation:
[clojure-finance.github.io/clojure-backtesting-website](https://clojure-finance.github.io/clojure-backtesting-website/#part-ii-api-documentation)

## Development

Run the unit and end-to-end tests:

```
lein test
```

## License

MIT
