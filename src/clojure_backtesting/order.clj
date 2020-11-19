(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]
			[clojure-backtesting.parameters :refer :all]
			[clojure.string :as str]
			[java-time :as t]) ;; Useful for CSV handling
    )

;;This file is for ordering related functions
;;testing purpose
;(def file1 "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv")
;;(def a (read-csv-row file1))

(defn look_ahead_i_days
	;;return date
	;;here the format of the input date should be:
	;;year-month-day
	[date i]
	(let [[year month day] (map parse-int (str/split date #"-"))]
    (t/format "yyyy-MM-dd" (t/plus (t/local-date year month day) (t/days i)))))

;; helper function, natural logarithm
(defn log_10 [n]
  (/ (Math/log n) (Math/log 10)))

;; for each security:
;; add col 'cum_ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum_ret)
(defn add_aprc [data]
  "This function adds the adjusted price column to the dataset."
  ; get price on 1st day
  (def initial_price 0)
  (def cum_ret 0)
  (def curr_ticker "DEFAULT")
 ; traverse row by row in dataset
 (map (fn [line]
        (let [;line-new (select-keys line [:date :TICKER :PRC :RET])
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
          ;(def log_ret (Math/log (+ 1 ret))) ; natural log
          (def log_ret (log_10 (+ 1 ret))) ; log base 10
          (def cum_ret (+ cum_ret log_ret))
          (def aprc (* initial_price (Math/pow Math/E cum_ret)))
          (assoc line :INIT_PRICE initial_price :APRC aprc :LOG_RET log_ret :CUM_RET cum_ret)
          ; (swap! data-set_adj conj (assoc line-new "APRC" aprc "LOG_RET" log_ret "CUM_RET" cum_ret))
        )
      )
    data
 )
)

;;testing purpose, delete afterwards
;(def testfile1 (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv"))

;; Search_date function
(defn search_date
  "This function tries to retrieve the matching entry from the dataset."
  [date tic]
  ;;date e.g. "DD/MM?YYYY"
  ;;tic e.g. "AAPL"
  ;;return [false 0 0] if no match
  ;;return [true price reference] otherwise
  
  (loop [count 0 remaining (deref data-set)] ;(original line)
  ;(loop [count 0 remaining testfile1] 				;testing line, change the data-set to CRSP
    (if (empty? remaining)
      [false 0 0]
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (if (and (= (get first-line :date) date) ;;amend later if the merge data-set has different keys (using the keys in CRSP now)
                 (= (get first-line :TICKER) tic) ;;amend later if the merge data-set has different keys(using the keys in CRSP now)
                 )
          (let [price (get first-line :PRC)
                aprc (get first-line :APRC)] ;;amend later if you want to use the adjusted price instead of the closing price
            [true price aprc count])
          (recur (inc count) next-remaining)))))
)


;; Search in Order function
(defn search_in_order
	"This function turns the order processed date"
[date tic]
;;date e.g. "DD/MM?YYYY"
;;tic e.g. "AAPL"
;;return [false "No match date" 0 0 0] if no match
;;return [true T+1_date price aprc reference] otherwise

	(let [[match price aprc reference] (search_date date tic)]
		;;(let [[match price reference] [true "10" 348]]
		(if match
			(loop [i 1]
				(if (<= i MAXLOOKAHEAD)
					(let [t_1_date (look_ahead_i_days date i) 
						[b p aprc r] (search_date t_1_date tic)]
						(if b
							[b t_1_date (Double/parseDouble p) aprc r]
							(recur (inc i))						
						)
					)
					[false (str "No appropriate order date after looking ahead " MAXLOOKAHEAD " days") 0 0 0]
				)				
			)
			[false "No such date or ticker in the dataset" 0 0 0]
		)
	)
)

;; Create initial portfolio with cash only (User input thei initial-capital)
(defn init_portfolio

  [date init-capital] ;; the dataset is the filtered dataset the user uses, as we need the number of days from it
  ;; example: portfolio -> {:cash {:tot_val 10000} :"AAPL" {:price 400 :aprc adj_price :quantity 100 :tot_val 40000}}
  ;; example: portfolio_value {:date 1980-12-16 :tot_value 50000 :daily_ret 0}
  (def order_record (atom []))
  (def init-capital init-capital)
  (def num-of-tradays (count (deref data-set)))
  (def portfolio (atom {:cash {:tot_val init-capital}}))
  (def portfolio_value (atom [{:date date :tot_value init-capital :daily_ret 0}]))
)

;; Update the portfolio when placing an order
(defn update_portfolio
  [date tic quantity price aprc]
  (println aprc)
  ;; check whether the portfolio already has the security
  (if-not (contains? (deref portfolio) tic) 
    (let [tot_val (* aprc quantity) tot_val_real (* price quantity)]
      (do 
        (swap! portfolio (fn [curr_port] (conj curr_port [tic {:price price :aprc aprc :quantity quantity :tot_val tot_val}])))
        (swap! portfolio assoc :cash {:tot_val (- (get-in (deref portfolio) [:cash :tot_val]) tot_val_real)})
      )
    )
    ;; if already has it, just update the quantity
    (let [[tot_val qty] [(* aprc quantity) (get-in (deref portfolio) [tic :quantity])] tot_val_real (* price quantity)] 
      (do 
        (swap! portfolio assoc tic {:quantity (+ qty quantity) :tot_val (* aprc (+ qty quantity))})
        (swap! portfolio assoc :cash {:tot_val (- (get-in (deref portfolio) [:cash :tot_val]) tot_val_real)})
      )
    )
  )
  
  ;; then update the price & aprc of the securities in the portfolio
  (doseq [[ticker _] (deref portfolio)] 
    (if (= ticker tic)
      (let [qty_ticker (get-in (deref portfolio) [ticker :quantity])]
        (do (swap! portfolio assoc ticker {:price price :aprc aprc :quantity qty_ticker :tot_val (* aprc qty_ticker)})))
    )
  )
  ;; update the portfolio_value vector which records the daily portfolio value
  (let [[tot_value prev_value] [(reduce + (map :tot_val (vals (deref portfolio)))) (:tot_value (last (deref portfolio_value)))]] 
    (if (not= prev_value 0.0) ; check division by zero
      (let [ret (Math/log (/ tot_value prev_value))]
        (do (swap! portfolio_value (fn [curr_port_val] (conj curr_port_val {:date date :tot_value tot_value :daily_ret ret}))))
      )
      (let [ret 0.0]
        (do (swap! portfolio_value (fn [curr_port_val] (conj curr_port_val {:date date :tot_value tot_value :daily_ret ret}))))
      )
    )
  )
)

(defn total_cal
  "this function returns the remaining total stock of a tic"
  [date tic])

; (defn order_parl
;   "This is the parellel ordering function"
;   [arg]
;   (if (= (count arg) 3)
;     (let [[date tic quantity] arg]
;       (let [[match adj_date price adj_price reference] (search_in_order date tic)]
;         (if match
;           (do
;             (update_portfolio adj_date tic quantity price adj_price)
;             {:date adj_date :tic tic :price price :quantity quantity :total "TBI" :reference reference})
;           (do
;             (println (format "The order request on date: %s, tic: %s, quantity: %d falses" date tic quantity))
;             (println (format "Failure reason: %s" adj_date)));;else
;         ))))
;     (if (= (count arg) 4)
;       (let [[date tic quantity] arg]
;         (let [[match adj_date price adj_price reference] (search_in_order date tic)]
;           (if match
;             (do
;               (update_portfolio adj_date tic quantity price adj_price)
;               {:date adj_date :tic tic :price price :quantity "TBI" :total quantity :reference reference})
;             (do
;               (println (format "The order request on date: %s, tic: %s, quantity: %d falses" date tic quantity))
;               (println (format "Failure reason: %s" adj_date));;else
;           ))))))
;[b t_1_date p aprc r]

(defn order_parl
  "This is the parellel ordering function"
  [arg]
  (if (= (count arg) 3)
    (let [[date tic quantity] arg]
      (let [[match adj_date price adj_price reference] (search_in_order date tic)]
        ;;(let [[match price reference] [true "10" 348]]
        (if match
          (do
            (let [total (
              cond 
              (= (get (get (deref portfolio) tic) :quantity) nil) 0
              :else (get (get (deref portfolio) tic) :quantity)) 
              cash (get (get (deref portfolio) :cash) :tot_val)]
              (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
              (do
              (update_portfolio adj_date tic quantity price adj_price)
              {:date adj_date :tic tic :price price :quantity quantity :total (+ total quantity) :reference reference})
              (do 
                (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
                (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell."))))))
          (do 
            (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
            (println (format "Failure reason: %s." adj_date)))))))
  (if (= (count arg) 4)
    (let [[date tic quantity remaining] arg]
      (if (= remaining true)
        (let [[match adj_date price adj_price reference] (search_in_order date tic)]
        ;;(let [[match price reference] [true "10" 348]]
        (if match
          (let [total (
            cond 
              (= (get (get (deref portfolio) tic) :quantity) nil) 0
              :else (get (get (deref portfolio) tic) :quantity)) 
            cash (get (get (deref portfolio) :cash) :tot_val)]
            (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
            (do
            (update_portfolio adj_date tic (- quantity total) price adj_price)
            {:date adj_date :tic tic :price price :quantity (- quantity total) :total quantity :reference reference})
            (do 
              (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
              (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell.")))))
          (do
            (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
            (println (format "Failure reason: %s." adj_date)))))
        (order_parl [date tic quantity])))))

(defn order_internal
	"This is the main order function"
	([date tic quantity]
	;;@date date-and-time trading date
	;;@tic  trading ticker
	;;@quantity exact number to buy(+) or sell(-)
	(let [[match adj_date price adj_price reference] (search_in_order date tic)]
	;;(let [[match price reference] [true "10" 348]]
	(if match
    (do
      (let [total (
        cond 
        (= (get (get (deref portfolio) tic) :quantity) nil) 0
        :else (get (get (deref portfolio) tic) :quantity)) 
        cash (get (get (deref portfolio) :cash) :tot_val)]
        (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
        (do
        (update_portfolio adj_date tic quantity price adj_price)
        (swap! order_record concat [{:date adj_date :tic tic :price price :quantity quantity :total (+ total quantity) :reference reference}]))
        (do 
          (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
          (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell."))))))
    (do 
      (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
      (println (format "Failure reason: %s." adj_date))))));;else
	;;atoms [{:date :tic :price :quan :total :reference}{}]

	([date tic quantity remaining]
	;;@date trading date
	;;@tic  trading ticker
	;;@quantity remaining quantity (either sell or buy to reach such quantity)
	;;remaining bool
	(if (= remaining true)
		(let [[match adj_date price adj_price reference] (search_in_order date tic)]
		;;(let [[match price reference] [true "10" 348]]
		(if match
      (let [total (
        cond 
          (= (get (get (deref portfolio) tic) :quantity) nil) 0
          :else (get (get (deref portfolio) tic) :quantity)) 
        cash (get (get (deref portfolio) :cash) :tot_val)]
        (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
        (do
        (update_portfolio adj_date tic (- quantity total) price adj_price)
        (swap! order_record concat [{:date adj_date :tic tic :price price :quantity (- quantity total) :total quantity :reference reference}]))
        (do 
          (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
          (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell.")))))
      (do
        (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
        (println (format "Failure reason: %s." adj_date)))))
	  (order_internal date tic quantity)))

	([args]
	;;this is where parallel computing is needed.
	(swap! order_record concat (doall (pmap order_parl args)))
	(shutdown-agents));;needs more work
	)



