(ns clojure-backtesting.user
  (:require ;[clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.specs :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.large-data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clj-time.core :as clj-t]
            [clojure.edn :as edn]
            [java-time :as t]
            [clojupyter.kernel.version :as ver]
            )(:gen-class))

(defn -main
  "Write your code here"
  [& args]

  (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
  (init-portfolio "1980-12-15" 200)
  (order "AAPL" 10)
  (next-date)

  ;; (order "AAPL" 10)

  (update-eval-report (get-date))
  (next-date)
  (update-eval-report (get-date))

  (println (portfolio-daily-ret))
  ;; (next-date)
  ;; (update-eval-report (get-date))
  ;; (next-date)
  ;; (next-date)
  ;; (update-eval-report (get-date))
  ;; (next-date)
  ;; (update-eval-report (get-date))
  ;; (next-date)
  ;; (update-eval-report (get-date))
  ;; (next-date)
  ;; (update-eval-report (get-date))
  ;; (next-date)
  ;; (update-eval-report (get-date))
  (next-date)
  (next-date)
  (next-date)
  (println (portfolio-daily-ret))

  (pprint/print-table (deref order-record))
  (view-portfolio)
  (view-portfolio-record -1)
  (eval-report -1)

  (end-order)
)
