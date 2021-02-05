(ns clojure-backtesting.portfolio
    (:require [clojure-backtesting.data :refer :all]
              [clojure-backtesting.parameters :refer :all]
              [clojure-backtesting.counter :refer :all]
              [clojure.string :as str]
              [clojure.pprint :as pprint]
              [java-time :as t]
              [clojure.java.io :as io]
              [clojure.math.numeric-tower :as math])
      )
  
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

  ;; Backtester initialisation
  (defn init-portfolio
    "This is the function that initialise or restart the backtesting process"
    [date init-capital &{:keys [standard] :or {standard true}}] ;; the dataset is the filtered dataset the user uses, as we need the number of days from it
    ;; example: portfolio -> {:cash {:tot-val 10000} :"AAPL" {:price 400 :aprc adj-price :quantity 100 :tot-val 40000}}
    ;; example: portfolio-value {:date 1980-12-16 :tot-value 50000 :daily-ret 0 :loan 0 :leverage 0}
    ;; todo: implement case when standard is false
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
          (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) tot-val)})
        )
      )
      ;; if already has it, just update the quantity
      (let [[tot-val qty] [(* aprc quantity) (get-in (deref portfolio) [tic :quantity])] tot-val-real (* price quantity)] 
        (do 
          (swap! portfolio assoc tic {:quantity (+ qty quantity) :tot-val (* aprc (+ qty quantity))})
          (swap! portfolio assoc :cash {:tot-val (- (get-in (deref portfolio) [:cash :tot-val]) tot-val)})
        )
      )
    )
    
    ;; then update the price & aprc of the securities in the portfolio
    (doseq [[ticker -] (deref portfolio)] 
      (if (= ticker tic)
        (let [qty-ticker (get-in (deref portfolio) [ticker :quantity])]
          (do 
            (if (or (= qty-ticker 0) (= qty-ticker 0.0))
              (swap! portfolio dissoc ticker) ; remove ticker from portfolio if qty = 0
              (swap! portfolio assoc ticker {:price price :aprc aprc :quantity qty-ticker :tot-val (* aprc qty-ticker)})
            )
          )
        )
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
                  ret (* (log-10 (/ tot-value prev-value)) new-leverage) ; update return with formula: daily_ret_lev = log(tot_val/prev_val) * leverage
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
            (let [ret (log-10 (/ tot-value prev-value))
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
    "This function prints the first n rows of the portfolio value record, pass -ve value to print whole record."
    [n]
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
        
          (swap! portfolio-record conj
                 {:date date
                  :tot-value tot-val
                  :daily-ret daily-ret
                  :tot-ret tot-ret
                  :loan loan
                  :leverage leverage}) 
        )
      )
    )
    
    (if (or n (> n 0))
      (pprint/print-table (take n (deref portfolio-record)))
      (pprint/print-table (deref portfolio-record))
    )
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
              (swap! portfolio-table conj
                     {:asset "cash"
                      :price "N/A"
                      :aprc "N/A"
                      :quantity "N/A"
                      :tot-val tot-val}) 
              )
            )
            (do
              (let [price (get row :price)
                aprc (format "%.2f" (get row :aprc))
                quantity (get row :quantity)
                tot-val (int (get row :tot-val))
               ]
              (swap! portfolio-table conj
                     {:asset ticker
                      :price price
                      :aprc aprc
                      :quantity quantity
                      :tot-val tot-val}) 
              )
            )
          )
        )
      )
    
    (pprint/print-table (deref portfolio-table))
  )
