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
            [criterium.core :as criterium]
            [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
            [clojupyter.kernel.version :as ver]
            )(:gen-class))

;;testing purpose
; (comment 
; (def filex "/home/kony/Documents/GitHub/clojure-backtesting/resources/Compustat-extract.csv")

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

    (defonce my-sacc (tufte/add-accumulating-handler! "*"))
    (defonce my-sacc-drainer
      ;; Will drain and print formatted stats every minute
      (future
        (while true
          (when-let [m (not-empty @my-sacc)]
            (println (tufte/format-grouped-pstats m)))
          (Thread/sleep 6000)
        )
      ))

    ; request to send `profile` stats to `println`
    ; (def my-printer (tufte/add-basic-println-handler! ;; tufte profiler
    ;   {:format-pstats-opts {:columns [:n-calls :max :mean :mad :clock :total]}}))
    
    (reset! data-set (add-aprc (read-csv-row "./resources/CRSP-extract.csv")))

    (profile {} (dotimes [_ 1]
      ; wrap functions to profile with p, pass an id
      (do
        (p :init-portfolio (init-portfolio "1980-12-15" 10000))
        (p :order (order "AAPL" 1))
        (p :update-eval-report (update-eval-report (get-date)))
        (p :eval-report (eval-report))
      )
    ))
  
    ;; Criterium example
    ;(criterium/quick-bench (eval-report))
 )