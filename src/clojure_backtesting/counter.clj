(ns clojure-backtesting.counter
    (:require   [java-time :as t]
                [clojure.string :as str]
                [clojure-backtesting.parameters :refer :all]
                [clojure-backtesting.data :refer :all]))

;This namespace defines the program counter aka date of the program. 
(def date (atom (t/local-date 2020 11 24)))

(defn look-ahead-i-days
	;;return date
	;;here the format of the input date should be:
	;;year-month-day
	[date i]
	(let [[year month day] (map parse-int (str/split date #"-"))]
    (t/format "yyyy-MM-dd" (t/plus (t/local-date year month day) (t/days i)))))


(defn init-date
    "This function init the counter by a string"
    [_date]
    (let [[year month day](map parse-int (str/split _date #"-"))]
        (reset! date (t/local-date year month day)))
    (def cur-reference (atom [0 (deref data-set)]))
    )

(defn get-date
    []
    (t/format "yyyy-MM-dd" (deref date)))

(defn- search-next-date
"This function tells us whether some date is in the dataset"
    [date dataset]
    (loop [count (nth dataset 0) remaining (nth dataset 1)]
    (if (empty? remaining)
      (if (not= 0 (nth dataset 0))
        (loop [count 0 remaining (take (nth dataset 0) (deref data-set))]
          (if (empty? remaining)
            false
            (let [first-line (first remaining)
                  next-remaining (rest remaining)]
              (if (= (get first-line :date) date)
                (do
                  (reset! cur-reference [count next-remaining])
                  true)
                (recur (inc count) next-remaining)))))
        false)
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (if (= (get first-line :date) date)
          (do
            (reset! cur-reference [count next-remaining])
            true)
          (recur (inc count) next-remaining))))))

(defn maintain-tics
  "This function should be called either in init-portfolio or next-date"
  ([initial]
  ;traverse the whole dataset
   (def tics-info (atom {}))
   (def available-tics- (atom {}))
   (loop [count 0 remaining (deref data-set) cur-tic nil start-date nil end-date nil num 0 reference nil]
     (if (empty? remaining)
       (do
         (if (not= cur-tic nil)
           (swap! tics-info assoc cur-tic {:start-date start-date :end-date end-date :pointer (atom {:num num :reference reference})}))
         (deref tics-info))
       (let [first-line (first remaining)
             next-remaining (rest remaining)]
         (if (not= cur-tic (get first-line :TICKER))
           (do
             (if (not= cur-tic nil)
               (swap! tics-info assoc cur-tic {:start-date start-date :end-date end-date :pointer (atom {:num num :reference reference})}))
             (let [cur-tic (get first-line :TICKER) start-date (get first-line :date) end-date (get first-line :date) num count reference remaining]
               (recur (inc count) next-remaining cur-tic start-date end-date num reference)))
           (if (= (get (first remaining) :date) (get-date))
             (recur (inc count) next-remaining cur-tic start-date (get first-line :date) count remaining)
             (recur (inc count) next-remaining cur-tic start-date (get first-line :date) num reference))))))
   (maintain-tics))
  ([]
   (reset! available-tics- {})
   (loop [tics (keys (deref tics-info))]
     (if (empty? tics)
       nil
       (let [cur-tic (first tics) remaining (rest tics) cur-pointer (get  (get (deref tics-info) cur-tic) :pointer) cur-reference (get (deref cur-pointer) :reference) cur-num (get (deref cur-pointer) :num)]
       (if (= (get (first (rest cur-reference)) :date) (get-date))
         (do
           (swap! available-tics- assoc cur-tic {:num (inc cur-num) :reference (rest cur-reference)})
           (reset! cur-pointer {:num (inc cur-num) :reference (rest cur-reference)}))
         (if (= (get (first cur-reference) :date) (get-date))
           (do
             (swap! available-tics- assoc cur-tic (deref cur-pointer))
             )))
       (recur remaining))))))
     

(defn next-date
  "This function increment the date counter to the next available date"
  []
    ;return the new date, if found
    ;return 0, if not found(exceed the finding limit)
  (if (not= (resolve 'tics-info) nil)
    (do
      (loop [date_ nil tics (keys (deref tics-info))]
        (if (empty? tics)
          (do
            (reset! date date_)
            (maintain-tics)
            (get-date))
          (let [cur-tic (first tics) remaining (rest tics) cur-reference (get (deref (get  (get (deref tics-info) cur-tic) :pointer)) :reference)]
            ;(println cur-tic)
            (if (= (get (first cur-reference) :date) (get-date))
              (if (= date_ nil)
                (recur (t/local-date "yyyy-MM-dd" (get (first (rest cur-reference)) :date)) remaining)
                (recur (t/min date_ (t/local-date "yyyy-MM-dd" (get (first (rest cur-reference)) :date))) remaining))
              (recur date_ remaining))))))
  (loop [i 1]
      (if (<= i MAXDISCONTINUITY)
            ;(println (look-ahead-i-days (get-date) i))
        (if (search-next-date (look-ahead-i-days (get-date) i) (deref cur-reference))
          (do
            (swap! date t/plus (t/days i))
            (get-date))
          (recur (inc i)))
        0))))