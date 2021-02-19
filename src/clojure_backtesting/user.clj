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
            ;[clojure.spec.alpha :as s]
            [clojupyter.kernel.version :as ver]
            )(:gen-class))

(defn -main
  "Write your code here"
    [& args] ; pass ./resources/CRSP-extract.csv as arg
    (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
    ;; (init-portfolio "1980-12-15" 100000)

    ;; ; test with ordering
    ;; (order "AAPL" 100)
    ;; (update-eval-report (get-date))
    ;; (next-date)
    ;; (next-date)
    ;; (next-date)
    ;; (next-date)
    ;; (next-date)
    ;; (order "AAPL" 100)
    ;; (update-eval-report (get-date))


    ;; ;; golden cross 
    ;; ;; (def MA50-vec-aapl [])
    ;; ;; (def MA200-vec-aapl [])
    ;; ;; (def MA50-vec-f [])
    ;; ;; (def MA200-vec-f [])
    ;; ;; (while (not= (get-date) "1983-12-29")
    ;; ;; (do
    ;; ;;   (def tics (deref available-tics)) ;20 ms
    ;; ;;   (def MA50-vec-aapl (get-prev-n-days :PRC 50 "AAPL" MA50-vec-aapl))
    ;; ;;   (def MA200-vec-aapl (get-prev-n-days :PRC 200 "AAPL" MA200-vec-aapl))
    ;; ;;   (def MA50-vec-f (get-prev-n-days :PRC 50 "F" MA50-vec-f ))
    ;; ;;   (def MA200-vec-f (get-prev-n-days :PRC 200 "F" MA200-vec-f))
    ;; ;;   (let [[MA50 MA200] [(moving-average :PRC MA50-vec-aapl) (moving-average :PRC MA200-vec-aapl)]]
    ;; ;;     (if (> MA50 MA200)
    ;; ;;       (order "AAPL" 1 :print false) 
    ;; ;;       (order "AAPL" 0 :remaining true))))
    ;; ;;   (let [[MA50 MA200] [(moving-average :PRC MA50-vec-f) (moving-average :PRC MA200-vec-f)]]
    ;; ;;     (if (> MA50 MA200)
    ;; ;;       (order "F" 1  :print false) 
    ;; ;;       (order "F" 0 :remaining true )))
    ;; ;;   (update-eval-report (get-date))
    ;; ;;   (next-date))

    ;; ;; (end-order)

    ;; ;(pprint/print-table (deref order-record))
    ;; (view-portfolio)
    ;; (view-portfolio-record -1)
    ;; (eval-report -1)
    ;; (end-order)

    ;; test with new add-aprc-by-date
    ;; (reset! data-set (add-aprc-by-date (read-csv-row "./resources/CRSP-extract-sorted.csv")))
    ;; (println (take 3 (deref data-set)))
  
    (init-portfolio "1980-12-15" 10)

    (order "AAPL" 10)
    (update-eval-report (get-date))

    (next-date)
    (next-date)
    (next-date)

    (order "AAPL" -40)

    (next-date)
    (next-date)
    ;(next-date)
    ;(next-date)
    ;(next-date)

    ;(end-order)

    (pprint/print-table (deref order-record))
    (view-portfolio)
    (view-portfolio-record -1)
    (eval-report -1)
  

    ;; big dataset
    ;; ;(init-portfolio "1980-12-15" 100000)
    ;; (load-large-dataset "../data-sorted-cleaned/data-CRSP-sorted-cleaned.csv" "main" add-aprc-by-date)
    ;; ;; load compustat
    ;; (load-large-dataset "../data-sorted-cleaned/data-Compustat-sorted-cleaned.csv" "compustat")
    ;; (set-main "main")
    ;; (init-portfolio "1963-02-05" 10)
    ;; (get-compustat-line (get-line) "compustat")
    ;; (order-lazy "PPL" 10 :print true)
    ;; ;; (time (while
    ;; ;;       (< (compare (get-date) "1965-02") 0)
    ;; ;;         ;(order-lazy "PPL" 10 :direct false)
    ;; ;;         (next-day)))

)
