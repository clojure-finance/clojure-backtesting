(ns clojure-backtesting.plot
  (:require [oz.core :as oz]))

;; Oz library for Clojure: https://github.com/metasoarous/oz

(defn- x-axis [x full-date]
  (if full-date
    {:field x :type "temporal" :timeUnit "yearmonthdate"}
    {:field x :type "temporal"}))

(defn plot
  "Line chart of `dataset` (a sequence of maps) with one line per value of
   `series`, `x` on the time axis and `y` (or `y1` and `y2` on independent
   axes) as values. `full-date` formats the axis as year-month-day."
  ([dataset series x y full-date]
   (oz/view!
    {:width 800 :height 500
     :data {:values dataset}
     :encoding {:x (x-axis x full-date)
                :y {:field y :type "quantitative"}
                :color {:field series :type "nominal"}}
     :mark "line"}))
  ([dataset series x y1 y2 full-date]
   (oz/view!
    {:width 800 :height 500
     :data {:values dataset}
     :encoding {:x (x-axis x full-date)
                :color {:field series :type "nominal"}}
     :layer [{:mark {:type "line"}
              :encoding {:y {:field y1 :type "quantitative"}
                         :color {:field series :type "nominal"}}}
             {:mark {:type "line"}
              :encoding {:y {:field y2 :type "quantitative"}
                         :color {:field series :type "nominal"}}}]
     :resolve {:scale {:y "independent"}}})))
