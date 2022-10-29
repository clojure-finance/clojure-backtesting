{:title "Data Management"
 :date "2021-01-25"
 :layout :post
 :tags  []
 :toc true}

<br>

**`ns: clojure-backtesting.data-management`**

This namespace features functions for csv data manipulation.

---

## Read Data
Functions for reading csv file.

<br>

### `get-set`

Return a set of ticker and date combinations.

**Parameters:**
- `data` - A row based dataset

**Example:**

```clojure
(get-set "./resources/CRSP-extract.csv")

;; output:
;; a set of maps
{{:tic "AAPL" :datadate "2020-12-10"}}
```

<br>

### `read-csv-row`

Read the CSV file into memeory in a row by row format. 

**Parameters:**
- `file` - path to the csv file to be read

**Example:**

```clojure
(read-csv-row "./resources/CRSP-extract.csv")

;; output:
{{:ewretd "0.002093", :HSICCD "3571", :CFACSHR "56", :date "1980-12-15", :OPENPRC "", :SECSTAT "R", :SHROUT "55.136", :TICKER "AAPL", :COMNAM "APPLE COMPUTER INC", :PRIMEXCH "Q", :TRDSTAT "A", :HEXCD "3", :RET "-0.052061", :EXCHCD "3", :CFACPR "56", :DLRET "", :PRC "27.3125", :vwretd "0.001605", :CUSIP "03783310", :NCUSIP "03783310", :PERMCO "00007", :PERMNO "14593", :SHRCD "11", :sprtrn "0.001702", :VOL "", :SICCD "3573"}...}
```

<br>

### `read-csv-col`

Read the CSV file into memeory in a column by column format. 

**Parameters:**
- `file` - path to the csv file to be read

Example:

```clojure
(read-csv-col "./resources/CRSP-extract.csv")

;; output:
[[:ewretd ["0.002093" "-0.001389" "0.007502" "0.006936" "0.008056" "0.007997" "0.001412" "0.004105" "0.004242" "-0.005709" "0.001174" "0.009927" "0.009458" "0.010594" "0.005368" "-0.03238" "-0.00516" "0.008907" "0.004471" "-0.000664" "0.005702" "0.004183" "0.005738" "0.002124" "-0.010683" "-0.002868" "-0.004657" "0.001699" "-0.003975" "0.004729" "0.001001" "0.002597" "0.001105" "-0.018646" "0.001114" "0.004765" "0.007917" "0.007249" "-0.00282" "-0.001213" "-0.002401" "-0.004518" "-0.001726" "-5.5e-05" "0.001811" "-0.008043" "-0.001957" "0.001809" "0.002916" "-3.4e-05" "0.008656" "0.008212" "0.003819" "-0.001614" "0.002521" "0.001221" "0.002958" "0.003551" "-0.002266" "-0.001807" "0.011681" "0.005793" "0.006435" "-0.000237" "0.004204" "0.003192" "0.008718" "0.005984" "-0.000118" "0.009045" "0.001948" "-0.002487" "-0.000217" "0.007541" "0.005162" "0.002897" "0.002749" "-0.006616" "0.001807" "0.003888" "0.005053" "0.004563" "-0.005292" "-0.003674" "0.006399" "0.008239" "0.003353" "-0.002742" "0.000864" "0.002988" "0.006598" "0.001707" "-0.00814" "-0.005957" "0.00279" "-0.000297" "-0.013762" "-0.006381" "0.002456" "0.005405" "0.004743" "-0.007511" "0.000823" "0.002881" "0.005549" "0.005913" "0.001988" "-0.002401" "0.00334" "0.001324" "0.003778" "0.00442" "0.006344" "0.004271" "0.003653" "0.000907" "-0.011098" "-0.003048" "0.00273" "0.004895" "-8.9e-05" "-0.003319" "0.001876" "0.00827" "0.003869" "0.001707" "-0.008553" "0.000689" "-0.004752" "0.00258" "-0.001013" "0.003547" "-0.001183" "0.001235" "0.000481" "-0.005352" "-0.007092" "-0.006769" "-0.006941" "-0.0139" "-0.004077" "-0.000875" "0.006533" "0.004753" "0.001068" "-0.004419" "0.004426" "0.003234" "0.00429" "-0.012409" "-0.008818" "-0.00373" "-0.002009" "0.006128" "0.004585" "-0.002349" "0.001375" "0.004231" "0.006598"...]...]
```

---

## Search Data
Functions for retreiving rows based on certain conditions.

<br>

### `get-price`

Returns the price of the given ticker.

**Parameters:**

- `tic` - ticker name

**Example:**

```clojure
(get-price "AAPL")

;; output:
"27.45"
;;Note that the format is string
```

<br>


### `last-quar`

Return the last quarter of the given date of row. For instance, if the date of row is "2020-12-09", the returned date will be "2020-09-31".

**Parameters:**
- `data` - one row of the CRSP dataset

**Example:**

```clojure
(last-quar "2020-12-09")

;; output:
"2020-09-31"
```

<br>

### `get-prev-n-days`

Return a vector of map containing the required information for a given period **from the current date (refer to ns:counter for the counting mechanism of the system)**

**Parameters**: 

- `key`- keyword type: the name of the column
- `n`- int: the period length
- `tic`- string: the ticker name

*Optional parameters (Strongly recommended):*

- `pre` - vector: the previous result
- `reference`: lazy: the dataset to refer to, by default, (deref data-set)

*Remarks:*

1. Use `pre` to speed up the infomation retrieving.

2. The logic of the function when `pre` is given is that if the length of the `pre` vector is less than `n`. The function will append one more record to the vector if matched. When the length is equal to `n`, the function will remove the first element of the `pre` and append a new matching result to the tail. And the function will only search in the context of `reference`, so do make sure that the reference given to it is correct.

**Example:**

```clojure
(get-prev-n-days :PRC 2 "AAPL")

;; output:
;; a vector of map(s)
[{:date "2020-11-10" :PRC "12"} {:date "2020-11-11" :PRC "19"}]
```
<br>

### `moving-average`

Used as an example function about how to define wrapper functions to the above function output.

**Parameters:**

- `key` - key of the content to extract from the map
- `vector` - a vector of maps

**Example:**

```clojure
(moving-average (get-prev-n-days :PRC 2 "AAPL")) ;; usually combined using with (get-prev-n-days)

;; output:
15.5
```

<br>

### `moving-sd`

Returns the standard deviation of the map.

**Parameters:**

- `key` - key of the content to extract from the map
- `vector` - a vector of maps

**Example:**

```clojure
(moving-sd (get-prev-n-days :PRC 2 "AAPL")) ;; usually combined using with (get-prev-n-days)

;; output:
15.5
```

---

## Merge Data <span style="color:red">(Not recommended, potential bugs)</span>

***We recommend you to not join the datasets, but search for the data seperately from two datasets.***

Functions for merging the csv files either by row or by column.

<br>

### `merge-data-row`

Merge 2 CSV files row by row using left-join method, i.e. merging the trading data with the accounting data. Merging that data by row is recommended. 

**Parameters:**
- `file1` - the CRSP trading dataset
- `file2` - the COMPUSTAT accounting dataset

**Example**:

```clojure
(merge-data-row "./resources/CRSP-extract.csv" "./resources/Compustat-extract.csv")
```
<br>


### `merge-csv-col`

Merge 2 CSV files column by column using left-join method, i.e. merging the trading data with the accounting data. 

**Parameters:**
- `file1` - the CRSP trading dataset
- `file2` - the COMPUSTAT accounting dataset

**Example**:

```clojure
# (merge-data-col "./resources/CRSP-extract.csv" "./resources/Compustat-extract.csv")
```

---
