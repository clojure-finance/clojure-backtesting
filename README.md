# Clojure Backtesting

A day-stepping backtesting framework for quantitative trading strategies,
designed for CRSP-style daily security data.

## Features

- **Day-by-day simulation** — step through trading days, place orders, and
  track portfolio performance
- **Technical indicators** — SMA, EMA, MACD, RSI, Bollinger Bands, Rate of
  Change, Parabolic SAR, ATR, Keltner Channels
- **Margin and shorting** — borrow to buy or short sell, with configurable
  initial and maintenance margin requirements
- **Dividend handling** — reinvest automatically or pay to cash
- **Fundamental data** — join Compustat-style datasets, respecting report dates
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

The bundled sample dataset (three securities, Q1 1990) lets you run every API
without external data:

```clojure
(load-dataset "resources/sample-data/main" "main" add-aprc)
(init-portfolio "1990-01-02" 10000)

;; Buy 50 shares — fills at the next close
(order "10001" 50)

;; Step forward 5 trading days
(dotimes [_ 5]
  (next-date)
  (update-eval-report))

;; Sell 20 shares
(order "10001" -20)

;; Continue and close out
(dotimes [_ 3] (next-date) (update-eval-report))
(end-order)

;; Review results
(print-order-record)
(print-portfolio)
(print-portfolio-record -1)
(print-eval-report)
```

Run `lein run` for a quick strategy demo, or `lein test` for the full test
suite.

## Using Your Own Data

Preprocess your data with the included [`clojask-script`](clojask-script) tool,
which converts CRSP and Compustat-style CSVs into the partitioned layout the
backtester expects. See its [README](clojask-script/README.md) for column
mappings and parameters.

```clojure
(load-dataset "/path/to/preprocessed/crsp" "main" add-aprc)
(load-dataset "/path/to/preprocessed/compustat" "compustat")
```

## Examples

The [`examples/`](examples/) folder contains runnable strategy notebooks:

| Example                   | Description                                         |
|---------------------------|-----------------------------------------------------|
| `simple_trading_strategy` | Basic buy/sell loop with order and portfolio output |
| `golden_cross`            | SMA crossover strategy                              |
| `bollinger_bands`         | Mean-reversion using Bollinger Bands                |
| `relative_strength_index` | RSI-based overbought/oversold signals               |
| `rate_of_change`          | Momentum strategy using ROC                         |
| `buying_on_margin`        | Leveraged positions and margin mechanics            |
| `Fundamental analysis`    | Filtering by Compustat fundamentals                 |

Regenerate outputs with:
```
lein run -m clojure-backtesting.worksheet examples/*.clj
```

## Configuration

Call these before `init-portfolio`:

| Function                           | Effect                                             |
|------------------------------------|----------------------------------------------------|
| `(update-reinvest-dividends false)` | Pay dividends to cash instead of reinvesting       |
| `(update-output-dir "out")`         | Write orders, values, and reports as CSV files     |

Tune trading parameters in `clojure-backtesting.parameters`:

| Parameter            | Default | Meaning                                         |
|----------------------|---------|-------------------------------------------------|
| `ORDER-EXPIRATION`   | 3       | Days before an unfilled order expires           |
| `MISSING-DAYS-LIMIT` | 10      | Days without a price before a holding delists   |
| `INITIAL-MARGIN`     | 0.5     | Equity required for new margin positions        |
| `MAINTENANCE-MARGIN` | 0.25    | Equity floor; breach closes all positions       |

## Documentation

Full API documentation:
[clojure-finance.github.io/clojure-backtesting-website](https://clojure-finance.github.io/clojure-backtesting-website/#part-ii-api-documentation)

## Development

```
lein test
```

## License

MIT
