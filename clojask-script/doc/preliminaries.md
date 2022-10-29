{:title "Preliminaries"
 :date "2021-01-26"
 :layout :post
 :tags  []
 :toc true}

---

## Example Dataset Format

<br>

Both `Compustat-extract.csv` and `CRSP-extract.csv` are extracts of the dataset provided by the Center for Research in Security Prices, LLC.

**Link to official documentation**: [http://crsp.org/files/CCM_Database_SAS_ASCII_R_FileFormats.pdf](http://crsp.org/files/CCM_Database_SAS_ASCII_R_FileFormats.pdf)

---

## Standard Dataset Format

<br>

**Requirements:**

1. In csv file format
2. **Row based:** rows are instances and columns are parameters. The first line is the header for each column.
3. Different tickers are uniquely identified by the Ticker ID or Name; and the same ticker should have the same ID or name.
4. Same tickers are stored in clusters or blocks. The row numbers are continuous.
5. Dates within each cluster or block is in order from the earliest to the lastest date.
6. The dataset is less than 100MB.

**Some remarks:**

1. The date in init-portfolio must be a valid date in the dataset.
2. Ticker should have a unique identifier named TICKER, which should not be null.

---

## Large Dataset Format

<br>

**Requirements:**

1. In csv file format
2. **Row based:** rows are instances and columns are parameters. The first line is the header for each column.
3. Different tickers are uniquely identified by the Ticker ID or Name. And the same ticker has the same ID or name.
4. Tickers on the same date are stored in clusters or blocks.
5. Dates within each cluster or block is stored by alphabetical order of names.
6. The dataset is more than 100MB.

<br>

**Some remarks:**

1. The date in init-portfolio must be a valid date in the dataset.
2. Ticker should have a unique identifier named TICKER, which should not be null

---------------

## Column Names

### CRSP dataset

- `:PRC` : closing price
- `:OPENPRC` : opening price
- `:date` : date
- `:TICKER` : ticker name
- `:APRC` : adjusted price

**<span style="color:red">Remark:</span>**

- The trading price is by default set to be closing price.

You can change it to opening price by changing the parameter.clj in `src/`:

```clojure
(def PRICE-KEY :OPRNPRC)
```

<br>

### Compustat dataset

- `datadate`: quarterly date

