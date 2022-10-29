{:title "Large Data"
 :date "2021-01-19"
 :layout :post
 :tags  []
 :toc true}

<br>

**`ns: large-data`**

This namespace features functions that handle large datasets which cannot be loaded to memory at once, e.g. `data-CRSP.csv`.

---

## Read In

Lazily read in the large datasets, with unique name and function.

<br>

### `load-large-dataset`

**Parameters:**

- `location` - abosolute or relative location of the file
- `name` - string, a unique identifier of the dataset

*Optional parameters:* 

- `function`: the extra function that you want to apply when reading in the file (for example, add-aprc-by-date)

**Possible usages:**

```clojure
(load-large-dataset "PATH/TO/FILE" "main")
```

**Implementation peek:**

All the large datasets are stored inside dataset-col, which is map of name being the key and lazy sequence of the large dataset as the content.

<br>

### `set-main`

**Parameters:**

- `name` - string: the name of the main dataset
- definition of main dataset: where the price and adjusted price inforamtion is in.

**Possible usages:**

```clojure
(set-main "main")
```

<br>

---

## Make Order

Functions for making an order to trade a stock.

<br>

### `order-lazy`

This order function allows user to set orders for large dataset. 

**Parameters:**

- `tic` - name / identifier of the ticker to buy or sell (string)
- `quantity` - the quantity to trade. If it is positive, buy the amount, else if it negative, sell the amount. (int/double)

*Optional parameters:* (same with `order`)

- `:remaining` -  boolean: if the quantity given is the trading amount or the exact remaining amount. *By default, false.* It is true, meaning that after this order, I want to have `quantity` amount of certain tickers on my account.

- `:leverage` - boolean: if leverage is **allowed** (if necessary) in this order. Leverage includes buying on margin and short sell. You will only buy on margin if you run out of cash. We will also short sell only when you do not have enough stock. *By default, true.*

- `:dataset` -  lazy sequence: Specify the dataset to look for the order. Note that the function will search from the first line of the given dataset. So if used properly, it could speed up the program. If you are using a standard format dataset, you do not need to use this argument. 

- `:print`  - boolean: If to print the successful orders. By default, false.

- `:direct` - boolean: If to direct the order record into a outer file. By default, true. You will find the file named **out-order-record.csv** in the same directory of your running program, i.e. if you create another clj file, it will be under the same layer of your clj file.

  **The only difference with ordinary `order`:**

- `:expiration` -  int: how many days will this order request be effective. For example, if input 1, then this order request will be withdrawn after tomorrow (if this order does not trade that day because of no such ticker).

<br>

**Possible usages:**

1. *Order with or without leverage:*

    ```clojure
    (order "AAPL" 10) ;with leverage, exact value trading
    (order "AAPL" 10 :leverage false) ;without leverage, exact value trade
    (order "AAPL" -10 :remaining true) ;with leverage, remaining value
    (order "AAPL" -10 :leverage false :remaining true) ;without leverage, remaining value (This must be a failed trade)
    ```

2. *Print the order log:*

    ```clojure
    (order "AAPL" 10 :print true) 
    ```
   
---

## Automation

<br>

### `set-automation`

Initiate an automation by this function.

**Parameters:**

- condition: function: a boolean function as condition
- action: function: function to operate if condition is true

*Optional:*

- max-dispatch: int: the maximum dispatch times of the this automation. By default, infinite.

**Return:**

int: a unique identifier of this condition and action pair

**Example:**

```clojure
(set-automation #((< (get (get (deref available-tics) "AAPL") :PRC))23)) #((order-lazy "AAPL" 0 :remaining true)))
```

***Internal Implementation:***

All the conditions are checked at the first line of the next-date function.

```clojure
(defn next-date
  []
  (check-automation)
  ...)
```

<br>

### `cancel-automation`

Delete an automation.

**Parameter:**

- num: int: the unique identifier returned by `set-automation`

**Example:**

```clojure
(cancel-automation num)
```

---

## Limit orders

<br>

### `stop-buy`

Makes a stop buy order (executed once only when condition is fulfilled).

**Parameter:**

- `tic`: string: ticker to be traded
- `prc`: num: the ticker will be bought when its adjusted price is greater than `prc`
- `qty`: num: quantity to be traded
- `mode`: "lazy or "non-lazy", the current backtester mode

**Example:**

```clojure
(stop-buy "AAPL" 25 10 "non-lazy")

;; output:
Automation 1 dispatched.
```

<br>

### `limit-buy`

Makes a limit buy order (executed once only when condition is fulfilled).

**Parameter:**

- `tic`: string: ticker to be traded
- `prc`: num: the ticker will be bought when its adjusted price is smaller than `prc`
- `qty`: num: quantity to be traded
- `mode`: "lazy or "non-lazy", the current backtester mode

**Example:**

```clojure
(limit-buy "AAPL" 25 10 "non-lazy")

;; output:
Automation 1 dispatched.
```

<br>

### `stop-sell`

Makes a stop sell order (executed once only when condition is fulfilled).

**Parameter:**

- `tic`: string: ticker to be traded
- `prc`: num: the ticker will be sold when its adjusted price is smaller than `prc`
- `qty`: num: quantity to be traded
- `mode`: "lazy or "non-lazy", the current backtester mode

**Example:**

```clojure
(stop-buy "AAPL" 25 10 "non-lazy")

;; output:
Automation 1 dispatched.
```

<br>

### `limit-sell`

Makes a limit sell order (executed once only when condition is fulfilled).

**Parameter:**

- `tic`: string: ticker to be traded
- `prc`: num: the ticker will be sold when its adjusted price is greater than `prc`
- `qty`: num: quantity to be traded
- `mode`: "lazy or "non-lazy", the current backtester mode

**Example:**

```clojure
(limit-sell "AAPL" 25 10 "non-lazy")

;; output:
Automation 1 dispatched.
```



---

## Get Corresponding Line

<br>

### `get-compustat-line`

This function reach out to compustat to find the corresponding line for CRSP.

**Parameters:**

- `line` - map: a line of CRSP dataset (with valid date).
- `name` - string: the name of compustat dataset


**Example:**

```clojure
(get-compustat-line (get (get (deref available-tics) "IBM") :reference) "compustat") ;; recall the definition of available-tics!

;; output
{:fincfq "", :dpcy "", :fic "USA", :itccy "", :cshoq "", :dltisq "", :wcapchq "", :niq "59.213", :dpcq "", :sstkq "", :cik "51143.0", :oancfq "", :loq "", :pstkrq "", :oibdpq "", :ipodate "", :datafqtr "1962.5", :prstkcq "", :ivncfy "", :pstkq "", :saleq "467.7", :ltq "", :lctq "", :txdbq "", :dlcq "", :ppentq "", :dpq "", :atq "", :dvpq "", :wcapchy "", :capxy "", :itccq "", :xintq "", :datacqtr "1962.5", :dltrq "", :ibq "59.213", :actq "", :mibtq "", :xrdq "", :sstky "", :invtq "", :dvy "", :oiadpq "", :mibq "", :icaptq "", :txditcq "", :prccq "353.1442", :ceqq "", :sppey "", :dvq "", :seqq "", :fincfy "", :capxq "", :revtq "467.7", :oancfy "", :ivncfq "", :cusip "459200101", :dltisy "", :gvkey "6066", :addzip "10504", :dlttq "", :prstkcy "", :rdq "", :sic "7370.0", :xsgaq "", :exchg "11.0", :dltry "", :rectq "", :sppeq "", :cogsq "", :tic "IBM", :cheq "", :datadate "1962-09-30", :conm "INTL BUSINESS MACHINES CORP"}
```


