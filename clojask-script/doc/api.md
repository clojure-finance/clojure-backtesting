{:title "API"
 :date "2022-10-16"
 :layout :post
 :tags  []
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

---

# Fundamental APIs

Below are the documentations for basic APIs of the backtester.

## Import Datasets

#### load-dataset

Import the supported dataset that contains the stock market information.

| Argument            | Type                 | Function                          | Remarks                                                      |
| ------------------- | -------------------- | --------------------------------- | ------------------------------------------------------------ |
| `dataset directory` | String               | The path of the dataset directory | The creation of the supported dataset can be found in this tutorial. |
| `role`              | "main" / "compustat" |                                   |                                                              |

**Return**

"Date range: {start date} ~ {end data}"

**Example**

```clojure
=> (load-dataset "/Volumes/T7/CRSP" "main")
"Date range: 1972-01-03 ~ 2017-02-10"
```

## State Starting Postition

#### init-portfolio

Initialize the starting date and capital.

| Argument  | Type   | Function                     | Remarks                                                      |
| --------- | ------ | ---------------------------- | ------------------------------------------------------------ |
| `date`    | String | Indicate the starting date   | If this date is missing in the dataset (i.e. it is a weekend), will use the nearest earlier date instead. If it is also unavailable, will use the nearest later date. |
| `capital` | Number | Indicate the initial capital | Will be given in the form of cash                            |

**Return**

"Date: {actual starting data (may be different from `date`)} Cash: ${`capital`}"

**Example**

```clojure
=> (init-portfolio "1980-04-07" 2000)
"Date: 1980-04-07 Cash: $2000"
```

## Make Orders

#### order

Buy or sell certain number of shares of a ticker.

| Argument       | Type    | Function                                                     | Remarks                                                      |
| -------------- | ------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `ticker`       | String  | Indicate the target ticker                                   | Should be an affordable ticker in the dataset                |
| `quantity`     | Number  | Positive means buy & Negative means sell                     | Indicates the trading amount or the remaining amount depending on `remaining`. |
| [`expiration`] | Integer | Number of days before this order request expires             | By default, *ORDER-EXPIRATION*                               |
| [`remaining`]  | Boolean | If `quantity` is the trade amount or remaining amount        | By default, false (i.e. quantity is the exact trade amount)  |
| [`leverage`]   | Boolean | If you can loan to make the purchase                         | By default, true. However, *INITAIL-MARGIN*, which is the minimum cash : order total ratio should be obeyed. |
| [`print`]      | Boolean | If to print order record on the screen when the order is really made | By default, false.                                           |
| [`log`]        | Boolean | If to direct order record to the log file when the order is really made | By default, false.                                           |

**Return**

All pending orders. Vector of maps.

**Example**

```clojure
=> (order "APL" 10)
=> (next-date) ;; will introduce later
=> (order "APL" 0 :remaining true)
```

## Move Time Pointer

The backtester is used to simulate real-world trading situations. Therefore, there is a date pointer in the platform that times the global clock of the system. Users can move forward through time and cannot go back. Also, users have access to only market information till "today", not future. Below functions are used to interact with the date pointer.

#### get-date

Get today's date.

| No Argument |
| ----------- |

**Return**

A string representing the current date.

**Example**

```clojure
=> (get-date)
"1980-04-07"
```

#### next-date

Move the date pointer forward to the next valid date, skipping over weekends, holidays.

| No Argument |
| ----------- |

**Return**

The new date, if succeed. Otherwise if it has reached the end date, `nil`.

**Example**

```clojure
=> (next-date)
"1980-04-08"
```

#### get-prev-n-date

Get the date n days before the current date (skipping over weekends, holidays).

| Argument | Type             | Function                    | Remarks                                                |
| -------- | ---------------- | --------------------------- | ------------------------------------------------------ |
| `n`      | Positive Integer | Number of days to look back | If the search reaches the starting date, return `nil`. |

**Return**

A date string if searching succeed. If the search passes the starting date, return `nil`.

**Example**

```clojure
=> (get-prev-n-date 3)
"1980-04-01"
```

## Retrieve Information From Datasets

#### get-info

Get all the market information for today in the format of sequence of maps. Note that there is also `get-info-map` which gives another format better for get ticker specific information.

| No Argument |
| ----------- |

**Return**

A sequence of map, each of which contains keys: `:PRC`, `:TICKER`, `:DATE`, `:APRC`, `:RET`.

For example,

```clojure
({:ewretd "-0.01599", :HSICCD "5311.0", :CUM-RET -1.0845139034240396, :CFACSHR "1.1", :INIT-PRICE 5.5, :OPENPRC "", :SECSTAT "R", :SHROUT "3.48", :TICKER "NCL", :DATE "1980-04-07", :APRC 1.859363432980367, :COMNAM "NICHOLS S E INC", :PRIMEXCH "A", :TRDSTAT "A", :FACSHR "", :HEXCD "2", :RET -0.035713999999999996, :EXCHCD "2", :CFACPR "1.1", :DLRET "", :PRC 3.375, :vwretd "-0.019375", :FACPR "", :CUSIP "65380310", :NCUSIP "65380310.0", :PERMCO "21291", :DIVAMT "", :PERMNO "56549", :SHRCD "11", :sprtrn "-0.019187", :VOL "0.0", :SICCD "5311"} {:ewretd "-0.01599", :HSICCD "4923.0", :CUM-RET 0.13767620201494443, :CFACSHR "4.5", :INIT-PRICE 12.75, :OPENPRC "", :SECSTAT "R", :SHROUT "1.7", :TICKER "NCNG", :DATE "1980-04-07", :APRC 14.631949703037334, :COMNAM "NORTH CAROLINA NATURAL GAS CORP", :PRIMEXCH "Q", :TRDSTAT "A", :FACSHR "", :HEXCD "1", :RET 0.019802, :EXCHCD "3", :CFACPR "4.5", :DLRET "", :PRC 12.875, :vwretd "-0.019375", :FACPR "", :CUSIP "65822110", :NCUSIP "65822110.0", :PERMCO "3152", :DIVAMT "", :PERMNO "58043", :SHRCD "11", :sprtrn "-0.019187", :VOL "", :SICCD "4922"} ... )
```

**Example**

```clojure
=> (get-info)
...
```

#### get-info-map

Get all the market information for today in the format of map, where ticker is the key and information of the ticker is the value.

| No Argument |
| ----------- |

**Return**

A map whose keys are all the tickers available today.

For example,

```clojure
{"PL" {:ewretd "-0.01599", :HSICCD "2426.0", :CUM-RET 3.3140493252614993, :CFACSHR "2.0", :INIT-PRICE 14.125, :OPENPRC "", :SECSTAT "R", :SHROUT "12.037", :TICKER "PL", :DATE "1980-04-07", :APRC 388.38441221936534, :COMNAM "PACIFIC LUMBER CO", :PRIMEXCH "N", :TRDSTAT "A", :FACSHR "", :HEXCD "1", :RET 0.005602, :EXCHCD "1", :CFACPR "2.0", :DLRET "", :PRC 44.875, :vwretd "-0.019375", :FACPR "", :CUSIP "69452910", :NCUSIP "69452910.0", :PERMCO "3604", :DIVAMT "", :PERMNO "58667", :SHRCD "10", :sprtrn "-0.019187", :VOL "28900.0", :SICCD "2426"}, "DPT" {:ewretd "-0.01599", :HSICCD "3573.0", :CUM-RET 1.0294626755074137, :CFACSHR "4.0", :INIT-PRICE 21.875, :OPENPRC "", :SECSTAT "R", :SHROUT "4.135", :TICKER "DPT", :DATE "1980-04-07", :APRC 61.240400324837374, :COMNAM "DATAPOINT CORP", :PRIMEXCH "N", :TRDSTAT "A", :FACSHR "", :HEXCD "1", :RET -0.035885, :EXCHCD "1", :CFACPR "10.3", :DLRET "", :PRC 100.75, :vwretd "-0.019375", :FACPR "", :CUSIP "23810020", :NCUSIP "23810020.0", :PERMCO "1249", :DIVAMT "", :PERMNO "59707", :SHRCD "11", :sprtrn "-0.019187", :VOL "29200.0", :SICCD "3573"} ... }
```

**Example**

```clojure
=> (get-info-map)
...
```

#### get-tic-info

Get information for the specified ticker.

| Argument | Type   | Function           | Remarks |
| -------- | ------ | ------------------ | ------- |
| `ticker` | String | The certain ticker |         |

**Return**

A map of information if the ticker exists, otherwise `nil`.

**Example**

```clojure
=> (get-tic-info "APL")
{:ewretd "-0.01599", :HSICCD "2649.0", :CUM-RET -1.0134462484096032, :CFACSHR "1.0", :INIT-PRICE 21.25, :OPENPRC "", :SECSTAT "R", :SHROUT "3.179", :TICKER "APL", :DATE "1980-04-07", :APRC 7.713026455704485, :COMNAM "A P L CORP", :PRIMEXCH "N", :TRDSTAT "A", :FACSHR "", :HEXCD "1", :RET 0.0, :EXCHCD "1", :CFACPR "1.0", :DLRET "", :PRC 8.125, :vwretd "-0.019375", :FACPR "", :CUSIP "202410", :NCUSIP "202410.0", :PERMCO "23557", :DIVAMT "", :PERMNO "28636", :SHRCD "10", :sprtrn "-0.019187", :VOL "3200.0", :SICCD "2649"}

=> (get-tic-info "none")
nil
```

#### get-tic-price

Continuing from `get-tic-info`, get only the price for the specified ticker.

| Argument | Type   | Function           | Remarks |
| -------- | ------ | ------------------ | ------- |
| `ticker` | String | The certain ticker |         |

**Return**

A double representing the closing price of the ticker, otherwise `nil`.

**Example**

```clojure
=> (get-tic-price "APL")
8.125
=> (get-tic-price "none")
nil
```

#### get-tic-by-key

Continuing from `get-tic-info`, get the value of a key from the information of the specified ticker.

| Argument | Type    | Function           | Remarks                                           |
| -------- | ------- | ------------------ | ------------------------------------------------- |
| `ticker` | String  | The certain ticker |                                                   |
| `key`    | Keyword | Specific the key   | Should be according the available keys in the map |

**Return**

Value of arbitrary type depending on the key if the map contains the key, otherwise `nil`.

**Example**

```clojure
=> (get-tic-by-key "APL" :HSICCD)
"2649.0"
=> (get-tic-by-key "APL" :None)
nil
```

#### get-prev-n-days

Get information of the past n days (not including today, skipping weekends).

| Argument | Type             | Function                        | Remarks                                                      |
| -------- | ---------------- | ------------------------------- | ------------------------------------------------------------ |
| [`n`]    | Positive Integer | The number of days to look back | If not give, return a lazy sequence of all prev days, in descending time order.<br>Note that the length of return might be smaller than `n`, if it reaches the starting date. |

**Return**

A sequence (length <= n) of sequence (length = number of tickers) of maps that contains data of the previous n days (not including today), in descending time order, i.e. the most recent date first.

**Example**

```clojure
=> (get-prev-n-days 2)
...

;; one common way that can get all the information of past n days including today is:
=> (conj (get-prev-n-days (- n 1)) (get-info))
...
```

#### get-tic-prev-n-days

Get information of a specific ticker of the past n days (not including today, skipping weekends).

| Argument | Type             | Function                        | Remarks                                                      |
| -------- | ---------------- | ------------------------------- | ------------------------------------------------------------ |
| `ticker` | String           | The specified ticker            |                                                              |
| `n`      | Positive Integer | The number of days to look back | Note that if a ticker is absent in some dates, the length of the actual return might be smaller than `n`. |

**Return**

A vector (length <= n) of maps that contains ticker data of the previous n days (not including today), in descending time order, i.e. the most recent date first.

**Example**

```clojure
=> (get-tic-prev-n-days "APL" 2)
[{:ewretd "0.001023", :HSICCD "2649.0", :CUM-RET -1.0134462484096032, :CFACSHR "1.0", :INIT-PRICE 21.25, :OPENPRC "", :SECSTAT "R", :SHROUT "3.179", :TICKER "APL", :DATE "1980-04-03", :APRC 7.713026455704485, :COMNAM "A P L CORP", :PRIMEXCH "N", :TRDSTAT "A", :FACSHR "", :HEXCD "1", :RET 0.0, :EXCHCD "1", :CFACPR "1.0", :DLRET "", :PRC 8.125, :vwretd "-0.0036850000000000003", :FACPR "", :CUSIP "202410", :NCUSIP "202410.0", :PERMCO "23557", :DIVAMT "", :PERMNO "28636", :SHRCD "10", :sprtrn "-0.005162", :VOL "1700.0", :SICCD "2649"} {:ewretd "0.011197", :HSICCD "2649.0", :CUM-RET -1.0134462484096032, :CFACSHR "1.0", :INIT-PRICE 21.25, :OPENPRC "", :SECSTAT "R", :SHROUT "3.179", :TICKER "APL", :DATE "1980-04-02", :APRC 7.713026455704485, :COMNAM "A P L CORP", :PRIMEXCH "N", :TRDSTAT "A", :FACSHR "", :HEXCD "1", :RET 0.015625, :EXCHCD "1", :CFACPR "1.0", :DLRET "", :PRC 8.125, :vwretd "0.00857", :FACPR "", :CUSIP "202410", :NCUSIP "202410.0", :PERMCO "23557", :DIVAMT "", :PERMNO "28636", :SHRCD "10", :sprtrn "0.004893", :VOL "2600.0", :SICCD "2649"}]

;; one common way that can get all the information of past n days including today is:
=> (conj (seq (get-tic-prev-n-days "ticker" (- n 1))) (get-tic-info "ticker"))
...
```

