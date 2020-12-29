(ns clojure-backtesting.plot
  (:require [clojure-backtesting.data :refer :all]
            [clojupyter.misc.helper :as helper]
            [oz.notebook.clojupyter :as oz]
  ) ;; require all libriaries from core
)

;; Oz library for Clojure: https://github.com/metasoarous/oz

(defn plot
  "this is the function that allows the users to plot charts,"
  ([dataset series x y ]
  (oz/view!
    { :width 800 :height 500 ;adjust the graph size
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
    { :width 800 :height 500 ;adjust the graph size
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





