(ns clojure-backtesting.direct
  (:require [clojure.pprint :as pp]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.automation :refer :all]))

(defn- first-n
  "The first n rows, or all of them when n is nil or not positive."
  [n rows]
  (if (and n (> n 0)) (take n rows) rows))

(defn print-order-record
  [& [n]]
  (pp/print-table (first-n n (deref order-record))))

(defn print-automation-list
  []
  (let [data (deref automated-conditions)
        data (map (fn [key] (zipmap [:id :condition :action :max-dispatch :expire-date] (concat [key] (get data key)))) (sort (keys data)))
        data (map #(update-by-keys % [:condition :action] (fn [action] (:source (meta action)))) data)
        data (map #(update % :max-dispatch (fn [_] (or (deref _) "Unlimited"))) data)
        data (map #(update % :expire-date (fn [_] (or _ "Never"))) data)]
    (pp/print-table [:id :condition :action :max-dispatch :expire-date] data)))

(defn print-automation-history
  [& [n]]
  (pp/print-table [:date :automation] (first-n n (deref dispatch-history))))

(defn print-portfolio-record
  "This function prints the first n rows of the portfolio value record, pass -ve value to print whole record."
  [& [n]]
  (pp/print-table
   (first-n n
            (for [row (deref portfolio-value)]
              {:date (get row :date)
               :tot-value (str "$" (format "%.2f" (float (get row :tot-value))))
               :daily-ret (str (format "%.2f" (* (get row :daily-ret) 100)) "%")
               :tot-ret (str (format "%.2f" (* (get row :tot-ret) 100)) "%")
               :loan (str "$" (format "%.2f" (get row :loan)))
               :leverage (format "%.2f" (get row :leverage))
               :margin (str (format "%.2f" (* (get row :margin) 100)) "%")}))))

(defn print-portfolio
  "This function prints portfolio map in a table format."
  []
  (pp/print-table
   (for [[security row] (deref portfolio)]
     (if (= security :cash)
       {:asset "cash"
        :price "N/A"
        :aprc "N/A"
        :quantity "N/A"
        :tot-val (format "%.2f" (float (get row :tot-val)))}
       {:asset security
        :price (get row :price)
        :aprc (format "%.4f" (get row :aprc))
        :quantity (get row :quantity)
        :tot-val (format "%.2f" (get row :tot-val))}))))

(defn print-eval-report
  "This function prints the first n rows of the evaluation report, pass a -ve number to print full report."
  [& [n]]
  (pp/print-table (first-n n (deref eval-report-data))))
