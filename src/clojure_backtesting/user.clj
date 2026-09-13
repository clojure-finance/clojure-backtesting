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
  "A short run against the bundled sample dataset. Pass a directory to use
   another dataset in the same layout."
  [& [dir]]
  (println (load-dataset (or dir "resources/sample-data/main") "main" add-aprc))
  (init-portfolio "1990-01-02" 100000)
  (order "10001" 100) ; fills at the next close
  (order "10002" 50)
  (dotimes [_ 10]
    (next-date)
    (update-eval-report))
  (order "10001" -50)
  (dotimes [_ 5]
    (next-date)
    (update-eval-report))
  (end-order)
  (print-order-record)
  (print-portfolio)
  (print-portfolio-record -1)
  (print-eval-report -1))
