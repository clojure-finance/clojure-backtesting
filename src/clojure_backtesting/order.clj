(ns clojure-backtesting.order
  (:require [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.indicators :refer :all]))

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
      (write-record! order-wrtr (format "%s,%s,%f,%f\n" date permno (double quantity) price)))
    (swap! order-record conj {:date date :permno permno :price price :aprc (format "%.2f" adj-price) :quantity quantity})))

(defn- short-within-margin?
  "Whether equity still covers INITIAL-MARGIN of the gross position value
   after trading `quantity` shares at `price` from a holding of `total`,
   which leaves or extends a short position."
  [total quantity price]
  (or (nil? INITIAL-MARGIN)
      (let [{:keys [equity gross]} (portfolio-exposure)
            gross-after (+ (- gross (Math/abs (double (* total price))))
                           (Math/abs (double (* (+ total quantity) price))))]
        (>= equity (* INITIAL-MARGIN gross-after)))))

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
                (if (short-within-margin? total quantity price)
                  (place-order date permno quantity info 0 print direct) ;This is the sell on margin case
                  (println (format "Order request %s | %s | %s fails due to initial margin requirement on the short position." order-date permno (str quantity))))
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

(def pending-order (atom (sorted-map)))

(defn order
  "Queues an order of `quantity` shares of `permno`. It fills at the close
   of the next trading day on which the security has a price, up to
   `expiration` trading days after today (ORDER-EXPIRATION by default);
   after that it is dropped. `:remaining true` makes `quantity` the target
   holding instead of the amount to trade."
  ([permno quantity & {:keys [expiration remaining leverage print direct] :or {expiration ORDER-EXPIRATION remaining false leverage LEVERAGE print PRINT direct DIRECT}}]
   (if (= (deref TERMINATED) false)
     (let [place-date (get-date)
           expire-date (date-after-n-trading-days place-date expiration)]
       (swap! pending-order assoc [expire-date permno] {:place place-date :expire expire-date :permno permno :quantity quantity :remaining remaining :leverage leverage :print print :direct direct})))))

(defn check-order
  []

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
          (order-internal (get-date) security 0 true false PRINT DIRECT info)
          (println (str (get-date) ": " security " has no price today and cannot be closed."))))
      (update-eval-report)
      (close-records!)
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
          margin-call (println (str (get-date) ": Portfolio margin " port-margin " (equity / gross position value) is below the maintenance margin " MAINTENANCE-MARGIN ". Closing all positions.")))
        (println "To restart. Please call init-portfolio again.")
        (end-order)))))

(defn next-date
  "Advances the clock one trading day: revalues holdings, charges interest,
   checks margin, fills pending orders and runs automations. Returns the new
   date, or nil once the run has terminated."
  []
  (when (= (deref TERMINATED) false)
    (if-let [_date (get-next-date)]
      (do
        (reset! date _date)
        (update-daily-indicators)
        (incur-interest-cost)
        (update-holding-tickers)
        (check-terminating-condition)
        (check-order)
        (check-automation)
        _date)
      (throw (Exception. "No next date in the dataset. Call init-portfolio to restart.")))))
