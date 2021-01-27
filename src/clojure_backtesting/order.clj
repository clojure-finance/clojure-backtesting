(ns clojure-backtesting.order
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math])
    )

;;This file is for ordering related functions
;;testing purpose
;(def file1 "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv")
;;(def a (read-csv-row file1))
(def lazy-mode (atom false))
(def dataset-col (atom {}))

(defn set-main
  "Doing the initialzation for the lazy mode."
  [name]
  (def pending-order (atom {}))
  (def main-name name)
  (reset! lazy-mode true))

(defn valid-line
  "Check if a line is valid in format"
  [line]
  (if (not= (get line :TICKER) "")
    true
    false))

(defn- lazy-init
  "Go to the starting line of the dataset, the date should be large or equal the input date."
  [date & [name]]
  (loop [count 0 remaining (get (deref dataset-col) (or name main-name))]
    (if (empty? remaining)
      (do
        (println "The date is beyond the date frame of the dataset.")
        nil)
      (let [first-line (first remaining)
            next-remaining (rest remaining)
            cur-date (get first-line :date)]
         ;(println first-line)
        (if (and (>= (compare cur-date date) 0) (valid-line first-line))
             ;(reset! data-set remaining)
             ;(println "-----")
          (do
             ;(println count)
            (swap! dataset-col assoc (or name main-name) remaining)
            first-line)
          (if (< count 2000)
            (recur (inc count) next-remaining)
            (do
              (swap! dataset-col assoc (or name main-name) remaining)
              (recur 0 next-remaining))))))))

;;testing purpose, delete afterwards
;(def testfile1 (read-csv-row "/home/kony/Documents/GitHub/clojure-backtesting/resources/CRSP-extract.csv"))

;; Search-date function
(defn search-date
  "This function tries to retrieve the matching entry from the dataset."
  [date tic dataset]
  ;;date e.g. "DD/MM?YYYY"
  ;;tic e.g. "AAPL"
  ;;return [false 0 0] if no match
  ;;return [true price reference] otherwise
  
  (loop [count 0 remaining dataset] ;(original line)
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
  [date tic dataset]
;; date e.g. "DD/MM/YYYY"
;; tic e.g. "AAPL"
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

;; Create initial portfolio with cash only (User input thei initial-capital)
(defn init-portfolio
  "This is the function that initialise or restart the backtesting process"
  [date init-capital] ;; the dataset is the filtered dataset the user uses, as we need the number of days from it
  ;; example: portfolio -> {:cash {:tot-val 10000} :"AAPL" {:price 400 :aprc adj-price :quantity 100 :tot-val 40000}}
  ;; example: portfolio-value {:date 1980-12-16 :tot-value 50000 :daily-ret 0 :loan 0 :leverage 0}
  (if (not (deref lazy-mode))
    (do
      (init-date date)
      (maintain-tics true)
    )
    (lazy-init date)
  )
  (try (io/delete-file "./out_order_record.csv")
       nil) ;First delete the file (act as emptying)
  (def wrtr (io/writer "./out_order_record.csv" :append true))
  (.write wrtr "date,TICKER,quantity\n")
  (def order-record (atom []))
  (def init-capital init-capital)
  (def loan-exist false) ; global swtich for storing whether loan exists
  (def eval-report-data (atom [])) ; to store evaluation report (in string format, for printing)
  (def eval-record (atom [])) ; to store evaluation report (in number format)
  (def portfolio (atom {:cash {:tot-val init-capital}}))
  (def portfolio-value (atom [{:date date :tot-value init-capital :daily-ret 0.0 :tot-ret 0.0 :loan 0.0 :leverage 0.0}])))

;; Update the portfolio when placing an order
(defn update-portfolio
  [date tic quantity price aprc loan]
  ;(println aprc)
  ;; check whether the portfolio already has the security
  (if-not (contains? (deref portfolio) tic) 
    (let [tot-val (* aprc quantity) 
          tot-val-real (* price quantity)]
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
    (if (and (not= prev-value 0) (not= prev-value 0.0)) ; check division by zero
      ; if loan != 0 or previous leverage ratio != 0
      (if (or (not= loan 0) (= loan-exist true))
      
        (do ; exist leverage
          (let [new-loan (+ loan (get (last (deref portfolio-value)) :loan)) ; update total loan
                new-leverage (/ new-loan (- tot-value new-loan)) ; update leverage ratio = total debt / total equity
                ret (* (Math/log (/ tot-value prev-value)) new-leverage) ; update return with formula: daily_ret_lev = log(tot_val/prev_val) * leverage
                tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)
                last-date (get (last (deref portfolio-value)) :date)
                last-index (- (count (deref portfolio-value)) 1)
               ]
            (do
              (if (= loan-exist false) ; check switch
                (def loan-exist true)
              )
              (if (= last-date date) ; check if date already exists
                (do 
                  ;(println "last-date")
                  ;(println last-index)
                  (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value)))) ; drop last entry in old portfolio-value vector
                  (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :leverage new-leverage :loan new-loan})))
                )
                (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :leverage new-leverage :loan new-loan})))
              ) 
            )
          )
        )

        (do ; no leverage, update return with log formula: daily_ret = log(tot_val/prev_val)
          (let [ret (Math/log (/ tot-value prev-value))
            tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)
            last-date (get (last (deref portfolio-value)) :date)
            last-index (- (count (deref portfolio-value)) 1)]
            (do 
              (if (= last-date date) ; check if date already exists
                (do 
                  ;(println "last-date")
                  ;(println last-index)
                  (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value)))) ; drop last entry in old portfolio-value vector
                  (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :loan 0.0 :leverage 0.0})))
                )
                (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :loan 0.0 :leverage 0.0})))
              )
            )
          )
        )
      )
      ; if prev_value == 0, let ret = 0.0
      (let [ret 0.0
            tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)]
        (do (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret}))))
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
            daily-ret (str (format "%.2f" (* (get row :daily-ret) 100)) "%")
            tot-ret (str (format "%.2f" (* (get row :tot-ret) 100)) "%")
            loan (str "$" (format "%.2f" (get row :loan)))
            leverage (str (format "%.2f" (* (get row :leverage) 100)) "%")
           ]
      
        (swap! portfolio-record concat [
          {:date date
           :tot-value tot-val
           :daily-ret daily-ret
           :tot-ret tot-ret
           :loan loan
           :leverage leverage
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

(defn- place-order
  "This private function do the basic routine for an ordering, which are update portfolio and return record"
  [date tic quantity price adj-price loan reference print direct]
  (update-portfolio date tic quantity price adj-price loan)
  (if print
    (println (format "Order: %s | %s | %d." date tic quantity)))
  (if direct
    (.write wrtr (format "%s,%s,%d\n" date tic quantity)))
  {:date date :tic tic :price price :aprc (format "%.2f" adj-price) :quantity quantity})

(defn order-internal
	"This is the main order function"
	([order-date tic quan remaining leverage dataset print direct]
	;;@date date-and-time trading date
	;;@tic  trading ticker
	;;@quantity exact number to buy(+) or sell(-)
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
        (if (and (not= quantity 0) (not= quantity 0.0)) ;; ignore the empty order case
          (if (and (and (>= (+ total quantity) 0) (or (<= quantity 0) (>= cash (* price quantity)))))
            (place-order date tic quantity price adj-price 0 reference print direct) ;loan is 0 here
            (do
              (if leverage
                (if (< (+ total quantity) 0)
                  (place-order date tic quantity price adj-price 0 reference print direct) ;This is the sell on margin case
                  (let [loan
                        (cond (<= cash 0)
                              (* quantity price)
                              :else (- cash (* quantity price)))]
                    (place-order date tic quantity price adj-price loan reference print direct))) ;This is the buy on margin case
                (do
                  (println (format "Order request %s | %s | %d fails." order-date tic quantity))
                  (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell. Try to solve by enabling leverage."))))))
          (update-portfolio date tic 0 price adj-price 0))))
    (do 
      (println (format "The order request %s | %s | %d fails." order-date tic quan))
      (println (format "Failure reason: %s." date))))))
  )

(defn order
  ([tic quantity & {:keys [remaining leverage dataset print direct] :or {remaining false leverage LEVERAGE dataset (deref data-set) print false direct true}}]
   (let [record (order-internal (get-date) tic quantity remaining leverage dataset print direct)]
     (if record
     (swap! order-record conj record)
       );else
     ))
  ([arg] ;This function still needs to be developed in order for parallelisium
   (swap! order-record conj (doall (pmap order-internal arg))))
  )

(defn- updateHoldingTickers
  "Update all the tickers in terms of portfolio"
  []
  (for [tic (keys (deref portfolio))]
    (order-internal (get-date) tic 0 false true (deref data-set) false false)))

(defn next-date
  []
  (updateHoldingTickers)
  (internal-next-date))

(defn end-order
  []
  (.close wrtr))