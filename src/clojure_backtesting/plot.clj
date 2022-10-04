(ns clojure-backtesting.plot
  (:require [clojure-backtesting.data :refer :all]
            [clojupyter.misc.helper :as helper]
            [oz.core :as oz]
            [oz.notebook.clojupyter :as oz-nb]
            [clojure.string :as str]))

;; Oz library for Clojure: https://github.com/metasoarous/oz

(def oz-view! (if (str/ends-with? *file* "clj")
                 oz/view!
                 oz-nb/view!))

;; Plotting function
(defn plot
  "This is the function for plotting line charts."
  ;; With only one y-axis value
  ([dataset series x y full-date]
    (if full-date
      (oz-view! ; with full-date
        { :width 800 :height 500 ; adjust the graph size
          :data      {:values dataset}
          :encoding  {:x {:field x :type "temporal" :timeUnit "yearmonthdate"};"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
                      :y {:field y :type "quantitative"}
                      :color {:field series :type "nominal"} ;if it's only 1 line, no need this
                    } 
          :mark "line"
        })
      (oz-view! ; w/o full date
        { :width 800 :height 500 ;adjust the graph size
          :data      {:values dataset}
          :encoding  {:x {:field x :type "temporal"} ;"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
                      :y {:field y :type "quantitative"}
                      :color {:field series :type "nominal"} ;if it's only 1 line, no need this
                    } 
          :mark "line"
        })))
  
  ;; With two y-axis values
  ([dataset series x y1 y2 full-date] ;y1 & y2 should be key for values to be plotted, e.g. :tot-value or :daily-ret
    (if full-date
      (oz-view!
        { :width 800 :height 500 ;adjust the graph size
          :data      {:values dataset}
          :encoding  {:x {:field x :type "temporal" :timeUnit "yearmonthdate"} ;"field" means the x-axis name, "type" asking what's the data type of x-axis values; choosing from "quantity"/"nominal"/"temporal"  
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
        })
      (oz-view!
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
        }))))





