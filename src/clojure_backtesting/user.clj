(ns clojure-backtesting.user
  (:require ;[clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.specs :refer :all]
            [clojure-backtesting.counter :refer :all]
            ;[clojure-backtesting.parameters :refer :all]
            ;[clojure-backtesting.large-data :refer :all]
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
    (init-portfolio "1980-12-15" 100000)
    ;(order "AAPL" 50)

    (time (do (def MA50-vec-aapl [])
          (def MA200-vec-aapl [])
          (def MA50-vec-f [])
          (def MA200-vec-f [])
          (while (not= (get-date) "1981-12-15")
            (do
              ;; write your trading strategy here
              (def tics (deref available-tics-)) ;20 ms
              (def MA50-vec-aapl (get-prev-n-days :PRC 50 "AAPL" MA50-vec-aapl (get (get tics "AAPL"):reference)))
              (def MA200-vec-aapl (get-prev-n-days :PRC 200 "AAPL" MA200-vec-aapl (get (get tics "AAPL") :reference)))
              (def MA50-vec-f (get-prev-n-days :PRC 50 "F" MA50-vec-f (get (get tics "F"):reference)))
              (def MA200-vec-f (get-prev-n-days :PRC 200 "F" MA200-vec-f (get (get tics "F") :reference)))
              (let [[MA50 MA200] [(moving-average :PRC MA50-vec-aapl) (moving-average :PRC MA200-vec-aapl)]]
                (if (> MA50 MA200)
                  (order "AAPL" 1 :reference (get (get tics "AAPL") :reference) :print false) 
                  (order "AAPL" 0 :remaining true :reference (get (get tics "AAPL") :reference))))
              (let [[MA50 MA200] [(moving-average :PRC MA50-vec-f) (moving-average :PRC MA200-vec-f)]]
                (if (> MA50 MA200)
                  (order "F" 1 :reference (get (get tics "F") :reference) :print false) 
                  (order "F" 0 :remaining true :reference (get (get tics "F") :reference))))
              (next-date)))))
    
    (.close wrtr)
    (pprint/print-table (deref order-record))
    (view-portfolio-record)
    
    (update-eval-report (get-date))
    (eval-report)
 )

;;sample activation command:
;;lein run "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv"
