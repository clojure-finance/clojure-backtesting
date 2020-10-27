(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]
			[clojure-backtesting.paremeters :refer :all]) ;; Useful for CSV handling
    )

;;This file is for ordering related functions

(def order_record (atom[]))
(def total_record (atom{}))

;;testing purpose
(def file1 "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv")
;;(def a (read-csv-row file1))

(defn look_ahead_i_days
	;;return date
	[date i]
	)

; (defn search_in_
; 	[date tic]
; 	(search_in_search date))

(defn search_in_order
	"This function try to retrieve the matching entry from the dataset"
	[date tic]
	;;date e.g. "DD/MM?YYYY"
	;;tic e.g. "AAPL"
	;;return [false 0 0] if no match
	;;return [true price reference] otherwise

	(loop [count 0 remaining (deref data-set)]
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
	[arg]
	(if (= (count arg) 3)
		(let [[date tic quantity] arg]
		(let [[match price reference] (search_in_order date tic)]
			(if match
				{:date date :tic tic :price price :quantity quantity :total "TBI" :reference reference}
				(println (format "The order request for date: %s, tic: %s, quantity: %d falses" date tic quantity)));;else
		))
	(if (= (count arg) 4)
		(let [[date tic quantity] arg]
		(let [[match price reference] (search_in_order date tic)]
			(if match
				{:date date :tic tic :price price :quantity "TBI" :total quantity :reference reference}
				(println (format "The order request for date: %s, tic: %s, quantity: %d falses" date tic quantity)));;else
		)))))

(defn order_internal
	"This is the main order function"
	([date tic quantity]
	;;@date date-and-time trading date
	;;@tic  trading ticker
	;;@quantity exact number to buy(+) or sell(-)
	(let [[match price reference] (search_in_order date tic)]
	;;(let [[match price reference] [true "10" 348]]
	(if match
		(do 
			(swap! order_record concat [{:date date :tic tic :price price :quantity quantity :total "TBI" :reference reference}]))
		(println (format "The order request for date: %s, tic: %s, quantity: %d fails" date tic quantity)))));;else
	;;atoms [{:date :tic :price :quan :total :reference}{}]

	([date tic quantity remaining]
	;;@date trading date
	;;@tic  trading ticker
	;;@quantity remaining quantity (either sell or buy to reach such quantity)
	;;remaining bool
	(if (= remaining true)
		(let [[match price reference] (search_in_order date tic)]
		;;(let [[match price reference] [true "10" 348]]
		(if match
			(do 
				(swap! order_record concat [{:date date :tic tic :price price :quantity "TBI" :total quantity :reference reference}]))
			(println (format "The order request for date: %s, tic: %s, quantity: %d falses" date tic quantity))))
	(order_internal date tic quantity)))

	([args]
	;;this is where parallel computing is needed.
	(swap! order_record concat (pmap order_parl args))
	(shutdown-agents));;needs more work
	)



