(ns clojure-backtesting.plot
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
; vector of maps [{:x-axis title value, :series-name "ABC", :y-axis title value}{......}]

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

  ; portfolio-value data type {:date xxxx :tot-value 50000 :daily-ret 0}


;testing data

(def singlestock (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/plotting-testing-data-singlestock.csv"))

(def multistocks (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/plotting-testing-data.csv"))

;;examples in jupyter notebook

(defn plot
  "this is the function that allows the users to plot charts,"
  ([dataset series x y ]
  (oz/view!
    { :width 500 :height 500 ;adjust the graph size
      :data      {:values dataset}
      :encoding  {:x {:field x :type "temporal"};"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
                  :y {:field y :type "quantitative"}
                  :color {:field series :type "nominal"} ;if it's only 1 line, no need this
                 } 
      :mark "line"
    }
  ))
  
  ([dataset series x y1 y2] ;y1 & y2 should be key for values to be plotted, e.g. :tot-value or :daily-ret

  (oz/view!
    { :width 500 :height 500 ;adjust the graph size
      :data      {:values dataset}
      :encoding  {:x {:field x :type "temporal"} ;"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
                  :color {:field series :type "nominal"} ;if it's only 1 line, no need this
                 } 
      :layer     [
                 {:mark {:type "line"}
                  :encoding {:y {:field y1 :type "quantitative"}
                            :color {:field series :type "nominal"}}
                 } 
                 {:mark {:type "line"}
                 :encoding {:y {:field y2 :type "quantitative"}
                            :color {:field series :type "nominal"}}
                }]
      :resolve   {:scale {:y "independent"}}
    }
  ))
)

