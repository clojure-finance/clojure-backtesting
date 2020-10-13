(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]) ;; Useful for CSV handling
    )

;;This file is for ordering related functions

(def order_record (atom[]))
(def total_record (atom{}))
;; Create initial portfolio with cash only (User input thei initial-capital)
(defn initiate_portfolio
  [init-capital]
  (def portfolio (atom {:cash init-capital}))
  (def portfolio_value (atom init-capital)))

(defn update_portfolio
  [tic quantity price]
  (if-not (contains? (deref portfolio) tic)
    (let [tot_val (* price quantity)]
      (do (swap! portfolio (fn [curr_port] (conj curr_port [tic {:price price :quantity quantity :tot_val tot_val}])))
      (swap! portfolio assoc :cash (- (:cash (deref portfolio)) tot_val))))
    (let [[tot_val qty] [(* price quantity) (get-in (deref portfolio) [tic :quantity])]]
      (do (swap! portfolio assoc tic [:price price :quantity (+ qty quantity) :tot_val (* price (+ qty quantity))])
      (swap! portfolio assoc :cash (- (:cash (deref portfolio)) tot_val)))))
      )

(defn search_in_order
	"This function try to retrieve the matching entry from the dataset"
	[date tic quantity]
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
