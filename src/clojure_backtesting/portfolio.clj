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
    (open-record! portvalue-wrtr "out_portfolio_value_record.csv" "date,tot-value,daily-ret,tot-ret,loan,short,leverage,margin\n")
    (open-record! evalreport-wrtr "out_evaluation_report.csv" "date,tot-value,vol,r-vol,sharpe,r-sharpe,pnl-pt,max-drawdown\n"))
  (reset! order-record [])
  (reset! init-capital capital)
  (reset! eval-report-data [])
  (reset! eval-record [])
  (reset! portfolio {:cash {:tot-val capital}})
  (reset! portfolio-value [{:date (get-date) :tot-value capital :daily-ret 0.0 :tot-ret 0.0 :loan 0.0 :short 0.0 :leverage 0.0 :margin 1.0}])
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

(declare record-portfolio-value)

(defn update-loan
  "Records today's portfolio value after a change in borrowing. When
   `is-interest` is true, `amount` is interest debited from cash first;
   otherwise it is the amount just borrowed, which is informational since
   the loan balance is read from the cash balance."
  [date amount is-interest]
  (when is-interest
    (swap! portfolio update-in [:cash :tot-val] - amount))
  (record-portfolio-value date))

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

(defn write-off!
  "Books the holding in `permno` as cash at its last value and removes it,
   for a security that has stopped trading."
  [permno]
  (credit-cash! (get-in (deref portfolio) [permno :tot-val] 0.0))
  (swap! portfolio dissoc permno))

(defn revalue-holdings!
  "Brings every holding that has a row in today's `info-map` to today's
   price, crediting dividends to cash, then records today's portfolio value.
   A holding with no row for MISSING-DAYS-LIMIT trading days in a row is
   treated as delisted and written off to cash at its last value."
  [date info-map]
  (doseq [[permno position] (deref portfolio)
          :when (not= permno :cash)]
    (if-let [info (get info-map permno)]
      (let [[new-position payout] (revalue-position (dissoc position :missing) info)]
        (swap! portfolio assoc permno new-position)
        (credit-cash! payout))
      (let [missing (inc (get position :missing 0))]
        (if (>= missing MISSING-DAYS-LIMIT)
          (do
            (println (str date ": " permno " has had no price for " missing " trading days; its last value "
                          (format "%.2f" (double (:tot-val position))) " is booked as cash."))
            (write-off! permno))
          (swap! portfolio assoc-in [permno :missing] missing)))))
  (record-portfolio-value date))

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

(defn portfolio-exposure
  "The current portfolio map summarised in dollars: :cash, :long (market
   value of long positions), :short (absolute market value of short
   positions), :equity (cash + long - short), :gross (long + short) and
   :loan (cash borrowed, i.e. the negative cash balance)."
  []
  (let [p (deref portfolio)
        cash (double (get-in p [:cash :tot-val]))
        values (map :tot-val (vals (dissoc p :cash)))
        long-value (reduce + 0.0 (filter pos? values))
        short-value (- 0.0 (reduce + 0.0 (filter neg? values)))]
    {:cash cash
     :long long-value
     :short short-value
     :equity (- (+ cash long-value) short-value)
     :gross (+ long-value short-value)
     :loan (max 0.0 (- cash))}))

(defn record-portfolio-value
  "Appends (or replaces) today's entry in the portfolio-value vector from
   the current portfolio map: equity as :tot-value, its log return, the
   cumulative return, cash borrowed (:loan), the value of borrowed stock
   (:short), :leverage = (loan + short) / equity and :margin = equity /
   gross position value (1.0 with no positions)."
  [date]
  (let [{:keys [equity gross loan short]} (portfolio-exposure)
        prev (last (deref portfolio-value))
        ret (log-return equity (:tot-value prev))
        tot-ret (+ (:tot-ret prev) ret)
        margin (if (pos? gross) (/ equity gross) 1.0)
        leverage (if (pos? equity) (/ (+ loan short) equity) 0.0)
        entry {:date date :tot-value equity :daily-ret ret :tot-ret tot-ret
               :loan loan :short short :leverage leverage :margin margin}]
    (when (or (pos? loan) (pos? short))
      (reset! LOAN-EXIST true))
    (swap! portfolio-value (fn [entries] (conj (if (= (:date prev) date) (pop entries) entries) entry)))
    (write-record! portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f,%f\n" date equity ret tot-ret loan short leverage margin))))

;; Main function to update portfolio map + portfolio-value record when placing an order
(defn update-portfolio
  "Applies a trade of `quantity` shares of `permno` at today's row `info`
   and records today's portfolio value. `loan` is the amount borrowed for it."
  [date permno quantity info loan]
  (update-portfolio-map date permno quantity info loan)
  (record-portfolio-value date))

(defn total-value
  "This function returns the remaining total value including the cash and stock value"
  []
  (if (= (last (deref portfolio-value)) nil)
    (get (deref portfolio) :cash)
    (get (last (deref portfolio-value)) :tot-value)))
