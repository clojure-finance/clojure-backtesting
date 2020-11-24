(ns clojure-backtesting.counter
    (:require   [java-time :as t]
                [clojure.string :as str]
                [clojure-backtesting.parameters :refer :all]
                [clojure-backtesting.data :refer :all]))

;This namespace defines the program counter aka date of the program. 
(def date (atom (t/local-date 2020 11 24)))

(defn look_ahead_i_days
	;;return date
	;;here the format of the input date should be:
	;;year-month-day
	[date i]
	(let [[year month day] (map parse-int (str/split date #"-"))]
    (t/format "yyyy-MM-dd" (t/plus (t/local-date year month day) (t/days i)))))


(defn init_date
    "This function init the counter by a string"
    [_date]
    (let [[year month day](map parse-int (str/split _date #"-"))]
        (reset! date (t/local-date year month day)))
    )

(defn get_date
    []
    (t/format "yyyy-MM-dd" (deref date)))

(defn search_next_date
"This function tells us whether some date is in the dataset"
    [date]
    (loop [count 0 remaining (deref data-set)]
    (if (empty? remaining)
      false
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (if (= (get first-line :date) date)
          true
          (recur (inc count) next-remaining))))))

(defn next_date
"This function increment the date counter to the next available date"
    []
    ;return the new date, if found
    ;return 0, if not found(exceed the finding limit)
    (loop [i 1]
        (if (<= i MAXDISCONTINUITY)
            ;(println (look_ahead_i_days (get_date) i))
            (if (search_next_date (look_ahead_i_days (get_date) i))
                (do
                    (swap! date t/plus (t/days i))
                    (get_date))
                (recur (inc i))						
            )
            0
        )				
    )	
)