(ns clojure-backtesting.user
  (:require ;[clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.specs :refer :all]
            [clojure-backtesting.counter :refer :all]
            ;[clojure-backtesting.large-data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clj-time.core :as clj-t]
            [clojure.edn :as edn]
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
    ; test with ordering
    ;(order "AAPL" 50)

    ; goldren cross
    ; (time (do (def MA50-vec-aapl [])
    ;       (def MA200-vec-aapl [])
    ;       (def MA50-vec-f [])
    ;       (def MA200-vec-f [])
    ;       (while (not= (get-date) "1981-12-29")
    ;         (do
    ;           (def tics (deref available-tics-)) ;20 ms
    ;           (def MA50-vec-aapl (get-prev-n-days :PRC 50 "AAPL" MA50-vec-aapl (get (get tics "AAPL"):reference)))
    ;           (def MA200-vec-aapl (get-prev-n-days :PRC 200 "AAPL" MA200-vec-aapl (get (get tics "AAPL") :reference)))
    ;           (def MA50-vec-f (get-prev-n-days :PRC 50 "F" MA50-vec-f (get (get tics "F"):reference)))
    ;           (def MA200-vec-f (get-prev-n-days :PRC 200 "F" MA200-vec-f (get (get tics "F") :reference)))
    ;           (let [[MA50 MA200] [(moving-average :PRC MA50-vec-aapl) (moving-average :PRC MA200-vec-aapl)]]
    ;             (if (> MA50 MA200)
    ;               (order "AAPL" 1 :reference (get (get tics "AAPL") :reference) :print false) 
    ;               (order "AAPL" 0 :remaining true :reference (get (get tics "AAPL") :reference))))
    ;           (let [[MA50 MA200] [(moving-average :PRC MA50-vec-f) (moving-average :PRC MA200-vec-f)]]
    ;             (if (> MA50 MA200)
    ;               (order "F" 1 :reference (get (get tics "F") :reference) :print false) 
    ;               (order "F" 0 :remaining true :reference (get (get tics "F") :reference))))
    ;           (next-date)))))
    ; (.close wrtr)
    ; (pprint/print-table (deref order-record))
    ; (view-portfolio)
    ; (view-portfolio-record)
    ; (update-eval-report (get-date))
    ; (eval-report)

    ; fundamental analysis
    (defn get-set
      "return a set of maps of tickers and datadate" ;;{:tic "AAPL", :datadate "1981/3/31", :ROE x.x}
      [file date]
      (loop [remaining file
              result-set []
              ROE-list [] ]
          (if (empty? remaining)
              ;; how to change the output here?
              ;;(into #{} (conj result-set {:ROE-set ROE-list}))
              {:data result-set :ROE-set ROE-list}
              (let [first-line (first remaining)
                  next-remaining (rest remaining)
                  ;;next-result-set (conj result-set (get first-line :datadate)
                  [year month day] (map parse-int (str/split date #"-"))]
                  ;;merged file use :datadate as key
                  (if (= date (get first-line :datadate))
                      (let [[niq PRC cshoq] (map edn/read-string [(get first-line :niq)(get first-line :PRC)(get first-line :cshoq)])
                            ROE (/ niq (* PRC cshoq))]
                          (recur next-remaining (conj result-set {:tic (get first-line :tic) :year year :ROE ROE}) (conj ROE-list ROE))
                      )
                      (recur next-remaining result-set ROE-list)
                  )  
              )
          )
      )
    )
  
  (defn get-ROE
    "return a set of ROE"  ;;{10.2 1.8 x.x ...}
    [dataset]
    (into #{} (get dataset :ROE-set))
    )

    (defn get-roe-20
      "return a set tickers" ;;{"AAPL" "GM"}
      [data roe-20]
      (loop [remaining (get data :data)
              result-set []]
          (if (empty? remaining)
              (into #{} result-set)
              (let [first-line (first remaining)
                  next-remaining (rest remaining)]
                  ;;conj the stock ticker whose ROE >= roe-20
                  ;;(if (not= -1 (compare roe-20 (get first-line :ROE)))
                  (if (>= (get first-line :ROE) roe-20)
                      (recur next-remaining (conj result-set (get first-line :tic)))           
                      (recur next-remaining result-set)            
                  )  
              )
          )
      )
    )

    (init-portfolio "1980-12-18" 100000)

      (def start-date (get-date))
      ;; time span = n days
      (def time-span 5) 

      ;; get stock tickers and ROE data
      ;; output {{:tic "AAPL" :year "1980" :ROE x.x}{...}{:ROE 1.x 2.x 3.x 10.x}}
      (def stock-data (get-set (read-csv-row "./resources/data-testing-merged.csv") "1980-12-18"))
      
      ;; 1. sort the stocks according to their ROE (= Net Income/Total Equity)
      
      ;; get a set of ROE
      (def roe-list (get-ROE stock-data))
      
      ;; sorting function, return tickers of the top 20% stocks
      ;; determine what are the top 20% stocks and turn the sorted-set into list format
      (def roe-sorted (into '[] (apply sorted-set roe-list)))
      
      ;(println roe-list)
      ;(println (int (* 0.8 (count roe-sorted))))
      
      ;;find the 20% cut-off ROE value
      (let [roe-20 (nth roe-sorted (int (* 0.8 (count roe-sorted))))]    
          ;;get the tickers of the top 20 with function get-roe-20
          (def stocks-to-buy (get-roe-20 stock-data roe-20))
      )
      
      ;; 2. buy the top 20% stocks and the sell the stocks that are not in the top 20% this year      
      (def stocks-to-buy-list (into [] stocks-to-buy))
      ;(println stocks-to-buy-list)
           
      (doseq [stock stocks-to-buy-list]        
        (order stock 10)
      )
              
    
      (while (not= time-span 0)
          ;; check whether the start day is in the dataset
          ;; find the quarterly date before inputting into get-set function
          (do 
              ; move on to the next trading day
              (next-date)
              ; decrement time span
              (println (get-date))
              (def time-span (dec time-span))
          )
      )
      
      ;; 3. sell all stocks by the end of time span
      (doseq [stock stocks-to-buy-list]        
        (order stock -10)
      )
      
      ; update the eval-report
      (update-eval-report (get-date))

      (pprint/print-table (deref order-record))
      (view-portfolio)
      (view-portfolio-record)
      (eval-report)
 )

;;sample activation command:
;;lein run "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv"
