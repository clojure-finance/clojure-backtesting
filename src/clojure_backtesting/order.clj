(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]) ;; Useful for CSV handling
    )

;;This file is for ordering related functions

(def order_record (atom[]))
(def total_record (atom{}))
;;{:tic [date total] }

(defn search_in_order
	"This function try to retrieve the matching entry from the dataset"
	[date tic]
	;;return [false 0 0] if no match
	;;return [true price reference] otherwise
	([true 15 354])
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
	(let [[match price reference] (search_in_order date tic)]
	(if match
		(do (swap! order_record concat [{:date date :tic tic :quantity quantity :total (total_cal date tic)}])))))
	;;atoms [{:date :tic :price :quan :total :reference}{}]

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




