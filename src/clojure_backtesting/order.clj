(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]) ;; Useful for CSV handling
)

;;This file is for ordering related functions

(def order_record (atom []))
(def total_record (atom {}))
(def data-set_adj (atom []))

; exponential ;unused, could be removed later
(defn exp [x n]
(reduce * (repeat n x)))

;; for each security:
;; add col 'cum_ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum_ret)
;; 
; new version
(defn add_aprc []
  "This function adds the adjusted price column to the dataset."
  ; get price on 1st day
  (def initial_price 0)
  (def cum_ret 0)
  (def curr_ticker "DEFAULT")
 ; traverse row by row, compute log(1+RET)
 (map (fn [line]
      ;(println line)
        (let [line-new (select-keys line [:date :TICKER :PRC :RET])
              price (Double/parseDouble (get line :PRC))
              ret (Double/parseDouble (get line :RET))
              ticker (get line :TICKER)]
          (if (not= curr_ticker ticker)
              (do
                (def curr_ticker ticker)
                (def initial_price price)
                (def cum_ret 0)
              )
          )
          (def log_ret (Math/log (+ 1 ret)))
          (def cum_ret (+ cum_ret log_ret))
          (def aprc (* initial_price (Math/pow Math/E cum_ret)))
          (swap! data-set_adj conj (assoc line-new "APRC" aprc "LOG_RET" log_ret "CUM_RET" cum_ret))
          [line-new]
        )
      )
    (deref data-set))
)


(defn search_in_order
  "This function tries to retrieve the matching entry from the dataset."
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
            [true price count])
          (recur (inc count) next-remaining))))))

;; Create initial portfolio with cash only (User input thei initial-capital)


(defn init_portfolio

  [date init-capital dataset] ;; the dataset is the filtered dataset the user uses, as we need the number of days from it
  ;; NOTE: example changed for adjusted prices
  ;; 
  ;; example: portfolio -> {:cash {:tot_val 10000} :"AAPL" {:price 400 :aprc adj_price :quantity 100 :tot_val 40000}}
  ;; example: portfolio_value {:date xxxx :tot_value 50000 :daily_ret 0}
  (def init-capital init-capital)
  (def num-of-tradays (count dataset))
  (def portfolio (atom {:cash {:tot_val init-capital}}))
  (def portfolio_value (atom [{:date date :tot_value init-capital :daily_ret 0}])))

(defn update_portfolio
  ;; added aprc
  [date tic quantity price]

  (if-not (contains? (deref portfolio) tic) ;; check whether the portfolio already has the security
    (let [tot_val (* price quantity)]
      (do (swap! portfolio (fn [curr_port] (conj curr_port [tic {:price price :quantity quantity :tot_val tot_val}])))
          (swap! portfolio assoc :cash {:tot_val (- (get-in (deref portfolio) [:cash :tot_val]) tot_val)})))

    (let [[tot_val qty] [(* price quantity) (get-in (deref portfolio) [tic :quantity])]] ;; if already has it, just update the quantity
      (do (swap! portfolio assoc tic {:price price :quantity (+ qty quantity) :tot_val (* price (+ qty quantity))})
          (swap! portfolio assoc :cash {:tot_val (- (get-in (deref portfolio) [:cash :tot_val]) tot_val)}))))

  (doseq [[ticker _] portfolio] ;; then update the price of the securities in the portfolio
    (let [[match price_ticker _] (search_in_order date ticker)]
      (if match
        (let [qty_ticker (get-in (deref portfolio) [ticker :quantity])]
          (do (swap! portfolio assoc ticker  {:price price_ticker :quantity qty_ticker :tot_val (* price_ticker qty_ticker)}))))))

  (let [[tot_value prev_value] [(reduce + (map :tot_val (vals (deref portfolio)))) (:tot_value (last portfolio_value))]] ;; update the portfolio_vector vector which records the daily portfolio value
    (let [ret (Math/log (/ tot_value prev_value))]
      (do (swap! portfolio_value (fn [curr_port_val] (conj curr_port_val {:date date :tot_value tot_value :daily_ret ret})))))))

(defn total_cal
  "this function returns the remaining total stock of a tic"
  [date tic])

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
       (println (format "The order request for date: %s, tic: %s, quantity: %d falses" date tic quantity)))));;else
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