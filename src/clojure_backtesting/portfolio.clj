(ns clojure-backtesting.portfolio
    (:require [clojure-backtesting.data :refer :all]
              [clojure-backtesting.data-management :refer :all]
              [clojure-backtesting.parameters :refer :all]
              [clojure-backtesting.counter :refer :all]
              [clojure.string :as str]
              [clojure.pprint :as pprint]
              [java-time :as t]
              [clojure.java.io :as io]
              [clojure.math.numeric-tower :as math]))
  
(def lazy-mode (atom false))
(def dataset-col (atom {}))


(defn set-main
  "This function does the initialisation for the lazy mode."
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
            (set-date cur-date)

            ;; need to call next-day in large-data
            ;; ============ Direct Copying ============
            (let [date cur-date dataset (get (deref dataset-col) main-name)]
              (reset! available-tics {})
              (loop [count 0 remaining dataset]
                (if (or (<= MAXRANGE count) (empty? remaining))
                  (do
                    (println "Exceed the maximum-line buffer for one date or the dataset reaches the end.")
                    (swap! dataset-col assoc (or name main-name) remaining))
                  (let [first-line (first remaining)
                        next-remaining (rest remaining)
                        cur-date (get first-line :date)
                        ticker (get first-line :TICKER)]
         ;(println first-line)        
                    (if (and (not= cur-date date) (valid-line first-line))
             ;(reset! data-set remaining)
             ;(println "-----")
                      (do
             ;(println count)
                        (swap! dataset-col assoc (or name main-name) remaining)
                        )
                      (do
                        (swap! available-tics assoc ticker {:reference first-line})
                        (recur (inc count) next-remaining)))))))
            ;; ============= Direct Copy Ends ============
                       
            cur-date)
          (if (< count 2000)
            (recur (inc count) next-remaining)
            (do
              (swap! dataset-col assoc (or name main-name) remaining)
              (recur 0 next-remaining))))))))

  (defn check-filepath
    "This function checks if a csv file is already created."
    [filepath]
    (try (io/delete-file filepath)
         (catch Exception e (println (format "Detect that you run the program for the first time.\n We created a file named %s to store order records.
                             \n You can find the file under the same directory of your runnning program.\n" filepath))))) ;First delete the file (act as emptying)

  ;; Backtester initialisation
  (defn init-portfolio
    "This function initialises or restarts the backtester."
    [date capital &{:keys [standard] :or {standard true}}] ;; the dataset is the filtered dataset the user uses, as we need the number of days from it
    ;; example: portfolio -> {:cash {:tot-val 10000} :"AAPL" {:price 400 :aprc adj-price :quantity 100 :tot-val 40000}}
    ;; example: portfolio-value {:date 1980-12-16 :tot-value 50000 :daily-ret 0 :loan 0 :leverage 0}
    ;; todo: implement case when standard is false

    ;; output order record to csv file
    (check-filepath "./out_order_record.csv")
    (def wrtr (io/writer "./out_order_record.csv" :append true))
    (.write wrtr "date,TICKER,quantity\n")
    ;; output portfolio value record to csv file
    (check-filepath "./out_portfolio_value_record.csv")
    (def portvalue-wrtr (io/writer "./out_portfolio_value_record.csv" :append true))
    (.write portvalue-wrtr "date,tot-value,daily-ret,tot-ret,loan,leverage,margin\n")
    ;; output evaluation report to csv file
    (check-filepath "./out_evaluation_report.csv")
    (def evalreport-wrtr (io/writer "./out_evaluation_report.csv" :append true))
    (.write evalreport-wrtr "date,tot-value,vol,r-vol,sharpe,r-sharpe,pnl-pt,max-drawdown\n")

    (def order-record (atom []))
    (def init-capital capital)
    (def eval-report-data (atom [])) ; to store evaluation report (in string format, for printing)
    (def eval-record (atom [])) ; to store evaluation report (in number format)
    (def portfolio (atom {:cash {:tot-val capital}}))
    (def portfolio-value (atom [{:date date :tot-value capital :daily-ret 0.0 :tot-ret 0.0 :loan 0.0 :leverage 0.0 :margin 0.0}]))
    
    (if (not (deref lazy-mode))
      (do
        (init-date date)
        (maintain-tics true))
      (lazy-init date))

    ;; ============ Global switches for internal use ============
    (def LOAN-EXIST false) ; global swtich for storing whether loan exists
    (def terminated (atom false)) ; global switch for storing whether user has lost all cash
    )

  
  ;; Update loan in portfolio
  (defn update-loan
    [date loan is-interest]
    (if (= is-interest true)
      ;; deduct cash in portfolio
      (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) loan)}))
    ;; update portfolio-value record
    (let [tot-value (reduce + (map :tot-val (vals (deref portfolio))))
          prev-value (:tot-value (last (deref portfolio-value)))
          new-loan (+ loan (get (last (deref portfolio-value)) :loan)) ; update total loan
          new-leverage (/ new-loan tot-value) ; update leverage ratio = total debt / total equity
          cash (get-in (deref portfolio) [:cash :tot-val]) ; get total amount of cash
          new-margin (/ tot-value (+ tot-value new-loan)) ; calculate portfolio margin
          ret (* (log-10 (/ tot-value prev-value)) new-leverage) ; update return with formula: daily_ret_lev = log(tot_val/prev_val) * leverage
          tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)
          last-date (get (last (deref portfolio-value)) :date)
          ]
      (do
        (if (= LOAN-EXIST false) ; check loan-exist switch
          (def LOAN-EXIST true)) ; flip the switch
        (if (= last-date date) ; check if date already exists
          (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value))))) ; drop last entry in old portfolio-value vector
        
        ; update portfolio-value vector
        (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :leverage new-leverage :loan new-loan :margin new-margin})))
        (.write portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f\n" date (double tot-value) (double ret) (double tot-ret) (double new-leverage) (double new-loan) (double new-margin)))
        )))


  ;; Update the portfolio map
  (defn update-portfolio-map
    [date tic quantity price aprc loan]
  
    ;; check whether the portfolio already has the security
    (if-not (contains? (deref portfolio) tic) 
      (let [tot-val (* aprc quantity) 
            tot-val-real (* price quantity)]
        (do 
          (swap! portfolio (fn [curr-port] (conj curr-port [tic {:price price :aprc aprc :quantity quantity :tot-val tot-val}])))
          (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) tot-val)})))
      ;; if already has it, just update the quantity
      (let [[tot-val qty] [(* aprc quantity) (get-in (deref portfolio) [tic :quantity])] tot-val-real (* price quantity)] 
        (do
          (swap! portfolio assoc tic {:quantity (+ qty quantity) :tot-val (* aprc (+ qty quantity))})
          (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) tot-val)}))))
    
    ;; then update the price & aprc of the securities in the portfolio
    (doseq [[ticker -] (deref portfolio)] 
      (if (= ticker tic)
        (let [qty-ticker (get-in (deref portfolio) [ticker :quantity])]
            (if (or (= qty-ticker 0) (= qty-ticker 0.0))
              (swap! portfolio dissoc ticker) ; remove ticker from portfolio if qty = 0
              (swap! portfolio assoc ticker {:price price :aprc aprc :quantity qty-ticker :tot-val (* aprc qty-ticker)})))))
    )

  ;; Update the portfolio-value vector which records the daily portfolio value
  (defn update-portfolio-value-vector
    [date tic quantity price aprc loan]
    (let [[tot-value prev-value] ;; get portfolio total
          [(reduce + (map :tot-val (vals (deref portfolio)))) (:tot-value (last (deref portfolio-value)))]] 

      (if (and (not= prev-value 0) (not= prev-value 0.0)) ; check division by zero
        ; if prev_value not 0
        (if (or (not= loan 0) LOAN-EXIST)

          ; exist leverage
          (update-loan date loan false)
  
          ; no leverage, update return with log formula: daily_ret = log(tot_val/prev_val)
          (let [ret (log-10 (/ tot-value prev-value))
                tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)
                last-date (get (last (deref portfolio-value)) :date)]
            (do 
              (if (= last-date date) ; check if date already exists
                (swap! portfolio-value (fn [curr-port-val] (pop (deref portfolio-value))))) ; drop last entry in old portfolio-value vector
              (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :loan 0.0 :leverage 0.0 :margin 0.0})))
              (.write portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f\n" date (double tot-value) (double ret) (double tot-ret) (double 0.0) (double 0.0) (double 0.0)))))
        )
        ; if prev_value is 0, let ret = 0.0
        (let [ret 0.0
              tot-ret (+ (get (last (deref portfolio-value)) :tot-ret) ret)]
          (do 
            (swap! portfolio-value (fn [curr-port-val] (conj curr-port-val {:date date :tot-value tot-value :daily-ret ret :tot-ret tot-ret :loan 0.0 :leverage 0.0 :margin 0.0})))
            (.write portvalue-wrtr (format "%s,%f,%f,%f,%f,%f,%f\n" date (double tot-value) (double ret) (double tot-ret) (double 0.0) (double 0.0) (double 0.0)))))
      )))


  ;; Main function to update portfolio map + portfolio-value record when placing an order
  (defn update-portfolio
    [date tic quantity price aprc loan]
    (update-portfolio-map date tic quantity price aprc loan)
    (update-portfolio-value-vector date tic quantity price aprc loan))
  

  ;; Utility function
  (defn view-portfolio-record
    "This function prints the first n rows of the portfolio value record, pass -ve value to print whole record."
    [n]
    (def portfolio-record (atom [])) ; temporarily stores record for view
    (doseq [row (deref portfolio-value)] 
      (do
        (let [date (get row :date)
              tot-val (str "$" (format "%.2f" (float (get row :tot-value))))
              daily-ret (str (format "%.2f" (* (get row :daily-ret) 100)) "%")
              tot-ret (str (format "%.2f" (* (get row :tot-ret) 100)) "%")
              loan (str "$" (format "%.2f" (get row :loan)))
              leverage (str (format "%.2f" (get row :leverage)))
              margin (str (format "%.2f" (* (get row :margin) 100)) "%")]
          ; append formatted data to portfolio-record 
          (swap! portfolio-record conj
                 {:date date
                  :tot-value tot-val
                  :daily-ret daily-ret
                  :tot-ret tot-ret
                  :loan loan
                  :leverage leverage
                  :margin margin
                 }))))
    (if (> n 0)
      (pprint/print-table (take n (deref portfolio-record)))
      (pprint/print-table (deref portfolio-record)))
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
        (if (= ticker :cash)
          (do ;; cash
            (let [tot-val (format "%.2f" (float (get row :tot-val)))]
            (swap! portfolio-table conj
                    {:asset "cash"
                    :price "N/A"
                    :aprc "N/A"
                    :quantity "N/A"
                    :tot-val tot-val})))
          (do ;; ticker
            (let [price (get row :price)
                  aprc (format "%.4f" (get row :aprc))
                  quantity (get row :quantity)
                  tot-val (format "%.2f" (get row :tot-val))]
            (swap! portfolio-table conj
                    {:asset ticker
                    :price price
                    :aprc aprc
                    :quantity quantity
                    :tot-val tot-val}))))))

    (pprint/print-table (deref portfolio-table)))
