(ns clojure-backtesting.counter
    (:require   [java-time :as t]
                [clojure.string :as str]
                [clojure-backtesting.parameters :refer :all]
                [clojure-backtesting.data :refer :all]))

(def date (atom nil))

(defn get-date
  []
  (deref date))

(defn init-date
  "Initialize the current date as input date.\n
   If the input not in dataset, will go to the closest smaller date (if any, otherwise closest larger date).\n
   Return the resultant date."
  [_date]
  ;; date input format checking
  (if-let [_date (first (first (rsubseq data-files <= _date)))]
    (reset! date _date)
    (if-let [_date (first (first (subseq data-files > _date)))]
      (reset! date _date)
      (throw (Exception. "Input date is wrong"))))
  (reset-daily-var)
  (get-date))

(defn get-next-date
  "Returns the next date without actually moving the counter."
  []
  (first (first (subseq data-files > (get-date)))))

(defn get-prev-n-date
  "Returns the date of the previous n valid days.\n
   E.g. prev 1 of 1973-02-04 may be 1973-02-03 or 1973-02-02 or even 1973-02-01."
  [n]
  (assert (> n 0) "n should be larger than 0.")
  (let [tmp (take n (rsubseq data-files < (get-date)))]
    (if (= n (count tmp))
      (first (last tmp))
      nil))
  )

;; (defn get-prev-n-dates
;;   "Returns the dates of the previous n valid days (in descending order, including today).\n
;;    E.g. prev 3 of 1973-02-04 are 1973-02-03, 1973-02-02, and 1973-02-01."
;;   [n]
;;   (assert (> n 0) "n should be larger than 0.")
;;   (let [tmp (take n (rsubseq data-files < (get-date)))]
;;     (if (= n (count tmp))
;;       (first (last tmp))
;;       nil)))

;; (defn next-date
;;   []
;;   (if-let [_date (get-next-date)]
;;     (do
;;       (reset! date _date)
;;       (if (= (deref tics-tomorrow) nil)
;;         (reset! tics-today nil)
;;         (do
;;           (reset! tics-today (deref tics-tomorrow))
;;           (reset! tics-tomorrow nil)))
;;       _date)
;;     (do
;;       (reset! tics-today nil)
;;       (reset! tics-tomorrow nil)
;;       nil))
;;   ;; todo
;;   )


;; ========= deprecated codes =============

;This namespace defines the program counter aka date of the program. 
;; (def date (atom (t/local-date 2020 11 24)))
;; ;; (def available-tics (atom {}))

;; (defn look-ahead-i-days
;; 	;;return date
;; 	;;here the format of the input date should be:
;; 	;;year-month-day
;;  	"This function will return the date i days after the given date."
;; 	[date i]
;; 	(let [[year month day] (map parse-int (str/split date #"-"))]
;;     (t/format "yyyy-MM-dd" (t/plus (t/local-date year month day) (t/days i)))))

;; (defn look-i-days-ago
;;   "This function is the opposite of look-ahead-n-days."
;;   [date n]
;;   (let [[year month day] (map parse-int (str/split date #"-"))]
;;     (t/format "yyyy-MM-dd" (t/minus (t/local-date year month day) (t/days n)))))

;; (defn init-date
;;     "This function init the counter by a string"
;;     [_date]
;;     (let [[year month day](map parse-int (str/split _date #"-"))]
;;         (reset! date (t/local-date year month day)))
;;     (def cur-reference (atom [0 (deref data-set)]))
;;     )

;; (defn set-date
;;   "This function sets the date with a string"
;;   [_date]
;;   (reset! date (t/local-date "yyyy-MM-dd" _date)))

;; (defn get-date
;;     []
;;     (t/format "yyyy-MM-dd" (deref date)))

;; (defn- search-next-date
;; "This function tells us whether some date is in the dataset"
;;     [date dataset]
;;     (loop [count (nth dataset 0) remaining (nth dataset 1)]
;;     (if (empty? remaining)
;;       (if (not= 0 (nth dataset 0))
;;         (loop [count 0 remaining (take (nth dataset 0) (deref data-set))]
;;           (if (empty? remaining)
;;             false
;;             (let [first-line (first remaining)
;;                   next-remaining (rest remaining)]
;;               (if (= (get first-line :date) date)
;;                 (do
;;                   (reset! cur-reference [count next-remaining])
;;                   true)
;;                 (recur (inc count) next-remaining)))))
;;         false)
;;       (let [first-line (first remaining)
;;             next-remaining (rest remaining)]
;;         (if (= (get first-line :date) date)
;;           (do
;;             (reset! cur-reference [count next-remaining])
;;             true)
;;           (recur (inc count) next-remaining))))))

;; (defn maintain-tics
;;   "This function should be called either in init-portfolio or next-date"
;;   ([initial]
;;   ;traverse the whole dataset
;;    (def tics-info (atom {}))
;;    (loop [count 0 remaining (deref data-set) cur-permno nil start-date nil end-date nil num 0 reference nil]
;;      (if (empty? remaining)
;;        (do
;;          (if (not= cur-permno nil)
;;            (swap! tics-info assoc cur-permno {:start-date start-date :end-date end-date :pointer (atom {:num num :reference reference})}))
;;          (deref tics-info))
;;        (let [first-line (first remaining)
;;              next-remaining (rest remaining)]
;;          (if (not= cur-permno (get first-line TICKER-KEY))
;;            (do
;;              (if (not= cur-permno nil)
;;                (swap! tics-info assoc cur-permno {:start-date start-date :end-date end-date :pointer (atom {:num num :reference reference})}))
;;              (let [cur-permno (get first-line TICKER-KEY) start-date (get first-line :date) end-date (get first-line :date) num count reference remaining]
;;                (recur (inc count) next-remaining cur-permno start-date end-date num reference)))
;;            (if (= (get (first remaining) :date) (get-date))
;;              (recur (inc count) next-remaining cur-permno start-date (get first-line :date) count remaining)
;;              (recur (inc count) next-remaining cur-permno start-date (get first-line :date) num reference))))))
;;    (maintain-tics))
;;   ([]
;;    (reset! available-tics {})
;;    (loop [tics (keys (deref tics-info))]
;;      (if (empty? tics)
;;        nil
;;        (let [cur-permno (first tics) remaining (rest tics) cur-pointer (get  (get (deref tics-info) cur-permno) :pointer) cur-reference (get (deref cur-pointer) :reference) cur-num (get (deref cur-pointer) :num)]
;;        (if (= (get (first (rest cur-reference)) :date) (get-date))
;;          (do
;;            (swap! available-tics assoc cur-permno {:num (inc cur-num) :reference (rest cur-reference)})
;;            (reset! cur-pointer {:num (inc cur-num) :reference (rest cur-reference)}))
;;          (if (= (get (first cur-reference) :date) (get-date))
;;            (do
;;              (swap! available-tics assoc cur-permno (deref cur-pointer))
;;              )))
;;        (recur remaining))))))

;; (defn internal-next-date
;;   "This function increment the date counter to the next available date"
;;   []
;;     ;return the new date, if found
;;     ;return 0, if not found(exceed the finding limit)
;;   (if (not= (count (deref tics-info)) 0)
;;     (do
;;       (loop [date_ nil tics (keys (deref tics-info))]
;;         (if (empty? tics)
;;           (do
;;             (reset! date date_)
;;             (maintain-tics)
;;             (get-date))
;;           (let [cur-permno (first tics) remaining (rest tics) cur-reference (get (deref (get  (get (deref tics-info) cur-permno) :pointer)) :reference)]
;;             ;(println cur-permno)
;;             (if (= (get (first cur-reference) :date) (get-date))
;;               (if (= date_ nil)
;;                 (recur (t/local-date "yyyy-MM-dd" (get (first (rest cur-reference)) :date)) remaining)
;;                 (recur (t/min date_ (t/local-date "yyyy-MM-dd" (get (first (rest cur-reference)) :date))) remaining))
;;               (recur date_ remaining))))))
;;   (loop [i 1]
;;       (if (<= i MAXDISCONTINUITY)
;;             ;(println (look-ahead-i-days (get-date) i))
;;         (if (search-next-date (look-ahead-i-days (get-date) i) (deref cur-reference))
;;           (do
;;             (swap! date t/plus (t/days i))
;;             (get-date))
;;           (recur (inc i)))
;;         nil))))