(ns clojure-backtesting.user
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.automation :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure-backtesting.indicators :refer :all]
            [clojure-backtesting.direct :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            ;; [clj-time.core :as clj-t]
            ;; [clojure.edn :as edn]
            ;; [java-time :as jt]
            ;; [clojupyter.kernel.version :as ver]
            )(:gen-class))

(defn -main
  "Write your code here"
  [& args]
  (load-dataset "/Volumes/T7/CRSP" "main")
  ;; (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
  ;(reset! data-set (add-aprc (read-csv-row "./resources/data-CRSP-lohi-extract-1000.csv")))
  (init-portfolio "1986-01-09" 2000)
  (let [tot-val (get-in (deref portfolio) [:cash :tot-val])]
    (println tot-val))

  ;; (order "AAPL" 10)
  ;(order "OMFGA" 10)
  (next-date)

  (update-eval-report (get-date))
  (next-date)
  (update-eval-report (get-date))

  (println (portfolio-daily-ret))
  (next-date)
  (next-date)
  (next-date)
  (next-date)
  (next-date)

  (pprint/print-table (deref order-record))
  (view-portfolio)
  (view-portfolio-record -1)
  (eval-report -1)

  ;; (println (sd-last-n-days "OMFGA" 10))
  ;; (let [prev-close (Double/parseDouble (get (first (get-prev-n-days :PRC 1 "OMFGA")) :PRC))]
  ;;   (println (parabolic-SAR "OMFGA" "non-lazy" 0.2 prev-close))
  ;;   )

  ;; (let [low-price (Double/parseDouble (get-by-key "OMFGA" :BIDLO "non-lazy"))
  ;;       high-price (Double/parseDouble (get-by-key "OMFGA" :ASKHI "non-lazy"))
  ;;       prev-atr (- high-price low-price)]
  ;;   (println (keltner-channel "OMFGA" "non-lazy" 10 prev-atr))
  ;;   )

  ;; (println (force-index "OMFGA" "non-lazy" 20))

  ;; (end-order)
  )
