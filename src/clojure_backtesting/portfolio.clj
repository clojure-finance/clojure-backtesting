(ns clojure-backtesting.portfolio
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure.java.io :as io]))

;; ============ Run state ============
;; Everything here is reset by init-portfolio.

(def portfolio (atom {:cash {:tot-val 0}}))
(def portfolio-value (atom []))
(def order-record (atom []))
(def eval-report-data (atom [])) ; evaluation report rows, formatted for printing
(def eval-record (atom [])) ; evaluation report rows, as numbers
(def init-capital (atom 0))
(def LOAN-EXIST (atom false)) ; whether a loan has been taken during this run
(def TERMINATED (atom true)) ; whether the run has ended; before the first run counts as ended

;; CSV records, written only while OUTPUT-DIR is set
(def order-wrtr (atom nil))
(def portvalue-wrtr (atom nil))
(def evalreport-wrtr (atom nil))

(defn write-record!
  "Appends `line` to the record held in `wrtr-atom`, if that record is being written."
  [wrtr-atom line]
  (when-let [w (deref wrtr-atom)]
    (.write ^java.io.Writer w ^String line)))

(defn- open-record! [wrtr-atom file-name header]
  (let [f (io/file OUTPUT-DIR file-name)]
    (io/make-parents f)
    (reset! wrtr-atom (io/writer f))
    (write-record! wrtr-atom header)))

(defn close-records!
  "Closes any open CSV records."
  []
  (doseq [wrtr-atom [order-wrtr portvalue-wrtr evalreport-wrtr]]
    (when-let [w (deref wrtr-atom)]
      (.close ^java.io.Writer w)
      (reset! wrtr-atom nil))))

  ;; Backtester initialisation
(defn init-portfolio
  "This function initialises or restarts the backtester at `date` with
   `capital` in cash. When OUTPUT-DIR is set, the order, portfolio-value
   and evaluation records are also written there as CSV files."
  [date capital]
  (assert (some #{:APRC} headers) "The main dataset has no APRC column. Load it with (load-dataset dir \"main\" add-aprc).")
  (assert (< (compare (init-date date) (first (last data-files))) 0) "Please do not start from the last date. Init portfolio fails.")
  (close-records!)
  (when OUTPUT-DIR
    (open-record! order-wrtr "out_order_record.csv" "date,security,quantity,price\n")
    (open-record! portvalue-wrtr "out_portfolio_value_record.csv" "date,tot-value,daily-ret,tot-ret,loan,leverage,margin\n")
    (open-record! evalreport-wrtr "out_evaluation_report.csv" "date,tot-value,vol,r-vol,sharpe,r-sharpe,pnl-pt,max-drawdown\n"))
  (reset! order-record [])
  (reset! init-capital capital)
  (reset! eval-report-data [])
  (reset! eval-record [])
  (reset! portfolio {:cash {:tot-val capital}})
  (reset! portfolio-value [{:date (get-date) :tot-value capital :daily-ret 0.0 :tot-ret 0.0 :loan 0.0 :leverage 0.0 :margin 0.0}])
  (reset-indicator-maps)
  (reset-automation)
  (reset! LOAN-EXIST false)
  (reset! TERMINATED false)
  (str "Date: " (get-date) " Cash: $" (get (get (deref portfolio) :cash) :tot-val)))

(defn log-return
  "Natural-log return from `prev` to `curr`, or 0.0 when either is not positive."
  [curr prev]
  (if (and (pos? curr) (pos? prev))
    (Math/log (/ (double curr) (double prev)))
    0.0))

  ;; Update loan in portfolio
(defn update-loan
  "Appends today's portfolio-value entry while a loan is (or has been) open.
   Cash already goes negative by the borrowed amount in `update-portfolio-map`,
   so the loan balance is simply the negative cash balance: it grows when
   interest is debited and shrinks when positions are sold. `loan` is the
   amount the caller borrowed in this order (informational) or, when
   `is-interest` is true, the interest to debit from cash. The equity return
   is log(tot-value / prev-value); tot-value is already net of the loan, so
   no leverage multiplier applies."
  [date loan is-interest]
  (when is-interest
      ;; deduct cash in portfolio
    (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) loan)}))
    ;; update portfolio-value record
  (let [tot-value (reduce + (map :tot-val (vals (deref portfolio))))
        prev-value (:tot-value (last (deref portfolio-value)))
        cash (get-in (deref portfolio) [:cash :tot-val]) ; get total amount of cash
        new-loan (max 0.0 (- (double cash))) ; loan outstanding = negative cash balance
        gross-value (+ tot-value new-loan)
        new-leverage (if (pos? tot-value) (/ new-loan tot-value) 0.0) ; total debt / total equity
        new-margin (if (pos? gross-value) (/ tot-value gross-value) 0.0) ; equity / gross position value
        ret (log-return tot-value prev-value)
        tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)
        last-date (get (last (deref portfolio-value)) :date)]
    (do
      (if (= (deref LOAN-EXIST) false) ; check loan-exist switch
        (reset! LOAN-EXIST true)) ; flip the switch
      (if (= last-date date) ; check if date already exists
        (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value))))) ; drop last entry in old portfolio-value vector

        ; update portfolio-value vector
      (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :leverage new-leverage :loan new-loan :margin new-margin})))
      (write-record! portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f\n" date (double tot-value) (double ret) (double tot-ret) (double new-leverage) (double new-loan) (double new-margin))))))

;; ============ Holdings ============
;;
;; The portfolio map is {:cash {:tot-val dollars}, permno {:price :aprc :cfacpr
;; :quantity :tot-val}, ...}. Cash and every :tot-val are in dollars (or
;; whatever unit the price column is in). A position remembers the price,
;; adjusted price and CFACPR of the last row it was valued on, so that the
;; next revaluation can apply the total return earned since then.

(declare record-portfolio-value)

(defn- split-factor
  "Shares held per share previously held, from CRSP's cumulative price
   adjustment factor: 2.0 across a 2-for-1 split. 1.0 when either value is
   missing, in which case a split is indistinguishable from a cash payout."
  [prev-cfacpr curr-cfacpr]
  (let [prev (->double prev-cfacpr)
        curr (->double curr-cfacpr)]
    (if (and prev curr (pos? curr))
      (/ prev curr)
      1.0)))

(defn revalue-position
  "Carries a position to today's data row `info`. Returns [position cash]
   where cash is the dividend paid out to the cash balance.
   With REINVEST-DIVIDENDS the position's value follows the security's
   total return (the APRC ratio since it was last valued) and the share
   count absorbs both dividends and splits; nothing is paid out.
   Without it the share count changes only by the split factor and the
   remainder of the total return is paid out as cash. Short positions pay
   the dividend instead of receiving it."
  [{:keys [price aprc quantity cfacpr] :as position} info]
  (let [new-price (PRICE-KEY info)
        new-aprc (:APRC info)
        growth (/ new-aprc aprc) ; 1 + total return since the last valuation
        carried (assoc position :price new-price :aprc new-aprc :cfacpr (:CFACPR info))]
    (if REINVEST-DIVIDENDS
      (let [scaled (* quantity growth (/ price new-price))
            ;; on a day without distributions the scaling is 1 up to
            ;; floating-point noise; keep the share count exact then
            new-quantity (if (< (Math/abs (double (- scaled quantity))) (* 1e-9 (Math/abs (double quantity))))
                           quantity
                           scaled)]
        [(assoc carried :quantity new-quantity :tot-val (* new-quantity new-price)) 0.0])
      (let [factor (split-factor cfacpr (:CFACPR info))
            new-quantity (* quantity factor)
            payout (* quantity (- (* growth price) (* factor new-price)))]
        [(assoc carried :quantity new-quantity :tot-val (* new-quantity new-price)) payout]))))

(defn- credit-cash! [amount]
  (when-not (zero? amount)
    (swap! portfolio update-in [:cash :tot-val] + amount)))

(defn- current-position
  "The position in `permno` valued on today's row `info` (paying out any
   dividend due), or an empty position if none is held."
  [permno info]
  (if-let [existing (get (deref portfolio) permno)]
    (let [[position payout] (revalue-position existing info)]
      (credit-cash! payout)
      position)
    {:price (PRICE-KEY info) :aprc (:APRC info) :cfacpr (:CFACPR info) :quantity 0.0 :tot-val 0.0}))

(defn revalue-holdings!
  "Brings every holding that has a row in today's `info-map` to today's
   price, crediting dividends to cash, then records today's portfolio value."
  [date info-map]
  (doseq [[permno position] (deref portfolio)
          :when (not= permno :cash)]
    (when-let [info (get info-map permno)]
      (let [[new-position payout] (revalue-position position info)]
        (swap! portfolio assoc permno new-position)
        (credit-cash! payout))))
  (record-portfolio-value date 0))

  ;; Update the portfolio map
(defn update-portfolio-map
  "Buys (quantity > 0) or sells quantity shares of `permno` at the price in
   today's row `info`, moving quantity * price between cash and the holding."
  [date permno quantity info loan]
  (let [position (current-position permno info)
        price (PRICE-KEY info)
        new-quantity (+ (:quantity position) quantity)]
    (swap! portfolio update-in [:cash :tot-val] - (* quantity price))
    (if (< (Math/abs (double new-quantity)) 1e-9)
      (swap! portfolio dissoc permno) ; remove security from portfolio if qty = 0
      (swap! portfolio assoc permno (assoc position :quantity new-quantity :tot-val (* new-quantity price))))))

  ;; Update the portfolio-value vector which records the daily portfolio value
(defn record-portfolio-value
  "Appends (or replaces) today's entry in the portfolio-value vector from the
   current portfolio map: total value, log return, cumulative return, loan,
   leverage and margin. `loan` is the amount borrowed by the order that
   triggered this, or 0 for a plain revaluation."
  [date loan]
  (let [[tot-value prev-value] ;; get portfolio total
        [(reduce + (map :tot-val (vals (deref portfolio)))) (:tot-value (last (deref portfolio-value)))]]

    (if (and (not= prev-value 0) (not= prev-value 0.0)) ; check division by zero
        ; if prev_value not 0
      (if (or (not= loan 0) (deref LOAN-EXIST))

          ; exist leverage
        (update-loan date loan false)

          ; no leverage, update return with log formula: daily_ret = ln(tot_val/prev_val)
        (let [ret (log-return tot-value prev-value)
              tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)
              last-date (get (last (deref portfolio-value)) :date)]
          (do
            (if (= last-date date) ; check if date already exists
              (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value))))) ; drop last entry in old portfolio-value vector
            (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :loan 0.0 :leverage 0.0 :margin 0.0})))
            (write-record! portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f\n" date (double tot-value) (double ret) (double tot-ret) (double 0.0) (double 0.0) (double 0.0))))))
        ; if prev_value is 0, let ret = 0.0
      (let [ret 0.0
            tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)]
        (do
          (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :loan 0.0 :leverage 0.0 :margin 0.0})))
          (write-record! portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f\n" date (double tot-value) (double ret) (double tot-ret) (double 0.0) (double 0.0) (double 0.0))))))))

;; Main function to update portfolio map + portfolio-value record when placing an order
(defn update-portfolio
  "Applies a trade of `quantity` shares of `permno` at today's row `info`
   and records today's portfolio value. `loan` is the amount borrowed for it."
  [date permno quantity info loan]
  (update-portfolio-map date permno quantity info loan)
  (record-portfolio-value date loan))

(defn total-value
  "This function returns the remaining total value including the cash and stock value"
  []
  (if (= (last (deref portfolio-value)) nil)
    (get (deref portfolio) :cash)
    (get (last (deref portfolio-value)) :tot-value)))
