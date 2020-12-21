(ns clojure-backtesting.user
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
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
    ;(println args)
    ; (reset! data-set (add-aprc (read-csv-row (first args))))
    ; ; (println (take 20 (deref data-set)))
    ; (init-portfolio "1980-12-16" 5000)
    ; (order-internal "1980-12-16" "AAPL" 30)
    ; (order-internal "1980-12-16" "IBM" 20)
    ; (update-eval-report "1980-12-16")
    ; (order-internal "1980-12-17" "IBM" -10)
    ; (update-eval-report "1980-12-17")
    ; (order-internal "1981-12-10" "IBM" 20)
    ; (update-eval-report "1985-12-18")
    ; (println (deref portfolio))
    ; (println (deref portfolio-value))
    ; (println (deref order-record)); (println (take 20 (deref data-set)))

    (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
    (init-portfolio "1980-12-16" 10000);

    ; (def num-of-days (atom 10))                              
    ; (while (pos? @num-of-days)
    ;     (do 
    ;         (if (= 10 @num-of-days)
    ;             (do
    ;                 (order "AAPL" 50) ; buy 50 stocks
    ;                 (println ((fn [date] (str "Buy 50 stocks of AAPL on " date)) (get-date)))
    ;             )
    ;         )
    ;         (if (odd? @num-of-days)
    ;             (do
    ;                 (order "AAPL" -10) ; sell 10 stocks
    ;                 (println ((fn [date] (str "Sell 10 stocks of AAPL on " date)) (get-date)))
    ;             )
    ;         )
    ;         (update-eval-report (get-date))
    ;         (next-date)
    ;         (swap! num-of-days dec)
    ;     )
    ; )
    (order "1980-12-16" "AAPL" 30)
    (order "1980-12-16" "IBM" 20)
    (update-eval-report "1980-12-16")
    
    ; (println (deref portfolio)) 
    (view-portfolio) ;; display it in a table 
    
    (pprint/print-table (deref order-record))
    
    (view-portfolio-record) ;; display portfolio value vector in table
    (eval-report)  
 )

;;sample activation command:
;;lein run "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv"
