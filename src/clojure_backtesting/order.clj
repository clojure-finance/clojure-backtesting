(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]) ;; Useful for CSV handling
    )

;;This file is for ordering related functions

(def order_record (atom[]))
(def total_record (atom{}))

;;
;;(defn test 
;; (println f0))


;;testing purpose
(def file1 "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv")
(def a (read-csv-row file1))

(defn search_in_order
	"This function try to retrieve the matching entry from the dataset"
	[date tic quantity]
	;;a is the data-set now, a is from the CRSP dataset, may have to change the keys below later
	;;date e.g. "DD/MM?YYYY"
	;;tic e.g. "AAPL"
	;;return [false 0 0] if no match
	;;return [true price reference] otherwise

	(loop [count 0 remaining a]
        (if (empty? remaining)
          [false 0 0]
			(let [first-line (first remaining)
				next-remaining (rest remaining)]
				(if (and (= (get first-line :date) date) ;;amend later if the merge data-set has different keys
						(= (get first-line :TICKER) tic) ;;amend later if the merge data-set has different keys
					)
					(let [price (get first-line :PRC)]
						[true price count]
					)
					(recur (inc count) next-remaining)
				)
			)
	    )
	)	
)

(defn total_cal
	"this function returns the remaining total stock of a tic"
	[date tic]
	)

(defn order_parl
	"This is the parellel ordering function"
	([date tic quantity]
	(0))
	([date tic quantity remaining]
	(0)))

(defn order_iternal
	"This is the main order function"
	([date tic quantity]
	;;@date date-and-time trading date
	;;@tic  trading ticker
	;;@quantity exact number to buy(+) or sell(-)
	(let [[match price reference] (search_in_order date tic quantity)]
	(if match
		(do (swap! order_record concat [{:date date :tic tic :quantity quantity :total (total_cal date tic)}])))))
	;;atoms [({:date :tic :price :quan :total :reference}{})]

	([date tic quantity remaining]
	;;@date trading date
	;;@tic  trading ticker
	;;@quantity remaining quantity (either sell or buy to reach such quantity)
	;;remaining bool
	())

	([args]
	;;this is where parallel computing is needed.
	(pmap order_parl args));;needs more work

	)



