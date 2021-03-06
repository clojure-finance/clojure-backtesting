(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math])
    )

(defn search-date
  "This function tries to retrieve the matching entry from the dataset."
  [date tic dataset]
  ;; date e.g. "DD/MM/YYYY"
  ;; tic e.g. "AAPL"
  ;; dataset

  ;; return [false 0 0] if no match
  ;; return [true price reference] otherwise
  
  (loop [count 0 remaining dataset] ;(original line)
  ;(loop [count 0 remaining testfile1] 	;testing line, change the data-set to CRSP
    (if (empty? remaining)
      [false 0 0]
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (if (and (= (get first-line :date) date) ;;amend later if the merge data-set has different keys (using the keys in CRSP now)
                 (= (get first-line :TICKER) tic) ;;amend later if the merge data-set has different keys(using the keys in CRSP now)
                 )
          (let [price (get first-line PRICE-KEY)
                aprc (get first-line :APRC)] ;;amend later if you want to use the adjusted price instead of the closing price
            [true price aprc count])
          (recur (inc count) next-remaining)))))
)


(defn search-in-order
  "This function turns the order processed date"
  [date tic dataset]
  ;; @date e.g. "DD/MM/YYYY"
  ;; @tic e.g. "AAPL"
  ;; @dataset

  ;; return [false "No match date" 0 0 0] if no match
  ;; return [true T+1-date price aprc reference] otherwise

  (if (and (not (deref lazy-mode)) (not= (count (deref available-tics)) 0))
    (if (and (not= -1 (.indexOf (keys (deref available-tics)) tic)) (not= (get (get (get (deref available-tics) tic) :reference) :date) (get (get (deref tics-info) tic) :end-date)))
      (let [t-1-date (get (first (rest (get (get (deref available-tics) tic) :reference))) :date)
            [b p aprc r] (search-date t-1-date tic (get (get (deref available-tics) tic) :reference))]
        (if b
          [b t-1-date (Double/parseDouble p) aprc r]))
      [false "No such date or ticker in the dataset or the dataset has reached the end" 0 0 0])
    (let [[match price aprc reference] (search-date date tic dataset)]
		;;(let [[match price reference] [true "10" 348]]
      (if match
        (if (deref lazy-mode) 
            [match date (Double/parseDouble price) aprc reference]
            (loop [i 1]
              (if (<= i MAXLOOKAHEAD)
                (let [t-1-date (look-ahead-i-days date i)
                      [b p aprc r] (search-date t-1-date tic dataset)]
                  (if b
                    [b t-1-date (Double/parseDouble p) aprc r]
                    (recur (inc i))))
                [false (str "No appropriate order date after looking ahead " MAXLOOKAHEAD " days") 0 0 0])))
        [false "No such date or ticker in the dataset" 0 0 0]))))


(defn- place-order
  "This private function does the basic routine for an ordering - update portfolio and return record."
  [date tic quantity price adj-price loan reference print direct]
  ;; (println loan)
  (if (not (deref terminated))
    (update-portfolio date tic quantity price adj-price loan)
  )
  (if print
    (println (format "Order: %s | %s | %f." date tic (double quantity))))
  (if direct
    (.write wrtr (format "%s,%s,%f\n" date tic (double quantity))))
  {:date date :tic tic :price price :aprc (format "%.2f" adj-price) :quantity quantity})


(defn order-internal
	"This is the main order function"
	[order-date tic quan remaining leverage dataset print direct]
	;; @date date-and-time trading date
	;; @tic  trading ticker
	;; @quantity exact number to buy(+) or sell(-)
	(let [[match date price adj-price reference] (search-in-order order-date tic dataset)] 
   ; Note that the date here may contain error information
	(if match
    (do
      (let [total (cond
                    (= (get (get (deref portfolio) tic) :quantity) nil) 0
                    :else (get (get (deref portfolio) tic) :quantity))
            cash (get (get (deref portfolio) :cash) :tot-val)
            quantity (cond 
                       remaining (- quan total)
                       :else quan)]
        (if (and (and (not= quantity 0) (not= quantity 0.0)) (not= quantity "special")) ;; ignore the empty order case
          (if (and (and (>= (+ total quantity) 0) (or (<= quantity 0) (>= cash (* adj-price quantity)))))
            (place-order date tic quantity price adj-price 0 reference print direct) ;loan is 0 here
            (do
              (if leverage
                (if (< (+ total quantity) 0)
                  (place-order date tic quantity price adj-price 0 reference print direct) ;This is the sell on margin case
                  (let [loan
                        (cond (<= cash 0)
                              (* quantity adj-price)
                              :else (- (* quantity adj-price) cash))]
                    (if (or (= INITIAL-MARGIN nil) (>= cash (* INITIAL-MARGIN (+ loan cash))))
                      (place-order date tic quantity price adj-price loan reference print direct)
                      (if print
                        (println (format "Order request %s | %s | %d fails due to initial margin exceeding." order-date tic quantity)))))) ;This is the buy on margin case
                (do
                  (println (format "Order request %s | %s | %d fails." order-date tic quantity))
                  (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell. Try to solve by enabling leverage."))))))
          (if (= quantity "special")
            (update-portfolio date tic 0 price adj-price 0)
            nil))))
    (do 
      (if (not= quan "special")
        (do
          (println (format "The order request %s | %s | %d fails." order-date tic quan))
          (println (format "Failure reason: %s." date))))))))

(defn order
  ([tic quantity & {:keys [remaining leverage dataset print direct] :or {remaining false leverage LEVERAGE dataset (deref data-set) print false direct true}}]
   (let [record (order-internal (get-date) tic quantity remaining leverage dataset print direct)]
     (if record
     (swap! order-record conj record)
       ) ;else
     ))
  ([arg] ;This function still needs to be developed for parallelisium
   (swap! order-record conj (doall (pmap order-internal arg))))
  )

(defn updateHoldingTickers
  "Update all the tickers in terms of portfolio"
  []
  (doseq [tic (rest (keys (deref portfolio)))]
    (order-internal (get-date) tic "special" false true (deref data-set) false false)))

