# Clojure Backtesting Library

A backtesting framework for quantitative investing and trading.

## Requirements


- Java
- Clojure Leiningen

A good [tutorial](https://ericnormand.me/guide/how-to-install-clojure) about installing the above softwares. **Remember to reboot the system after changing the environment path every time.**

##### Optional:


- Jupyter Notebook

[Offical document](https://jupyter.org/install#jupyter-notebook) to install Juputer Notebook. Replace `pip` with `pip3` if the former fails.

## How to install

1. ### Lein REPL

   *No need to install extra softwares. **Recommended for new users.***

   1. Clone the repo and decompress.

   2. Under the directory, run below command in the terminal to start a Lein REPL.

      `lein repl`

   3. Run the examples in the [`/examples`](/examples) folder run by run in the REPL.

2. ### Juputer Notebook

   *Can save strategies in notebooks.*

   Detailed installation instructions about running backtester with Jupyter Notebook could be found [here](https://clojure-finance.github.io/clojure-backtesting-website/posts/get-started/https://clojure-finance.github.io/clojure-backtesting-website/).

   All Jupyter Notebook examples could be found in the [`/examples`](/examples) folder.

3. ### Try it out online

   *Only try out the basic APIs with a small dataset.*

   [Online interactive notebook](https://mybinder.org/v2/gh/clojure-finance/clojure-backtesting/binder)

## How to use

1. Go through every examples in the [`/examples`](/examples) folder to get a basic understanding of the system.
2. Documentations for every detailed APIs can be found [here](https://clojure-finance.github.io/clojure-backtesting-website/#part-ii-api-documentation).
3. Learn to use the APIs to write your own strategy!

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

