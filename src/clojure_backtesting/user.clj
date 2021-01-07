(ns clojure-backtesting.user
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.specs :refer :all]
            [clojure-backtesting.counter :refer :all]
            ;;[clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clj-time.core :as clj-t]
            [java-time :as t]
            ;[clojure.spec.alpha :as s]
            [clojupyter.kernel.version :as ver]
            )(:gen-class))

;;testing purpose
; (comment 
; (def filex "/home/kony/Documents/GitHub/clojure-backtesting/resources/Compustat-extract.csv")

; (def a (read-csv-row filex))
; )        

; maintain a map that contains the necessary information about each ticker

; main function for reading dataset
(defn main-read-data
    [& args]
    (println args)
    (reset! data-set (read-csv-row (first args)))
    (pprint/print-table (deref data-set))
)

(defn -main
  "Write your code here"
    [& args] ; pass ./resources/CRSP-extract.csv as arg
    (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
    (init-portfolio "1980-12-15" 10000)
    (time (do (def MA50-vec [])
              (def MA200-vec [])
              (while (not= (get-date) "2000-02-10")
                (do
        ;; write your trading strategy here
                  (def tics (deref available-tics-)) ;20 ms
                  (def MA50-vec (get-prev-n-days :PRC 50 "AAPL" MA50-vec (get (get tics "AAPL") :reference)))
        ;(println (get-date))
        ;(println MA50-vec)
                  (def MA200-vec (get-prev-n-days :PRC 200 "AAPL" MA200-vec (get (get tics "AAPL") :reference)))
        ;(println MA200-vec)
                  (let [[MA50 MA200] [(moving-average :PRC MA50-vec) (moving-average :PRC MA200-vec)]]
                    (if (> MA50 MA200)
                      (order "AAPL" 1 :reference (get (get tics "AAPL") :reference) :print true) ;1ms
                      (order "AAPL" 0 :remaining true :reference (get (get tics "AAPL") :reference) :print true))) ;1ms
                  ;(update-eval-report (get-date))
                  (next-date)))))
 )

;;sample activation command:
;;lein run "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv"
