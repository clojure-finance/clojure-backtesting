# Clojure Backtesting Library

A backtesting framework for quantitative investing and trading.

## Requirements


- Java
- Clojure Leiningen

A good [tutorial](https://ericnormand.me/guide/how-to-install-clojure) about installing the above softwares. **Remember to reboot the system after changing the environment path every time.**

## How to install

1. ### Lein REPL

   *No need to install extra softwares. **Recommended for new users.***

   1. Clone the repo and decompress.

   2. Under the directory, run below command in the terminal to start a Lein REPL.

      `lein repl`

   3. Run the examples in the [`/examples`](/examples) folder line by line in the REPL.

2. ### Gorilla REPL

   *Can save strategies in notebooks.*

   every time you open Gorilla REPL, run
   ```
   lein gorilla
   ```

3. ### Try it out online

   *Only try out the basic APIs with a small dataset.*

   [Online interactive notebook](https://mybinder.org/v2/gh/clojure-finance/clojure-backtesting/binder)

## How to use

1. Go through every examples in the [`/examples`](/examples) folder to get a basic understanding of the system.
2. Documentations for every detailed APIs can be found [here](https://clojure-finance.github.io/clojure-backtesting-website/#part-ii-api-documentation).
3. Learn to use the APIs to write your own strategy!

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

## Dividends

By default a holding's value follows the security's total return, i.e.
dividends are reinvested in the same security. Call
`(update-reinvest-dividends false)` before `init-portfolio` to have
dividends paid into cash instead; recognising splits in that mode needs the
CRSP `CFACPR` column in the main dataset.

## Update

Make sure you have the latest version of the code installed by running after each clone:
```
make add_kernel
```



## Report bugs

As we are still working to fully debug the code and create more examples, feel free to report issues in the repository and we appreciate your kind support.  

## Development

To start an interactive prompt where you can enter arbitrary code to run in the context of your project:
```
lein repl
```
To run the default `:main` set in `project.clj`:
```
lein run
```
To run all tests written in the `test` namespace:
```
lein test
```

