(ns clojure-backtesting.order
  (:require [clojure-backtesting.counter :refer :all] ;; [clojure.pprint :as pprint]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [java-time :as jt] ;; [clojure.java.io :as io]
            ))
;; ================ Deprecated Codes =================

;; (defn search-date
;;   "This function tries to retrieve the matching entry from the dataset."
;;   [date permno dataset]
;;   ;; date e.g. "DD/MM/YYYY"
;;   ;; permno e.g. "AAPL"
;;   ;; dataset

;;   ;; return [false 0 0] if no match
;;   ;; return [true price reference] otherwise

;;   (loop [count 0 remaining dataset] ;(original line)
;;   ;(loop [count 0 remaining testfile1] 	;testing line, change the data-set to CRSP
;;     (if (empty? remaining)
;;       [false 0 0]
;;       (let [first-line (first remaining)
;;             next-remaining (rest remaining)]
;;         (if (and (= (get first-line :date) date) ;;amend later if the merge data-set has different keys (using the keys in CRSP now)
;;                  (= (get first-line TICKER-KEY) permno) ;;amend later if the merge data-set has different keys(using the keys in CRSP now)
;;                  )
;;           (let [price (get first-line PRICE-KEY)
;;                 aprc (get first-line :APRC)] ;;amend later if you want to use the adjusted price instead of the closing price
;;             [true price aprc count])
;;           (recur (inc count) next-remaining)))))
;; )

;; (defn search-in-order
;;   "This function turns the order processed date"
;;   [date permno dataset]
;;   ;; @date e.g. "DD/MM/YYYY"
;;   ;; @permno e.g. "AAPL"
;;   ;; @dataset

;;   ;; return [false "No match date" 0 0 0] if no match
;;   ;; return [true T+1-date price aprc reference] otherwise

;;   (if (and (not (deref lazy-mode)) (not= (count (deref available-tics)) 0))
;;     (if (and (not= -1 (.indexOf (keys (deref available-tics)) permno)) (not= (get (get (get (deref available-tics) permno) :reference) :date) (get (get (deref tics-info) permno) :end-date)))
;;       (let [t-1-date (get (first (rest (get (get (deref available-tics) permno) :reference))) :date)
;;             [b p aprc r] (search-date t-1-date permno (get (get (deref available-tics) permno) :reference))]
;;         (if b
;;           [b t-1-date (Double/parseDouble p) aprc r]))
;;       [false "No such date or security in the dataset or the dataset has reached the end" 0 0 0])
;;     (let [[match price aprc reference] (search-date date permno dataset)]
;; 		;;(let [[match price reference] [true "10" 348]]
;;       (if match
;;         (if (deref lazy-mode) 
;;             [match date (Double/parseDouble price) aprc reference]
;;             (loop [i 1]
;;               (if (<= i MAXLOOKAHEAD)
;;                 (let [t-1-date (look-ahead-i-days date i)
;;                       [b p aprc r] (search-date t-1-date permno dataset)]
;;                   (if b
;;                     [b t-1-date (Double/parseDouble p) aprc r]
;;                     (recur (inc i))))
;;                 [false (str "No appropriate order date after looking ahead " MAXLOOKAHEAD " days") 0 0 0])))
;;         [false "No such date or security in the dataset" 0 0 0])))
;; )

;; (defn order-internal
;;   "This is the main order function"
;;   [order-date permno quan remaining leverage dataset print direct]
;; 	;; @date date-and-time trading date
;; 	;; @permno  trading security
;; 	;; @quantity exact number to buy(+) or sell(-)
;;   (let [[match date price adj-price reference] (search-in-order order-date permno dataset)]
;;    ; Note that the date here may contain error information
;;     (if match
;;       (do
;;         (let [total (cond
;;                       (= (get (get (deref portfolio) permno) :quantity) nil) 0
;;                       :else (get (get (deref portfolio) permno) :quantity))
;;               cash (get (get (deref portfolio) :cash) :tot-val)
;;               quantity (cond
;;                          remaining (- quan total)
;;                          :else quan)]
;;           (if (and (and (not= quantity 0) (not= quantity 0.0)) (not= quantity "special")) ;; ignore the empty order case
;;             (if (and (and (>= (+ total quantity) 0) (or (<= quantity 0) (>= cash (* adj-price quantity)))))
;;               (place-order date permno quantity price adj-price 0 reference print direct) ;loan is 0 here
;;               (do
;;                 (if leverage
;;                   (if (< (+ total quantity) 0)
;;                     (place-order date permno quantity price adj-price 0 reference print direct) ;This is the sell on margin case
;;                     (let [loan
;;                           (cond (<= cash 0)
;;                                 (* quantity adj-price)
;;                                 :else (- (* quantity adj-price) cash))]
;;                       (if (or (= INITIAL-MARGIN nil) (>= cash (* INITIAL-MARGIN (+ loan cash))))
;;                         (place-order date permno quantity price adj-price loan reference print direct)
;;                         (if print
;;                           (println (format "Order request %s | %s | %d fails due to initial margin requirement." order-date permno quantity)))))) ;This is the buy on margin case
;;                   (do
;;                     (println (format "Order request %s | %s | %d fails." order-date permno quantity))
;;                     (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell. Try to solve by enabling leverage."))))))
;;             (if (= quantity "special")
;;               (update-portfolio date permno 0 price adj-price 0)
;;               nil))))
;;       (do
;;         (if (not= quan "special")
;;           (do
;;             (println (format "The order request %s | %s | %d fails." order-date permno quan))
;;             (println (format "Failure reason: %s." date))))))))

;; ==========================================================

(defn search-in-order
  "This function turns the order processed date"
  [permno dataset]
  ;; @date e.g. "DD/MM/YYYY" = (get-date)
  ;; @permno e.g. "AAPL"
  ;; @dataset = nil

  ;; return [false "No match date" 0 0 0] if no match
  ;; return [true T+1-date price aprc reference] otherwise
  ())

(defn- incur-transaction-cost
  "This private function deducts the commission fee for making an order.
   The fee is charged on the absolute trade value in dollars, so a sell is
   charged as well as a buy."
  [quantity price]
  (if (> TRANSACTION-COST 0)
    (let [cash-to-pay (* (Math/abs (double (* quantity price))) TRANSACTION-COST)]
      (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) cash-to-pay)}))))

(defn incur-interest-cost
  "This private function deducts the loan interests cost on every trading day."
  []
  (if (and (> INTEREST-RATE 0) (= (deref LOAN-EXIST) true))
    (let
     [tot-loan (get-in (last (deref portfolio-value)) [:loan])
      cash-to-pay (* (* INTEREST-RATE (/ 1 252)) tot-loan)]
      (update-loan (get-date) cash-to-pay true))))

(defn- place-order
  "This private function does the basic routine for an ordering - update portfolio and return record."
  [date permno quantity info loan print direct]
  (let [price (PRICE-KEY info)
        adj-price (:APRC info)]
    (if (not (deref TERMINATED))
      (do
        (incur-transaction-cost quantity price)
        (update-portfolio date permno quantity info loan))) ; w/o loan interest
    (if print
      (println (format "Order: %s | %s | %f." date permno (double quantity))))
    (if direct
      (.write wrtr (format "%s,%s,%f,%f\n" date permno (double quantity) price)))
    (swap! order-record conj {:date date :permno permno :price price :aprc (format "%.2f" adj-price) :quantity quantity})))

(defn order-internal
  "This is the main order function"
  [order-date permno quan remaining leverage print direct info]
	;; @date date-and-time trading date
	;; @permno  trading security
	;; @quantity exact number to buy(+) or sell(-)
  (let [date order-date
        price (PRICE-KEY info)]
    (let [total (cond
                  (= (get (get (deref portfolio) permno) :quantity) nil) 0
                  :else (get (get (deref portfolio) permno) :quantity))
          cash (get (get (deref portfolio) :cash) :tot-val)
          quantity (cond
                     remaining (- quan total)
                     :else quan)]
      (if (and (not= quantity 0) (not= quantity 0.0) (not= quantity "special")) ;; ignore the empty order case
        (if (and (and (>= (+ total quantity) 0) (or (<= quantity 0) (>= cash (* price quantity)))))
          (place-order date permno quantity info 0 print direct) ;loan is 0 here
          (do
            (if leverage
              (if (< (+ total quantity) 0)
                (place-order date permno quantity info 0 print direct) ;This is the sell on margin case
                (let [loan
                      (cond (<= cash 0)
                            (* quantity price)
                            :else (- (* quantity price) cash))]
                  (if (or (= INITIAL-MARGIN nil) (>= cash (* INITIAL-MARGIN (+ loan cash))))
                    (place-order date permno quantity info loan print direct)
                    (println (format "Order request %s | %s | %s fails due to initial margin requirement." order-date permno (str quantity)))))) ;This is the buy on margin case
              (do
                (println (format "Order request %s | %s | %s fails." order-date permno (str quantity)))
                (println (format "Failure reason: %s" "You do not have enough money to buy or have enough stock to sell. Try to solve by enabling leverage."))))))
        (if (= quantity "special")
          (update-portfolio date permno 0 info 0)
          nil)))))

;; (defn order
;;   ([permno quantity & {:keys [remaining leverage dataset print direct] :or {remaining false leverage LEVERAGE dataset (deref data-set) print false direct true}}]
;;    (let [record (order-internal (get-date) permno quantity remaining leverage dataset print direct)]
;;      (if record
;;      (swap! order-record conj record)
;;        ) ;else
;;      ))
;;   ([arg] ;This function still needs to be developed for parallelisium
;;    (swap! order-record conj (doall (pmap order-internal arg))))
;;   )

(def pending-order (atom (sorted-map)))

(defn order
;;  ([quantity & {:keys [remaining leverage print direct] :or {remaining false leverage LEVERAGE print false direct true}}]
;;   (order-lazy (curr-permno) quantity :remaining remaining :leverage leverage :print print :direct direct))
  ([permno quantity & {:keys [expiration remaining leverage print direct] :or {expiration ORDER-EXPIRATION remaining false leverage LEVERAGE print PRINT direct DIRECT}}]
   (if (= (deref TERMINATED) false)
     (let [place-date (get-date)
           expire-date (jt/format "yyyy-MM-dd" (jt/plus (jt/local-date "yyyy-MM-dd" place-date) (jt/days expiration)))]
       (swap! pending-order assoc [expire-date permno] {:place (get-date) :expire expire-date :permno permno :quantity quantity :remaining remaining :leverage leverage :print print :direct direct})))))

(defn check-order
  []
  ;; delete the expired orders
  ;; (reset! pending-order (subseq (deref pending-order) >= [(get-date) ""]))

  ;; traverse pending order for potential placing
  (loop [new-order (sorted-map) pending (subseq (deref pending-order) >= [(get-date) ""])]
    (if (= (count pending) 0)
      (reset! pending-order new-order)
      (let [pair (first pending)
            permno (nth (first pair) 1)
            arg (nth pair 1)
            remain (rest pending)]
        (if (get-permno-info permno)
          (do
            (order-internal (get-date) permno (:quantity arg) (:remaining arg) (:leverage arg) (:print arg) (:direct arg) (get-permno-info permno))
            (recur new-order remain))
          (recur (assoc new-order (first pair) arg) remain))))))

(defn update-holding-tickers
  "Revalues every holding at today's close, paying or reinvesting dividends."
  []
  (revalue-holdings! (get-date) (get-info-map)))

(defn end-order
  "Call this function at the end of the strategy. Closes every position at
   today's close; a security with no row today stays valued at its last
   price and is reported as still held."
  []
  ;; close all positions
  (if (not (deref TERMINATED))
    (do
      (doseq [[security] (deref portfolio)
              :when (not= security :cash)]
        (if-let [info (get (get-info-map) security)]
          (order-internal (get-date) security 0 true false false false info)
          (println (str (get-date) ": " security " has no price today and cannot be closed."))))
      (update-eval-report)
      (.close wrtr)
      (.close portvalue-wrtr)
      (.close evalreport-wrtr)
      (reset! pending-order (sorted-map))
      (reset! TERMINATED true))))

(defn check-terminating-condition
  "Close all positions if net worth < 0 or portfolio margin < maintenance margin, i.e. user has lost all cash"
  []
  (if (not (deref TERMINATED))
    (let [tot-value (get (last (deref portfolio-value)) :tot-value)
          port-margin (get (last (deref portfolio-value)) :margin)
          margin-call (and (deref LOAN-EXIST) (< port-margin MAINTENANCE-MARGIN))]
      ;; original inequality: value of stocks (excl. shorted stocks) - net cash > 0
      ;; rearranging, equivalent to checking value of stocks (incld. shorted stocks) - cash > 0
      ;; where LHS = net worth
      (when (or (= (get-next-date) nil) (< (compare tot-value 0) 0) margin-call) ; if net worth < 0
        (cond
          (= (get-next-date) nil) (println "You have reached the end of the dataset. No more orders are allowed.")
          (< (compare tot-value 0) 0) (println (str (get-date) ": You have lost all cash. Closing all positions."))
          margin-call (println (str (get-date) ": Portfolio margin " port-margin " is below the maintenance margin " MAINTENANCE-MARGIN ". Closing all positions.")))
        (println "To restart. Please call init-portfolio again.")
        (end-order)))))

(defn next-date
  []
  (if (= (deref TERMINATED) false)
    (if-let [_date (get-next-date)]
      (do
        (reset! date _date)
      ;; maintain tics
      ;; (if (= (deref tics-tomorrow) nil)
      ;;   (reset! tics-today nil)
      ;;   (do
        (reset-daily-var)
        (update-daily-indicators)
        (incur-interest-cost) ;; 
        (update-holding-tickers) ;; todo: order of these two
        (check-terminating-condition)
        (check-order)
        (check-automation)
        _date)
      (do
        (throw (Exception. "Reach unexpected code branch 1."))
      ;; (reset-daily-var)
        nil))))