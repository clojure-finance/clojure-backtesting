(ns clojure-backtesting.rename
  (:require [clojure-backtesting.data :refer :all]) ;; require all libriaries from core
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


(defn line

"plot single/ multiple line graphs, e.g. stock price changes, portfolio return changes"



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


)

{defn stock-price
"plot out stock price for multiple stocks"
[stock-list ]

}

;;tech-analysis
{defn MA-x
"plot MAs or other technical indicators on the chart"
[days ] ;;MA20/ MA50 or multiple MAs?
}

;; see if you would like to add more technical analysis tools for the users to analysize it
{defn Boll



}

;;other plot functions can include
;;- visualize the winning rate over time
;;- visualize the accumulative gain/loss for multiple strategies
;;- etc.