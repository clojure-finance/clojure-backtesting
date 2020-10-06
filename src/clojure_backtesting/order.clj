(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]) ;; Useful for CSV handling
    )

;;This file is for ordering related functions


(defn order_iternal
	"This is the main order function"
	[date tic quantity]
	;;@date date-and-time trading date
	;;@tic  trading ticker
	;;@quantity exact number to buy(+) or sell(-)
	()
	atoms {:tic ({:date :price :quan :total :reference}{})}

	[date tic quantity remaining]
	;;@date trading date
	;;@tic  trading ticker
	;;@quantity remaining quantity (either sell or buy to reach such quantity)
	;;remaining bool
	()

	)



