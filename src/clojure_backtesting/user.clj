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
    ;; (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))
    ;; (init-portfolio "1980-12-15" 100000)
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
  (load-large-dataset "../data-sorted-cleaned/data-CRSP-sorted-cleaned.csv" "main" add-aprc-by-date)
  (set-main "main")
;; load compustat
  (load-large-dataset "../data-sorted-cleaned/data-Compustat-sorted-cleaned.csv" "compustat")
  (init-portfolio "1971-12-18" 100000)
  (defn get-roe-map
    "return a set of maps of tickers and datadate"
    [tic]
;;     (println tic)
    (let [line (get (get (deref available-tics) tic) :reference)
          first-line (merge line (get-compustat-line line "compustat"))]
      (if (= (count first-line) 0)
        nil
        (let [[niq PRC cshoq] [(get first-line :niq) (get first-line :PRC) (get first-line :cshoq)]]
;;                 (println first-line)
          (if (and (> (count niq) 0) (and (> (count PRC) 0) (> (count cshoq) 0)))
            (let [[niq PRC cshoq] (map edn/read-string [niq PRC cshoq])]
;;                         (println (* PRC cshoq))
              (if (not= (- 0.0 (* PRC cshoq)) 0.0)
                {:tic tic :ROE (/ niq (* PRC cshoq))}
                nil))
            nil)))))

  (defn remove-nil
    [vec]
    (loop [res [] data vec]
      (if (empty? data)
        res
        (let [line (first data)
              remaining (rest data)]
          (if (= line nil)
            (recur res remaining)
            (recur (conj res line) remaining))))))

  (defn get-stocks-to-buy
    []
    (map (fn [roe-map] (get roe-map :tic)) (take-last 20 (sort-by :ROE (map get-roe-map (keys (deref available-tics)))))))
  (def year-count 1) ;; hold the stock for 3 years
  (def start-year 1970)
  (def rebalance-md (subs (get-date) 4)) ; = -12-18

  (def rebalance-years (into [] (range (+ start-year 1) (+ (+ start-year 1) year-count) 1))) ; rebalance every year

  (def rebalance-dates []) ; [1981-12-18, 1982-12-18, 1983-12-18]
  (doseq [year rebalance-years]
    (def rebalance-dates (conj rebalance-dates (str year rebalance-md))))

  (def end-date (last rebalance-dates)) ; 1983-12-18

  (println rebalance-dates)

  (while (not= (empty? rebalance-dates) true)
  ;(println (get-date)) ; debug
    (if (>= (compare (get-date) (first rebalance-dates)) 0) ; check if (get-date) has passed first date in rebalance-dates
      (do
        (def rebalance-dates (rest rebalance-dates)) ; pop the first date in rebalance-dates
      ;(println (rest rebalance-dates)) ; debug

        (def stocks-to-buy-list (get-stocks-to-buy))
        (println stocks-to-buy-list)
      ;; sell stocks held in portfolio
        (doseq [[ticker row] (deref portfolio)]
          (if (not= ticker :cash)
            (order-lazy ticker 0 :remaining true)))

      ;; buy stocks
        (doseq [stock stocks-to-buy-list]
          (order-lazy stock 10 :expiration 3))

        (update-eval-report (get-date)) ; update evaluation metrics
        ))
    (next-date) ; move on to the next trading day
    )
    (end-order)
    )
