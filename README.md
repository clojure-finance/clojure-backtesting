# Clojure Backtesting Library

A backtesting framework for quantitative investing and trading.

### Try it out online
[Online interactive notebook](https://mybinder.org/v2/gh/clojure-finance/clojure-backtesting/binder)

### Requirements

```
- Clojure
- Leiningen
```
## How to use

Detailed installation instructions and the API documentation could be accessed at [https://clojure-finance.github.io/clojure-backtesting-website/](https://clojure-finance.github.io/clojure-backtesting-website/).

All Jupyter Notebook examples could be found in the `/examples` directory.


## Update

Make sure you have the latest version of the code installed by running:
```
git pull
make add_kernel # need to re-install kernel after update
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


