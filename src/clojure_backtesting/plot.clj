
(ns clojure-backtesting.rename
  (:require [clojure-backtesting.data :refer :all]) ;; require all libriaries from core
  (:require [com.hypirion.clj-xchart :as c]) ;;not sure how to require this lib

)

;;This file is for plotting related functions

;;Potential clojure plot functions libraries:
;;1) [Preferred] clj-xchart: line charts, scatter charts, area charts, bar charts, pie charts, donut charts
	;; https://github.com/hypirion/clj-xchart
	;; https://hypirion.github.io/clj-xchart/

;;2) [Preferred] vaga-lite via Oz: for data visualizaitons: multiple line charts, stack charts, 
  ;; https://github.com/metasoarous/oz
;;3) cljplot - more academic, single line chart, ACF/PACF chart, P-P plot, Q-Q plot
;;4) Incanter - more statistical, can plot time-series data, but more on histogram, hypothesis testing


(defn plot-help

"this function is for giving instructions to the users on other plot functions"

[]

(println "(c/view chart1 chart2) : to view a chart")
(println "(c/to-bytes my-chart :png) :  returns a byte array of the output")


)




(def chart
  (c/xy-chart {"Expected rate" [(range 10) (range 10)]
               "Actual rate" [(range 10) (map #(+ % (rand-int 5) -2) (range 10))]}))

(comment 

;;plot historical price
(defn price

"Simple X-Y Chart. Require a map for input

c/xy-chart {\"Stock 1\" [(X-axis vector1: all dates) (Y-axis vector2: all prices)]
 \"Stock 2\" [(X-axis vector1: all dates) (Y-axis vector2: all prices)] 

}"

[stock start-date end-date]

;;take the stock name, the start and end date to do the plotting 
;;1. get the list of dates 2. get the list of prices 3. c/xy-chart 4.c/view chart to display charts
;;multiple lines allowed, then need to store inputs/ use overload structure

;;need to use overload structure for several stocks

[stock1 stock2 start-date end-date]


)




(defn bar

"plot bar charts/ staccked bar charts to e.g. show asset changes"

)


(defn pie
"plot pie chart, to e.g. show portfolio return changes, portfolio composition "

)

(defn area
"plot area chart, to e.g. show stock prices movements "
)

;; functions below can move to core/ user as applications
(defn portfolio-graph
"plot out the porfolio changes/return over a period of time "
[start-date end-date]

pie (xxxx)
line (xxxx)

)

{defn stock-price
"plot out stock price for multiple stocks"
[stock-list ]

line (xxxx)
line (xxxx)


}

;;tech-analysis
{defn MA-x
"plot MAs or other technical indicators on the chart"
[days ] ;;MA20/ MA50 or multiple MAs?

line (xxxx)


}

;; see if you would like to add more technical analysis tools for the users to analysize it
{defn Boll

line (xxxx)
line (xxxx)

}

;;other plot functions can include
;;- visualize the winning rate over time
;;- visualize the accumulative gain/loss for multiple strategies
;;- etc.


)
