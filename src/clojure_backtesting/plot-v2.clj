(ns clojure-backtesting.plotvnew
  (:require [clojure-backtesting.data :refer :all]
            [clojupyter.misc.helper :as helper]
  ) ;; require all libriaries from core
  (:use clojure.pprint)
)

(helper/add-dependencies '[metasoarous/oz "1.6.0-alpha31"])
(require '[oz.notebook.clojupyter :as oz])


;;This file is for plotting related functions


;;"Simple Line Chart based on Vega-Lite/ Vega. 
;; more customization can be found here: https://vega.github.io/vega-lite/docs/line.html

; Require the following data type for input
; vector of maps [{:x-axis title value, :series_name "ABC", :y-axis title value}{......}]

  ; e.g.
  ; [
  ; {:date 0, :item "IBM", :price 5.0} 
  ; {:date 1, :item "IBM", :price 6.192962712629476}
  ; .....
  ; {:date 10, :item "IBM", :price 8.30037210271847}
  ; {:date 0, :item "AAPL", :price 7.0} 
  ; .....
  ; {:date 0, :item "TSLA", :price 10.0} 
  ; .....
  ; ]

  ; portfolio_value data type {:date xxxx :tot_value 50000 :daily_ret 0}


;testing data

(def singlestock (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/plotting-testing-data-singlestock.csv")

(def multistocks (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/plotting-testing-data.csv")

;;examples in jupyter notebook

(defn plot
  "this is the function that allows the users to plot charts,"
  [dataset]
  (oz/view!
    { :width 500 :height 500 ;adjust the graph size
      :data      {:values dataset}
      :encoding  {:x {:field "date" :type "temporal"};"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
                  :y {:field "price" :type "quantitative"}
                  :color {:field "tic" :type "nominal"} ;if it's only 1 line, no need this
                 } 
      :mark "line"
    }
  )
)


(defn plot-dual-axis

  "this is the function that allows the users to plot charts in dual axis"
  
  [dataset y1 y2] ;y1 & y2 should be key for values to be plotted, e.g. :tot_value or :daily_ret

  (oz/view!
    { :width 500 :height 500 ;adjust the graph size
      :data      {:values dataset}
      :encoding  {:x {:field "date" :type "temporal"} ;"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
                  :color {:field "tic" :type "nominal"} ;if it's only 1 line, no need this
                 } 
      :layer     [
                 {:mark {:stroke "#800000" :type "line"}
                  :encoding {:y {:field y1 :type "quantitative"}}
                 } 
                 {:mark {:stroke "#000080" :type "line"}
                 :encoding {:y {:field y2 :type "quantitative"}}
                }]
      :resolve   {:scale {:y "independent"}}
    }
  )
)

