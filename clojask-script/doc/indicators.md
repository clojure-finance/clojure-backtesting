{:title "Indicators"
 :date "2021-01-18"
 :layout :post
 :tags  []
 :toc true}

<br>

**`ns: indicators`**

This namespace includes the APIs for some popular technical analysis indicators.

---

## Trend Indicators

<br>

### `EMA`

This function calculates the Exponential Moving Average (EMA). Note that the result is `true` after you call the `EMA-CYCLE` in a row.

(Assocated parameter: `EMA-CYCLE`, by default, 20)

**Parameters:**

- `price` - the price of a ticker
- `prev-ema` - ema of the last day

**Possible usages:**

```clojure
(def prev-EMA (get-price "AAPL")) 
; assign the lastest price to prev EMA as initialisation
(while
  (def prev-EMA (EMA (get-price "AAPL") prev-EMA))) 
  ; continuously overwrite prev-EMA with the latest result
```

<br>

### `tic-EMA`

An alternative wrapper function of the `EMA` function.

**Parameters:**

- `tic` - ticker name
- `key` - key of the price
- `prev-EMA` - ema of the last day

**Possible usages:**

```clojure
(def prev-EMA (get-price "AAPL")) 
;; assign the lastest price to prev EMA as initialisation
(while
  (def prev-EMA (tic-EMA "AAPL" :PRC prev-EMA))) 
  ;; continuously overwrite prev-EMA with the latest result
```

<br>

### `MACD`

This function calculates the Moving Average Convergence / Divergence (MACD).

**Parameters:**

- `price` - the price of a ticker
- `ema-12` - 12-day ema of the last day
- `ema-26` - 26-day ema of the last day

**Output:**

`[MACD new-ema-12 new-ema-26]`

**Possible usages:**

```clojure
(def ema-12 (get-price "AAPL")) 
(def ema-26 (get-price "AAPL"))
;; assign the lastest price to prev EMA as initialisation
(while
  (let [tmp (MACD (get-price "AAPL") ema-12 ema-26)
        new-MACD (first tmp)] 
        ;; this is the MACD result
    (def ema-12 (nth tmp 1))
    (def ema-26 (nth tmp 2)))) ;; continuously overwrite prev-EMA with the latest result
```

<br>

### `parabolic-SAR`

This function calculates the Parabolic Stop and Reverse (SAR).

**Parameters:**

- `tic` - the symbol of a ticker
- `mode` - "lazy" or "non-lazy
- `af` - acceleration factor
- `prev-psar` - SAR value of the previous period

**Output:**

- `parabolic-SAR`

**Possible usages:**

```clojure
(let [prev-close (Double/parseDouble (get (first (get-prev-n-days :PRC 1 "OMFGA")) :PRC))]
    (println (parabolic-SAR "OMFGA" "non-lazy" 0.2 prev-close))
)
```

<br>

## Momentum Indicators
### `ROC`

This function calculates the Rate of Change (ROC) indicator.

**Parameters:**

- `tic` - the name of the ticker
- `n` - time window

**Output:**

- `ROC`

**Possible usages:**

```clojure
(println (ROC "AAPL" 20)
```

<br>

### `RSI`

This function calculates the Relative Strength Index (RSI) indicator.

**Parameters:**

- `tic` - the name of the ticker
- `n` - time window

**Output:**

- `RSI`


**Possible usages:**

```clojure
(RSI "AAPL" 20)
```

<br>

## Volatility indicators
### `sd-last-n-days`

This function calculates standard deviation of a stock for the last n days.

**Parameters:**

- `tic` - the name of the ticker
- `n` - time window

**Possible usages:**

```clojure
(println (sd-last-n-days "AAPL" 20))
```
<br>

### `ATR`

This function calculates standard deviation of a stock for the last n days.

**Parameters:**

- `tic` - the name of the ticker
- `mode` - "lazy" or "non-lazy"
- `prev-atr` - previous ATR value
- `n` - average of the daily TR values for the last n days

**Possible usages:**

```clojure
(let [prev-atr (Double/parseDouble (get (first (get-prev-n-days :PRC 1 "OMFGA")) :PRC))]
    (println (ATR "OMFGA" "non-lazy" prev-atr 10))
    )
```
<br>


### `keltner-channel`

This function calculates the Keltner Channel value of a stock for the last n days.

**Parameters:**

- `tic` - the name of the ticker
- `mode` - "lazy" or "non-lazy"
- `window` - time window
- `prev-atr` - previous ATR value

**Possible usages:**

```clojure
(let [low-price (Double/parseDouble (get-by-key "OMFGA" :BIDLO "non-lazy"))
      high-price (Double/parseDouble (get-by-key "OMFGA" :ASKHI "non-lazy"))
      prev-atr (- high-price low-price)]
  (println (keltner-channel "OMFGA" "non-lazy" 10 prev-atr))
  )
```
<br>

## Other indicators
### `force-index`

This function calculates the force index of a stock for the last n days.

**Parameters:**

- `tic` - the name of the ticker
- `mode` - "lazy" or "non-lazy"
- `window` - time window

**Possible usages:**

```clojure
(force-index "OMFGA" "non-lazy" 20)
```
<br>