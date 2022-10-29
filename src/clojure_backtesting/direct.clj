(ns clojure-backtesting.direct
  (:require [clojure.pprint :as pp]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.counter :refer :all] ;; [clojure.pprint :as pprint]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.automation :refer :all]))

;; (defn print-map
;;   [dir]
;;   (pp/print-table dir))

(defn print-order-record
  [& [n]]
  (pp/print-table (if (> n 0) (take n (deref order-record)) (deref order-record))))

(defn print-automation-list
  []
  (let [data (deref automated-conditions)
        data (map (fn [key] (zipmap [:id :condition :action :max-dispatch :expire-date] (concat [key] (get data key)))) (sort (keys data)))
        data (map #(update-by-keys % [:condition :action] (fn [action] (:source (meta action)))) data)
        data (map #(update % :max-dispatch (fn [_] (or (deref _) "Unlimited"))) data)
        data (map #(update % :expire-date (fn [_] (or  _ "Never"))) data)]
    (pp/print-table [:id :condition :action :max-dispatch :expire-date] data)))

(defn print-automation-history
  [& [n]]
  (pp/print-table [:date :automation] (if (> n 0) (take n (deref dispatch-history))(deref dispatch-history))))

(defn print-portfolio-record
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
                :margin margin}))))
  (if (> n 0)
    (pp/print-table (take n (deref portfolio-record)))
    (pp/print-table (deref portfolio-record))))

(defn print-portfolio
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

  (pp/print-table (deref portfolio-table)))

;; ============ Record inspection ============

;; Print evaluation report
(defn print-eval-report
  "This function prints the first n rows of the evaluation report, pass a -ve number to print full report."
  [& [n]]
  (if (> n 0)
    (pp/print-table (take n (deref eval-report-data))) ;; only print first n rows
    (pp/print-table (deref eval-report-data))))