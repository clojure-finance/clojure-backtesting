{:title "Order"
 :date  "2021-01-22"
 :layout :post
 :tags  []
 :toc true}

**`ns: clojure-backtesting.order`**

This namespace features functions for making an order and viewing the order record.

---

## Initialisation

Functions for setting up the backtesting environment.

<br>

### `init-portfolio`

This function initialises the program with a starting date and the amount of initial capital.

**Parameters:**

- `date` - starting date: **Must be a valid date in the dataset**
- `cash` - the amount of money you have initially

*Optional parameters:* 

- `:standard`: boolean: set to false if you are not using a standard dataset. By default, true.

**Possible usages:**

```clojure
(init-portfolio "1980-12-15" 100000)
;; means that you start at "1980-12-15" havin 100000 cash
```

---

## Make Order

Functions for making an order to trade a stock.

<br>

### `order`

This order function allows user to set orders. 

**Parameters:**
- `tic` - name / identifier of the ticker to buy or sell (string)
- `quantity` - the quantity to trade. If it is positive, buy the amount, else if it negative, sell the amount. (int/double)

*Optional parameters:* 

- `:remaining` - boolean: if the quantity given is the trading amount or the exact remaining amount. *By default, false.* It is true, meaning that after this order, I want to have `quantity` amount of certain tickers on my account.

- `:leverage` - boolean: if leverage is **allowed** (if necessary) in this order. Leverage includes buying on margin and short sell. You will only buy on margin if you run out of cash. We will also short sell only when you do not have enough stock. *By default, true.*

- `:dataset` - lazy sequence: Specify the dataset to look for the order. Note that the function will search from the first line of the given dataset. So if used properly, it could speed up the program. If you are using a standard format dataset, you do not need to use this argument. 

- `:print` - boolean: If to print the successful orders. By default, false.

- `:direct` - boolean: If to direct the order record into a outer file. By default, true. You will find the file named **out-order-record.csv** in the same directory of your running program, i.e. if you create another clj file, it will be under the same layer of your clj file.

<br>

**Possible usages:**

1. *Order with or without leverage:*

   ```clojure
   (order "AAPL" 10) ;with leverage, exact value trading
   (order "AAPL" 10 :leverage false) ;without leverage, exact value trade
   (order "AAPL" -10 :remaining true) ;with leverage, remaining value
   (order "AAPL" -10 :leverage false :remaining true) ;without leverage, remaining value (This must be a failed trade)
   ```

<br>

2. *Use together with the available-tics: (This will buy 10 stocks from the first ticker that shows in available-tics.)*

   ```clojure
   (let [tics (deref available-tics) tic (name (first (keys (tics))))]
      (order tic 10 :dataset (get (deref (get (get (dref available-tics) :AAPL) :pointer)) :reference))) ; The part after the dataset is copied from usages of available-tics
   ```

<br>

3. *Print the order log:*

   ```clojure
   (order "AAPL" 10 :print true)
   ```

   ​      
---

## Order record

<br>

### `order-record`

An atom that contains a vector of maps. This variable is automatically maintained by the order function (regardless of `:print`).


**Example:**

```clojure
(println order-record)

;; output
[{:date "1980-12-18" :tic "AAPL" :quantity 13} ... ]
```


​      