# clojask-script

This script is used to transform an arbitrary stock related dataset to be compatible with the Clojure Backtester.

## Type of Inputs

### CRSP Type

This type of dataset will be used as the backbone dataset for the backtester that contains the stock market information. Therefore, it should have a few necessary columns containing the below information (names of these columns do not matter).

- security identifier
- date
- closing price / opening price
- Return

### Compustat Type

This type of dataset will be used as the supplementary dataset to the CRSP-type dataset. It can contain information such as fundamental data, economic data, etc. It should contain at least the essential columns below. The backtester currently supports only one Computstat-like dataset.

- security identifier
- date: effective until this date

#### How will CRSP merge with Compustat

It will be a forward rolling join between CRSP and Compustat, i.e. each row of CRSP will be merged with at most one Compustat row that has the latest earlier date (of course for the same security).

## Parameter Specification

You will mainly change parameters in `src/core.clj` file according to the nature of your file.

**Function** below means the functionality of each parameter.

**Expected Value** below indicates the acceptable value of each parameter.

| Parameter Name      | Function                                                     | Expected Value                                               |
| ------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| input-file          | Input file path☀️🌙                                            | String                                                       |
| output-dir          | Output directory (since the output will be a folder instead of a single file)☀️🌙 | String                                                       |
| type-of-dataset     | ☀️🌙                                                           | "CRSP" / "Compustat"                                         |
| security-identifier | Name of the security ID column☀️🌙                             | String, e.g. "PERMNO"                                        |
| date-identifier     | Name of the date column☀️🌙                                    | String, e.g. "datadate"                                      |
| data-format-string  | Format string for the date column☀️🌙                          | String, subject to the [java.util.Date format string](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html), e.g. "yyyy-MM-dd" |
| closing-price       | Name of the closing price column☀️*                           | Can be converted to double, e.g. "23.56295"                  |
| opening-price       | Name of the opening price column☀️*                           | Can be converted to double, e.g. "14.502954"                 |
| return-identifier   | Name of the column of daily change in the total value of an investment☀️ | String, e.g. "RET"                                           |
| daily-min-price     | Name of the column of lowest trading price during the day    | String, e.g. "BIDLO"                                         |
| daily-max-price     | Name of the column of highest trading price during the day   | String, e.g. "ASKHI"                                         |

☀️: Compulsory parameter for CRSP datasets

🌙: Compulsory parameters for Compustat datasets

\* Either `closing-price` and `opening-price` should be available for CRSP dataset.

## Run the Script

Just run the following command under the directory of `clojask-script`.

```
lein run
```

## The Resultant Dataset

The structure of the resultant dataset will be totally different from that of the input. However, you may not be worried about it because it is the backtester's job to read and decode it. 

During the decoding, each row is read as a hash map / dictionary, with key being the [keyword](https://clojuredocs.org/clojure.core/keyword) of the column names and value being the actual content (type String). **There are only three exceptions, closing price column will have key `:PRC`; opening price column will have key `:OPENPRC`; return column will have key `:RET`; all three columns have number values (instead of String).**
