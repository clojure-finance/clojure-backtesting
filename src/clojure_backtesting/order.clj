(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]
			[clojure-backtesting.parameters :refer :all]
      [clojure-backtesting.counter :refer :all]
      [clojure.string :as str]
      [clojure.pprint :as pprint]
			[java-time :as t]
      [clojure.math.numeric-tower :as math])
    )

;;This file is for ordering related functions
;;testing purpose
;(def file1 "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv")
;;(def a (read-csv-row file1))


;; helper function, natural logarithm
(defn log-10 [n]
  (/ (Math/log n) (Math/log 10)))

;; for each security:
;; add col 'cum-ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum-ret)
(defn add-aprc 
  "This function adds the adjusted price column to the dataset."
  [data]
  ; get price on 1st day
  (def initial-price 0)
  (def cum-ret 0)
  (def curr-ticker "DEFAULT")
 ; traverse row by row in dataset
  (map (fn [line]
        (let [;line-new (select-keys line [:date :TICKER :PRC :RET])
              price (Double/parseDouble (get line :PRC))
              ret (Double/parseDouble (get line :RET))
              ticker (get line :TICKER)]
          (if (not= curr-ticker ticker)
              (do
                (def curr-ticker ticker)
                (def initial-price price)
                (def cum-ret 0)
              )
          )
          ;(def log-ret (Math/log (+ 1 ret))) ; natural log
          (def log-ret (log-10 (+ 1 ret))) ; log base 10
          (def cum-ret (+ cum-ret log-ret))
          (def aprc (* initial-price (Math/pow Math/E cum-ret)))
          (assoc line :INIT-PRICE initial-price :APRC aprc :LOG-RET log-ret :CUM-RET cum-ret)
          ; (swap! data-set-adj conj (assoc line-new "APRC" aprc "LOG-RET" log-ret "CUM-RET" cum-ret))
        )
      )
    data
  )
)

;;testing purpose, delete afterwards
;(def testfile1 (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv"))

;; Search-date function
(defn search-date
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
(defn search-in-order
	"This function turns the order processed date"
[date tic]
;;date e.g. "DD/MM/YYYY"
;;tic e.g. "AAPL"
;;return [false "No match date" 0 0 0] if no match
;;return [true T+1-date price aprc reference] otherwise

	(let [[match price aprc reference] (search-date date tic)]
		;;(let [[match price reference] [true "10" 348]]
		(if match
			(loop [i 1]
				(if (<= i MAXLOOKAHEAD)
					(let [t-1-date (look-ahead-i-days date i) 
						[b p aprc r] (search-date t-1-date tic)]
						(if b
							[b t-1-date (Double/parseDouble p) aprc r]
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
(defn init-portfolio

  [date init-capital] ;; the dataset is the filtered dataset the user uses, as we need the number of days from it
  ;; example: portfolio -> {:cash {:tot-val 10000} :"AAPL" {:price 400 :aprc adj-price :quantity 100 :tot-val 40000}}
  ;; example: portfolio-value {:date 1980-12-16 :tot-value 50000 :daily-ret 0}
  (init-date date)
  (def order-record (atom []))
  (def init-capital init-capital)
  ;(def num-of-tradays (count (deref data-set))) ;; wrong, to-be-deleted
  (def portfolio (atom {:cash {:tot-val init-capital}}))
  (def portfolio-value (atom [{:date date :tot-value init-capital :daily-ret 0.0}]))
)

;; Update the portfolio when placing an order
(defn update-portfolio
  [date tic quantity price aprc]
  (println aprc)
  ;; check whether the portfolio already has the security
  (if-not (contains? (deref portfolio) tic) 
    (let [tot-val (* aprc quantity) tot-val-real (* price quantity)]
      (do 
        (swap! portfolio (fn [curr-port] (conj curr-port [tic {:price price :aprc aprc :quantity quantity :tot-val tot-val}])))
        (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) tot-val-real)})
      )
    )
    ;; if already has it, just update the quantity
    (let [[tot-val qty] [(* aprc quantity) (get-in (deref portfolio) [tic :quantity])] tot-val-real (* price quantity)] 
      (do 
        (swap! portfolio assoc tic {:quantity (+ qty quantity) :tot-val (* aprc (+ qty quantity))})
        (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) tot-val-real)})
      )
    )
  )
  
  ;; then update the price & aprc of the securities in the portfolio
  (doseq [[ticker -] (deref portfolio)] 
    (if (= ticker tic)
      (let [qty-ticker (get-in (deref portfolio) [ticker :quantity])]
        (do (swap! portfolio assoc ticker {:price price :aprc aprc :quantity qty-ticker :tot-val (* aprc qty-ticker)})))
    )
  )
  ;; update the portfolio-value vector which records the daily portfolio value
  (let [[tot-value prev-value] [(reduce + (map :tot-val (vals (deref portfolio)))) (:tot-value (last (deref portfolio-value)))]] 
    (if (not= prev-value 0.0) ; check division by zero
      (let [ret (Math/log (/ tot-value prev-value))
            last-date (get (last (deref portfolio-value)) :date)
            last-index (- (count (deref portfolio-value)) 1)]
        (do 
          (if (= last-date date) ; check if date already exists
            (do 
              ;(println "last-date")
              ;(println last-index)
              (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value)))) ; drop last entry in old portfolio-value vector
              (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret})))
            )
            (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret})))
          )
        )
      )
      (let [ret 0.0]
        (do (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret}))))
      )
    )
  )
)

;; utility function
(defn view-portfolio-record
  "This function prints the portfolio value vector in a table format, with units added."
  []
  (def portfolio-record (atom [])) ; temporarily stores record for view

  (doseq [row (deref portfolio-value)] 
    (do
      (let [date (get row :date)
            tot-val (str "$" (int (get row :tot-value)))
            daily-ret (str (format "%.2f" (get row :daily-ret)) "%")
           ]
      
        (swap! portfolio-record concat [
          {:date date
           :tot-value tot-val
           :daily-ret daily-ret
          }]) 
      )
    )
  )
  
  (pprint/print-table (deref portfolio-record))
)

(defn total-value
  "This function returns the remaining total value including the cash and stock value"
  []
  (if (= (last (deref portfolio-value)) nil)
    (get (deref portfolio) :cash)
    (get (last (deref portfolio-value)) :tot-value)))
;; utility function
(defn view-portfolio
  "This function prints portfolio map in a table format."
  []
  (def portfolio-table (atom [])) ; temporarily stores record for view

    (doseq [[ticker row] (deref portfolio)] 
      (do
        ; (println ticker)
        ; (println row)
        (if (= ticker :cash)
          (do
            (let [tot-val (int (get row :tot-val))]
            (swap! portfolio-table concat [
              {:asset "cash"
              :price "N/A"
              :aprc "N/A"
              :quantity "N/A"
              :tot-val tot-val
              }]) 
            )
          )
          (do
            (let [price (get row :price)
              aprc (format "%.2f" (get row :aprc))
              quantity (get row :quantity)
              tot-val (int (get row :tot-val))
             ]
            (swap! portfolio-table concat [
              {:asset ticker
              :price price
              :aprc aprc
              :quantity quantity
              :tot-val tot-val
              }]) 
            )
          )
        )
      )
    )
  
  (pprint/print-table (deref portfolio-table))
)

(defn total-cal
  "This function returns the remaining total stock of a tic"
  [date tic])

; (defn order-parl
;   "This is the parellel ordering function"
;   [arg]
;   (if (= (count arg) 3)
;     (let [[date tic quantity] arg]
;       (let [[match adj-date price adj-price reference] (search-in-order date tic)]
;         (if match
;           (do
;             (update-portfolio adj-date tic quantity price adj-price)
;             {:date adj-date :tic tic :price price :quantity quantity :total "TBI" :reference reference})
;           (do
;             (println (format "The order request on date: %s, tic: %s, quantity: %d falses" date tic quantity))
;             (println (format "Failure reason: %s" adj-date)));;else
;         ))))
;     (if (= (count arg) 4)
;       (let [[date tic quantity] arg]
;         (let [[match adj-date price adj-price reference] (search-in-order date tic)]
;           (if match
;             (do
;               (update-portfolio adj-date tic quantity price adj-price)
;               {:date adj-date :tic tic :price price :quantity "TBI" :total quantity :reference reference})
;             (do
;               (println (format "The order request on date: %s, tic: %s, quantity: %d falses" date tic quantity))
;               (println (format "Failure reason: %s" adj-date));;else
;           ))))))
;[b t-1-date p aprc r]

(defn order-parl
  "This is the parellel ordering function"
  [arg]
  (if (= (count arg) 3)
    (let [[date tic quantity] arg]
      (let [[match adj-date price adj-price reference] (search-in-order date tic)]
        ;;(let [[match price reference] [true "10" 348]]
        (if match
          (do
            (let [total (
              cond 
              (= (get (get (deref portfolio) tic) :quantity) nil) 0
              :else (get (get (deref portfolio) tic) :quantity)) 
              cash (get (get (deref portfolio) :cash) :tot-val)]
              (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
              (do
              (update-portfolio adj-date tic quantity price adj-price)
              {:date adj-date :tic tic :price price :quantity quantity :total (+ total quantity) :reference reference})
              (do 
                (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
                (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell."))))))
          (do 
            (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
            (println (format "Failure reason: %s." adj-date)))))))
  (if (= (count arg) 4)
    (let [[date tic quantity remaining] arg]
      (if (= remaining true)
        (let [[match adj-date price adj-price reference] (search-in-order date tic)]
        ;;(let [[match price reference] [true "10" 348]]
        (if match
          (let [total (
            cond 
              (= (get (get (deref portfolio) tic) :quantity) nil) 0
              :else (get (get (deref portfolio) tic) :quantity)) 
            cash (get (get (deref portfolio) :cash) :tot-val)]
            (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
            (do
            (update-portfolio adj-date tic (- quantity total) price adj-price)
            {:date adj-date :tic tic :price price :quantity (- quantity total) :total quantity :reference reference})
            (do 
              (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
              (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell.")))))
          (do
            (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
            (println (format "Failure reason: %s." adj-date)))))
        (order-parl [date tic quantity])))))

(defn order-internal
	"This is the main order function"
	([date tic quantity]
	;;@date date-and-time trading date
	;;@tic  trading ticker
	;;@quantity exact number to buy(+) or sell(-)
	(let [[match adj-date price adj-price reference] (search-in-order date tic)]
	;;(let [[match price reference] [true "10" 348]]
	(if match
    (do
      (let [total (
        cond 
        (= (get (get (deref portfolio) tic) :quantity) nil) 0
        :else (get (get (deref portfolio) tic) :quantity)) 
        cash (get (get (deref portfolio) :cash) :tot-val)]
        (if (and (and (>= (+ total quantity) 0) (>= cash (* price quantity))))
        (do
        (update-portfolio adj-date tic quantity price adj-price)
        (swap! order-record concat [{:date adj-date :tic tic :price price :quantity quantity :total (+ total quantity) :reference reference}]))
        (do
          (if (and LEVERAGE (>= (total-value) (math/abs (* price quantity))))
            (do
              (update-portfolio adj-date tic quantity price adj-price)
              (swap! order-record concat [{:date adj-date :tic tic :price price :quantity quantity :total (+ total quantity) :reference reference}]))
          (do
            (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
            (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell."))))))))
    (do 
      (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
      (println (format "Failure reason: %s." adj-date))))));;else
	;;atoms [{:date :tic :price :quan :total :reference}{}]

	([date tic quantity remaining]
	;;@date trading date
	;;@tic  trading ticker
	;;@quantity remaining quantity (either sell or buy to reach such quantity)
	;;remaining bool
	(if (= remaining true)
		(let [[match adj-date price adj-price reference] (search-in-order date tic)]
		;;(let [[match price reference] [true "10" 348]]
		(if match
      (let [total (
        cond 
          (= (get (get (deref portfolio) tic) :quantity) nil) 0
          :else (get (get (deref portfolio) tic) :quantity)) 
        cash (get (get (deref portfolio) :cash) :tot-val)]
        (if (and (>= (+ total quantity) 0) (>= cash (* price quantity)))
        (do
        (update-portfolio adj-date tic (- quantity total) price adj-price)
        (swap! order-record concat [{:date adj-date :tic tic :price price :quantity (- quantity total) :total quantity :reference reference}]))
        (do 
          (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
          (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell.")))))
      (do
        (println (format "The order request at date: %s, tic: %s, quantity: %d fails." date tic quantity))
        (println (format "Failure reason: %s." adj-date)))))
	  (order-internal date tic quantity)))

	([args]
	;;this is where parallel computing is needed.
	(swap! order-record concat (doall (pmap order-parl args)))
	);;needs more work
  )

(defn order
  (
    [tic quantity]
    (order-internal (get-date) tic quantity)
  )
  (
    [tic quantity remaining]
    (order-internal (get-date) tic quantity remaining)
  )
  (
    [arg]
  ))



