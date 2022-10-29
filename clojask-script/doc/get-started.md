{:title "Get Started"
:date "2021-01-27"
:layout :post
:toc true}

---

## Setting Up the Playground

<br>

1. Make sure that you have **leiningen** and **jupyter notebook** installed on your local machine. (These two may require further environment dependencies, please refer to their official websites for details.)

   - Installation guide for leiningen can be found at:

     <https://github.com/technomancy/leiningen/wiki/Packaging>

   - Jupyter notebook can be installed at:

     <https://jupyter.org/install>

<br>

#### Installation on Linux or Mac

Run the following command so that the project could be compiled as a .jar file and added as a new Jupyter kernel: (_Note that this operation may download a number of required packages, which may take up some time if you download them for the first time._)

```
make add_kernel
```

(If there is an error, make sure that you have **terminated** the Jupyter notebook running kernels before running this step.)

<br>

When the process is completed, you should see the following output:

```
Installed jar:      target/uberjar/clojure-backtesting-0.1.0-SNAPSHOT-standalone.jar
Install directory:  ~/Library/Jupyter/kernels/backtesting_clojure
Kernel identifier:  backtesting_clojure

Installation successful.
```

<br>

#### Installation on Windows

The backtester is proven to be working on Windows if set up correctly.
1. Install Java 8+, Jupyter notebook.
2. Install lein. Verify by typing `lein --version` in terminal. (Restart the shell after installing it.)
3. Pull the repository from GitHub or simply download it. And `cd` to this directory.
4. Input this into the shell:
```
lein uberjar;
lein clojupyter remove-install backtesting_clojure;
lein clojupyter install --ident backtesting_clojure --jarfile target/uberjar/clojure-backtesting-0.1.0-SNAPSHOT-standalone.jar
```
5. Repeat this step every time you update the backtester. 
6. If the output of the above command is like:
![373f2c4c235e05c728da361d1650cfaf的副本](https://user-images.githubusercontent.com/43634213/110751657-23eb1b00-827f-11eb-9232-fd02e80f35b4.png)
You should go to the installed location (Install directory in the above picture) and drag the whole kernel folder into the `\kernels\` folder of the same level.
And then, you should open the moved folder and find the .json file. Update the location in .json file to this new location.

<br>

Finally, when you restart the Jupyter Notebook application, you could select the kernel named `backtesting_clojure`. You can make use of the backtester by choosing this kernel.

---

## Beginner Tutorials

<br>

If you are new to clojure, we recommend having a quick read of the following tutorials first:

- [Clojure by Example](http://kimh.github.io/clojure-by-example/#about) - useful for beginners pick up the syntax quickly

- [Clojure Docs](https://clojuredocs.org/) - a more thorough documentation that explains the built-in functions in Clojure

- [Clojure for the Brave and True](https://www.braveclojure.com/clojure-for-the-brave-and-true/) - a book that helps you learn Clojure in an in-depth manner

---

## Help & Support

<br>

In case you would encounter difficulties in using the API within the backtester or have any suggestions for additional examples, feel free to post it [here](https://github.com/clojure-finance/clojure-backtesting/issues).

---

## Backtester Mode

<br>

You could run the backtester in two modes, which are the **lazy** and the **non-lazy** modes respectively. The only differences between the two modes lie within the way of loading the dataset and making an order.

<br>

### Lazy mode

- This is mainly for development, and you need to run it with the original large-sized datasets named `data-CRSP.csv` and `data-Compustat.csv`.
- Note that since these datasets are large in size (> 10 GB), they are not included in `resources/` directory within the repository, and need to be downloaded separately.

  (i) To run the backtester in lazy mode, load the dataset in the following way:

  ```clojure
  ;; load CRSP
  (load-large-dataset "data-CRSP.csv" "main" add-aprc-by-date)
  ;; load compustat
  (load-large-dataset "data-Compustat.csv" "compustat")
  ```

  As the original dataset is quite big and takes time to run with the backtester, optionally you could create a smaller version by truncating some rows:

  ```clojure
  cat data-CRSP.csv | tail 10000 > CRSP-smaller.csv
  ```

  (ii) An order could be made by calling the following function under the lazy mode:

  ```clojure
  (order-lazy "AAPL" 50) ; buy 50 stocks
  ```
  

<br>

### Non-lazy mode (Not recommended)

- This is the default mode that the backtester is set in.
- This is mainly for debugging, and you need to run it with the smaller datasets named `CRSP-extract.csv` and `Compustat-extract.csv`.

  (i) To run the backtester in non-lazy mode, load the dataset in the following way:

  ```clojure
  (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
  ```

  (ii) An order could be made by calling the following function under the non-lazy mode:

  ```clojure
  (order "AAPL" 50) ; buy 50 stocks
  ```


---

## Code Walkthrough

We'll go through the code in `./examples/Simple trading strategy.ipynb` notebook to have a glimpse of how to write code that could be run with the backtester.

<br>

### Import libraries

To make use of the functions in the backtester library, it is necessary to import them whenever you create a new jupyter notebook file. Also make sure that you've compiled the **most up-to-date** clojure-backteser kernel, and have selected it in the Jupyter Notebook application.

```clojure
(ns clojure-backtesting.demo
  (:require [clojure.test :refer :all]
            [oz.notebook.clojupyter :as oz]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.large-data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojupyter.kernel.version :as ver]
            [clojupyter.misc.helper :as helper]
  ) ;; require all libriaries from core
  (:use clojure.pprint)
)
```

<br>

### Import dataset

Load the CRSP-extract.csv dataset to the program by providing its relative path. Note that it is a must to nest it with `read-csv-row` and `add-aprc`, as they respectively parse the csv file and automatically add the adjusted price column to the dataset.

(i) To load the dataset for **non-lazy** mode, import the extract dataset:
```clojure 
(reset! data-set (add-aprc (read-csv-row "../resources/CRSP-extract.csv")));
```

(ii) To load the dataset for **lazy** mode, import the large dataset:
```clojure 
(load-large-dataset "../resources/CRSP-extract.csv" "main" add-aprc)
(set-main "main")
```

<br>

### Initialise portfolio

Pass the date ("YYYY-MM-DD") and initial capital (non-negative integer) to the `init-portfolio` function.

```clojure
(init-portfolio "1980-12-16" 10000);
```

<br>

### Check available tickers

You could check what tickers you could trade on the current date (i.e. 1980-12-16).

```clojure
(keys (deref available-tics))
```

<br>

### Write your strategy

With all these set-up, you are ready to write your strategy.

<br>

The below demo is a trading strategy that follows a simple logic.

<u>**Simple strategy**</u>

In a timespan of 10 days (inclusive of today),

- Buy 50 stocks of AAPL on the first day
- Sell 10 stocks of AAPL on every other day


(i) The following example code is for running under the **non-lazy** mode:
```clojure
;; define the "time span", i.e. to trade in the coming 10 days
(def num-of-days (atom 10))

(while (pos? @num-of-days) ;; check if num-of-days is > 0
    (do
        ;; write your trading strategy here
        (if (= 10 @num-of-days) ;; check if num-of-days == 10
            (do
                (order "AAPL" 50) ; buy 50 stocks
                (println ((fn [date] (str "Buy 50 stocks of AAPL on " date)) (get-date)))
            )
        )
        (if (odd? @num-of-days) ;; check if num-of-days is odd
            (do
                (order "AAPL" -10) ; sell 10 stocks
                (println ((fn [date] (str "Sell 10 stocks of AAPL on " date)) (get-date)))
            )
        )

        (update-eval-report (get-date)) ;; update the evaluation metrics every day

        ; move on to the next trading day
        (next-date)

        ; decrement counter
        (swap! num-of-days dec)
    )
)

; call this so as to create output files
(end-order)

; check whether counter == 0
(println ((fn [counter] (str "Counter: " counter)) @num-of-days))
```

<br>

(ii) The following example code is for running under the **lazy** mode:

```clojure
;; define the "time span", i.e. to trade in the coming 10 days 
(def num-of-days (atom 10))                              

(while (pos? @num-of-days) ;; check if num-of-days is > 0
    (do 
        ;; write your trading strategy here
        (if (= 10 @num-of-days) ;; check if num-of-days == 10
            (do
                (order-lazy "AAPL" 50) ; buy 50 stocks
                (println ((fn [date] (str "Buy 50 stocks of AAPL on " date)) (get-date)))
            )
        )
        (if (odd? @num-of-days) ;; check if num-of-days is odd
            (do
                (order-lazy "AAPL" -10) ; sell 10 stocks
                (println ((fn [date] (str "Sell 10 stocks of AAPL on " date)) (get-date)))
            )
        )
        
        (update-eval-report (get-date)) ;; update the evaluation metrics every day
        
        ; move on to the next trading day
        (next-date)
        
        ; decrement counter
        (swap! num-of-days dec)
    )
)

; check whether counter == 0
(println ((fn [counter] (str "Counter: " counter)) @num-of-days))
```

<br>

Note that in the above snippets, it is necessary to iteratively call `next-date` so that the system could "move on to the next trading day". (check the details in the _"Counter"_ section)

<br>

```clojure
;; output:

Buy 50 stocks of AAPL on 1980-12-16
Sell 10 stocks of AAPL on 1980-12-17
Sell 10 stocks of AAPL on 1980-12-19
Sell 10 stocks of AAPL on 1980-12-23
Sell 10 stocks of AAPL on 1980-12-26
Sell 10 stocks of AAPL on 1980-12-30
Counter: 0
```

By printing these debugging messages, I could double-check whether the orders were accurate.

<br>

### Check order record

Alternatively, you could also directly view the order record.

```clojure
(pprint/print-table (deref order-record))
```

```clojure
;; output:

|      :date | :tic |  :price | :quantity | :reference |
|------------+------+---------+-----------+------------|
| 1980-12-17 | AAPL | 25.9375 |        50 |          1 |
| 1980-12-18 | AAPL | 26.6875 |       -10 |          1 |
| 1980-12-22 | AAPL | 29.6875 |       -10 |          1 |
| 1980-12-24 | AAPL | 32.5625 |       -10 |          1 |
| 1980-12-29 | AAPL | 36.0625 |       -10 |          1 |
| 1980-12-31 | AAPL | 34.1875 |       -10 |          1 |
```

<br>

### View portfolio & portfolio record

You could view the portfolio and check the changes in portfolio value too. Note that the portfolio record could also be found in the `out_portfolio_value_record.csv` file.

```clojure
(view-portfolio)
```

```clojure
;; output:
| :asset |  :price | :aprc | :quantity | :tot-val |
|--------+---------+-------+-----------+----------|
|   cash |     N/A |   N/A |       N/A |    10295 |
|   AAPL | 34.1875 | 29.42 |         0 |        0 |
```

<br>

```clojure
;; pass -1 so that the entire record is printed, else pass the no. of rows
(view-portfolio-record -1)
```

```clojure
;; output:
|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage |
|------------+------------+------------+----------+-------+-----------|
| 1980-12-16 |     $10000 |      0.00% |    0.00% | $0.00 |     0.00% |
| 1980-12-17 |     $10000 |      0.00% |    0.00% | $0.00 |     0.00% |
| 1980-12-18 |     $10016 |      0.00% |    0.16% | $0.00 |     0.00% |
| 1980-12-19 |     $10043 |      0.27% |    0.44% | $0.00 |     0.00% |
| 1980-12-22 |     $10066 |      0.00% |    0.66% | $0.00 |     0.00% |
| 1980-12-23 |     $10081 |      0.15% |    0.81% | $0.00 |     0.00% |
| 1980-12-24 |     $10100 |      0.00% |    1.00% | $0.00 |     0.00% |
| 1980-12-26 |     $10122 |      0.22% |    1.22% | $0.00 |     0.00% |
| 1980-12-29 |     $10126 |      0.00% |    1.26% | $0.00 |     0.00% |
| 1980-12-30 |     $10123 |     -0.03% |    1.22% | $0.00 |     0.00% |
| 1980-12-31 |     $10119 |      0.00% |    1.19% | $0.00 |     0.00% |
```

```clojure
;; only print first 3 rows of the portfolio record
(view-portfolio-record 3)
```

```clojure
;; output:
|      :date | :tot-value | :daily-ret | :tot-ret | :loan | :leverage |
|------------+------------+------------+----------+-------+-----------|
| 1980-12-16 |     $10000 |      0.00% |    0.00% | $0.00 |     0.00% |
| 1980-12-17 |     $10000 |      0.00% |    0.00% | $0.00 |     0.00% |
| 1980-12-18 |     $10016 |      0.00% |    0.16% | $0.00 |     0.00% |
```

<br>

### Generate evaluation report

If you update the evaluation report every day (as `update-eval-report` is called for 10 times in the loop), you'll obtain a evluation report with daily records.

Detailed explanation of the evaluation metrics could be found in the _"Evaluate"_ section.

However, note that if you are traversing a large amount of dates, it would be better **not** to update the evaluation metrics every day as it would require a large amount of memory and computation time. Alternatively, you could print a small number of rows of the evaluation report.

 Note that the evaluation report could also be found in the `out_evaluation_report.csv` file.

<br>

```clojure
;; pass -1 so that the entire record is printed, else pass the no. of rows
(eval-report -1)
```

```Clojure
;; output:
|      :date | :tot-value |    :vol |  :r-vol |  :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
|------------+------------+---------+---------+----------+-----------+---------+---------------|
| 1980-12-16 |     $10000 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $0 |        0.0000 |
| 1980-12-17 |     $10016 | 0.0407% | 0.0407% |  1.7321% |   1.7321% |      $8 |      100.0000 |
| 1980-12-18 |     $10016 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $8 |        0.0000 |
| 1980-12-19 |     $10066 | 0.0598% | 0.0598% |  4.8019% |   4.8019% |     $22 |      100.0000 |
| 1980-12-22 |     $10066 | 0.0532% | 0.0532% |  5.3929% |   5.3929% |     $22 |      100.0000 |
| 1980-12-23 |     $10100 | 0.0499% | 0.0499% |  8.6792% |   8.6792% |     $25 |      100.0000 |
| 1980-12-24 |     $10100 | 0.0475% | 0.0475% |  9.1298% |   9.1298% |     $25 |      100.0000 |
| 1980-12-26 |     $10126 | 0.0477% | 0.0477% | 11.4442% |  11.4442% |     $25 |      100.0000 |
| 1980-12-29 |     $10126 | 0.0487% | 0.0487% | 11.2135% |  11.2135% |     $25 |      100.0000 |
| 1980-12-30 |     $10119 | 0.0473% | 0.0473% | 10.9035% |  10.9035% |     $19 |      113.3677 |
```

```clojure
;; just print the first 3 rows
(eval-report 3)
```

```Clojure
;; output:
|      :date | :tot-value |    :vol |  :r-vol |  :sharpe | :r-sharpe | :pnl-pt | :max-drawdown |
|------------+------------+---------+---------+----------+-----------+---------+---------------|
| 1980-12-16 |     $10000 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $0 |        0.0000 |
| 1980-12-17 |     $10016 | 0.0407% | 0.0407% |  1.7321% |   1.7321% |      $8 |      100.0000 |
| 1980-12-18 |     $10016 | 0.0000% | 0.0000% |  0.0000% |   0.0000% |      $8 |        0.0000 |
```
<br>

### Plot variables

You could try plotting some variables shown in the portfolio record / evaluation report tables.

<br>

#### Plotting values in portfolio record

<br>

```clojure
;; 1) Define the data to be the record that features the column
(def data (deref portfolio-value))

;; 2) Assign it to a new variable 'data-to-plot`, so that
;; we could have the legend name added (here = "port-value")
(def data-to-plot
 (map #(assoc % :plot "port-value")
  data))

;; 3) Pass the data and its keys to the plotting function
;; last arg "false" means we do not need full date to be shown on the x-axis
(plot data-to-plot :plot :date :daily-ret false)
```

<br>

Output:

<br>

![image](/img/plot-portfolio-value.png)

<br>

#### Plotting values in evaluation report

<br>

```clojure
(def data (deref eval-record))
(def data-to-plot
 (map #(assoc % :plot "volatility")
  data))

;; Note that we need to set it as "true" for plotting values in eval-report
(plot data-to-plot :plot :date :vol true)
```

<br>

Output:

<br>

![image](/img/plot-volatility.png)
