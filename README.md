# Clojure Backtesting Library

A backtesting framework for quantitative investing and trading, driven by
daily CRSP-style security data.

## Requirements

- Java 11 or later
- [Leiningen](https://leiningen.org/)

## How to install

Clone the repository and start a REPL under it:

```
lein repl
```

or a [Gorilla REPL](http://gorilla-repl.org/), which can save strategies as
notebooks:

```
make start
```

## Sample dataset

`resources/sample-data` holds a small synthetic dataset in the layout the
preprocessing script produces: three securities over January to March 1990,
one with a dividend, one with a 2-for-1 split and one with a gap, plus a
matching supplementary (Compustat-style) dataset. It is enough to run every
API without WRDS access:

```clojure
(load-dataset "resources/sample-data/main" "main" add-aprc)
(load-dataset "resources/sample-data/compustat" "compustat")
(init-portfolio "1990-01-02" 100000)
```

`lein run` runs a short strategy against it, and `lein test` runs the unit
and end-to-end tests on a fresh copy. Regenerate it with
`lein run -m clojure-backtesting.sample-data`.

To use your own data, preprocess it with [`clojask-script`](clojask-script)
into the same layout.

## How to use

1. Go through the examples in the [`/examples`](/examples) folder to get a
   basic understanding of the system. They are Gorilla REPL worksheets
   that run against the sample dataset as they are; their outputs are
   regenerated with `lein run -m clojure-backtesting.worksheet examples/*.clj`.
2. Documentation for every API can be found
   [here](https://clojure-finance.github.io/clojure-backtesting-website/#part-ii-api-documentation).
3. Learn to use the APIs to write your own strategy!

## Orders, margin and fundamentals

Orders fill at the next close and stay pending for `ORDER-EXPIRATION`
trading days (three by default). Buying beyond your cash borrows the
shortfall; selling shares you do not hold borrows the shares. Both count
towards the margin, equity over the gross value of positions, which must
stay above `MAINTENANCE-MARGIN` or every position is closed. New borrowing
must respect `INITIAL-MARGIN`.

A supplementary dataset is joined onto each day's rows using, per
security, the latest filing that was public on that day: its period end
must be on or before the day, and so must its report date when the data
has an `rdq` column.

## Dividends

By default a holding's value follows the security's total return, i.e.
dividends are reinvested in the same security. Call
`(update-reinvest-dividends false)` before `init-portfolio` to have
dividends paid into cash instead; recognising splits in that mode needs the
CRSP `CFACPR` column in the main dataset.

## Output files

Nothing is written to disk by default. Call `(update-output-dir "some/dir")`
before `init-portfolio` to have the order record, the daily portfolio value
and the evaluation report written there as CSV files during the run.

## Report bugs

Feel free to report issues in the repository.

## Development

To run all tests:

```
lein test
```
