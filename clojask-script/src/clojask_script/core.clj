(ns clojask-script.core
  (:require [clojask.dataframe :as ck]
            [clojask.api.gb-aggregate :as gb-agg]
            [clojask.onyx-comps :refer [start-onyx-groupby]]
            [clojure.java.io :as io]))

;; (def input-file "/Users/lyc/Desktop/RA clojure/data-sorted-cleaned/data-CRSP.csv")
(def input-file "/Users/lyc/Desktop/RA clojure/data-sorted-cleaned/data-Compustat.csv")
(def output-dir "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/Compustat/")
;; (def initial-price (atom {}))
;; ;; record cumulative return for each ticker
;; (def cum-ret (atom {}))
(defn log-10 [n]
  (/ (Math/log n) (Math/log 10)))

;; (defn aprc
;;   [date prc ret ticker]
;;   (let [date date
;;         price (Double/parseDouble prc)
;;         ret (Double/parseDouble ret)
;;         ticker ticker]
;;           ;; check whether the initial-price map already has the ticker
;;     (if-not (contains? (deref initial-price) ticker)
;;       (do ;; ticker appears the first time 
;;         (swap! initial-price (fn [ticker-map] (conj ticker-map [ticker {:price price}])))
;;         (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret ret}]))))
;;             ;; ticker does not appear the first time
;;       (let [prev-cumret (get-in (deref cum-ret) [ticker :cumret])
;;             log-ret (log-10 (+ 1 ret)) ; log base 10 
;;             ]
;;         (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret (+ log-ret prev-cumret)}])))))

;;     (def ticker-initial-price (get-in (deref initial-price) [ticker :price]))
;;     (def curr-cumret (get-in (deref cum-ret) [ticker :cumret]))
;;     (def aprc (* ticker-initial-price (Math/pow Math/E curr-cumret)))
;;     aprc))

;; (defn aprc
;;   [date prc ret ticker]
;;   (let [date date
;;         price (Double/parseDouble prc)
;;         ret (Double/parseDouble ret)
;;         ticker ticker]
;;           ;; check whether the initial-price map already has the ticker
;;     (if-not (contains? (deref initial-price) ticker)
;;       (do ;; ticker appears the first time 
;;         (swap! initial-price (fn [ticker-map] (conj ticker-map [ticker {:price price}])))
;;         (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret ret}]))))
;;             ;; ticker does not appear the first time
;;       (let [prev-cumret (get-in (deref cum-ret) [ticker :cumret])
;;             log-ret (log-10 (+ 1 ret)) ; log base 10 
;;             ]
;;         (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret (+ log-ret prev-cumret)}])))))

;;     (def ticker-initial-price (get-in (deref initial-price) [ticker :price]))
;;     (def curr-cumret (get-in (deref cum-ret) [ticker :cumret]))
;;     (def aprc (* ticker-initial-price (Math/pow Math/E curr-cumret)))

;;     (assoc line :INIT-PRICE ticker-initial-price :APRC aprc :CUM-RET curr-cumret)))

;; (defn aprc
;;   [date prc ret ticker]
;;   (let [date date
;;         price (Double/parseDouble prc)
;;         ret (Double/parseDouble ret)
;;         ticker ticker]
;;           ;; check whether the initial-price map already has the ticker
;;     (if-not (contains? (deref initial-price) ticker)
;;       (do ;; ticker appears the first time 
;;         (swap! initial-price (fn [ticker-map] (conj ticker-map [ticker {:price price}])))
;;         (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret ret}]))))
;;             ;; ticker does not appear the first time
;;       (let [prev-cumret (get-in (deref cum-ret) [ticker :cumret])
;;             log-ret (log-10 (+ 1 ret)) ; log base 10 
;;             ]
;;         (swap! cum-ret (fn [ticker-map] (conj ticker-map [ticker {:cumret (+ log-ret prev-cumret)}])))))

;;     (def ticker-initial-price (get-in (deref initial-price) [ticker :price]))
;;     (def curr-cumret (get-in (deref cum-ret) [ticker :cumret]))
;;     (def aprc (* ticker-initial-price (Math/pow Math/E curr-cumret)))

;;     (assoc line :INIT-PRICE ticker-initial-price :APRC aprc :CUM-RET curr-cumret)))

;; (defn -main
;;   "Main function"
;;   []
;;   (println (str "Input file: " input-file))
;;   (println (str "Output dir: " output-dir))
;;   (let [df (ck/dataframe input-file)
;;         headers (mapv keyword (ck/get-col-names df))]
;;     (ck/set-type df "PRC" "double")
;;     (ck/set-type df "RET" "double")
;;     (ck/group-by df "date")
;;     (io/make-parents (str output-dir "grouped/" "header"))
;;     (spit (str output-dir "header") (str headers))
;;     (let [groupby-keys (.getGroupbyKeys (:row-info df))
;;           groupby-index (vec (take (count headers) (iterate inc 0)))
;;           res (start-onyx-groupby 8 300 df (str output-dir "grouped/") groupby-keys groupby-index false)]
;;       (if (= res "success")
;;         (do
;;           (println "Dataset grouped successfully.")))))
  
;;   )
(defn -main
  "Main function"
  []
  (println (str "Input file: " input-file))
  (println (str "Output dir: " output-dir))
  (let [df (ck/dataframe input-file)
        headers (mapv keyword (ck/get-col-names df))]
    ;; (ck/set-type df "PRC" "double")
    ;; (ck/set-type df "RET" "double")
    (ck/group-by df "datadate")
    (io/make-parents (str output-dir "grouped/" "header"))
    (spit (str output-dir "header") (str headers))
    (let [groupby-keys (.getGroupbyKeys (:row-info df))
          groupby-index (vec (take (count headers) (iterate inc 0)))
          res (start-onyx-groupby 8 300 df (str output-dir "grouped/") groupby-keys groupby-index false)]
      (if (= res "success")
        (do
          (println "Dataset grouped successfully."))))))
