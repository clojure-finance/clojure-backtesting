## User Guide

### About the dataset

**[CRSP](https://connecthkuhk-my.sharepoint.com/:u:/g/personal/u35lyc_connect_hku_hk/ESQNElD-vL1KliKmOf57D2ABLCLYLHoc9wkJWhXUSTzCNw?e=DNaVWm)** is the official dataset for the backtester. New users are recommended to directly use this dataset to try out the functionalities of the backtester. Definitions for each column in CRSP can be found [here](https://crsp.org/files/CCM_Database_SAS_ASCII_R_FileFormats.pdf).

Of course, you can use your own dataset in the backtester. Follow this [guide]() on how to reform arbitrary dataset to be compatible with the backtester.

**[Compustat](https://connecthkuhk-my.sharepoint.com/:u:/g/personal/u35lyc_connect_hku_hk/Eddh3mmToBxCh7OlGx0R0-kB9G5a6Dq6xXW9dXXfHrn7OA?e=FQYfSl)** ...

Both datasets are available [here](https://connecthkuhk-my.sharepoint.com/:f:/g/personal/u35lyc_connect_hku_hk/EuAAauw1fuNFoDRav2D0J_EB6T_bbfkyNZ5ShN8Xw0cqhw?e=RtOcFL).

#### COLUMN NAMES

##### CRSP dataset

- `:PRC` : closing price
- `:OPENPRC` : opening price
- `:DATE` : date (yyyy-MM-dd)
- `:PERMNO` : ticker identifier
- `:APRC` : adjusted price

**Remark:**

- The trading price is by default set to be closing price.

You can change it to opening price by changing the parameter.clj in `src/`:

```
(def PRICE-KEY :OPRNPRC)
```