
(ns clojure-backtesting.plot
  (:require [clojure-backtesting.data :refer :all]
            [com.hypirion.clj-xchart :as c]
  ) ;; require all libriaries from core

  (:use clojure.pprint)
)

    (def r (java.util.Random. 42))
 (defn test
   []
   (c/view
     (c/xy-chart
      {"Maxime" {:x (range 10)
                 :y (mapv #(+ % (* 3 (.nextDouble r)))
                          (range 10))}
       "Tyrone" {:x (range 10)
                 :y (mapv #(+ 2 % (* 4 (.nextDouble r)))
                          (range 0 5 0.5))}}
      {:title "Longest running distance"
       :x-axis {:title "Months (since start)"}
       :y-axis {:title "Distance"
                :decimal-pattern "##.## km"}
       :theme :matlab})))

;;This file is for plotting related functions

;;clj-xchart: line charts, scatter charts, area charts, bar charts, pie charts, donut charts
	;; https://github.com/hypirion/clj-xchart
	;; https://hypirion.github.io/clj-xchart/


;;"Simple X-Y Chart. Require a map for input
;;c/xy-chart {"Stock 1" [(X-axis vector1: all dates) (Y-axis vector2: all prices)]
;;            "Stock 2" [(X-axis vector1: all dates) (Y-axis vector2: all prices)]}

(defn plot-help

"this is to give an example on how the plotting function is being used"
[]

(println "Please use the following format for the plot function. An example has been done for you. If successfully executed, a pop-up window with the graph will be shown.")

(println "")


(println 

 "(plot {\"Expected rate\" {:x (range 10) :y (range 10) :style {:line-color :red}}
         \"Actual rate\"   {:x (range 10) :y (map #(+ % (rand-int 5) -2) (range 10)) :style {:line-color :blue}}}
         {:title \"This is the title of the chart\"
          :y-axis {:title \"This is the title for y axis\"}
          :x-axis {:title \"This is the title for x axis\"}
          :legend {:position :inside-ne}
          })"
)

(plot {"Expected rate" {:x (range 10) :y (range 10) :style {:line-color :red}}
         "Actual rate"   {:x (range 10) :y (map #(+ % (rand-int 5) -2) (range 10)) :style {:line-color :blue}}}
         {:title "This is the title of the chart"
          :y-axis {:title "This is the title for y axis"}
          :x-axis {:title "This is the title for x axis"}
          :legend {:position :inside-ne}
          })
 
)


(comment
  ;for documentation purpose only, a comprehensive function"
  (plot {"Line A" {:x (range 10) :y (range 10) :style {:line-color :red}}
         "Line B" {:x (range 10) :y (map #(+ % (rand-int 5) -2) (range 10)) :style {:line-color :blue}}}
         {:title "This is the title of the chart"
          :y-axis {:title "This is the title for y axis"}
          :x-axis {:title "This is the title for x axis"}
          ;;:legend {:position :inside-ne}
          })
)


(defn plot
  
  "this is the function that allows the users to plot charts,"
 
  [data title]
  (c/view
       (c/xy-chart data title)
  )
)



