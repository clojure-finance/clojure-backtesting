(ns clojask-script.core
  (:require [clojask.dataframe :as ck]
            [clojask.api.gb-aggregate :as gb-agg]
            [clojask.onyx-comps :refer [start-onyx-groupby]]
            [clojure.java.io :as io]))

;; (def input-file "/Users/lyc/Desktop/RA clojure/data-sorted-cleaned/data-CRSP.csv")
(def input-file "/Users/lyc/Downloads/joined-compustat.csv")
(def output-dir "/Volumes/T7/Compustat/")
;; (def initial-price (atom {}))
;; ;; record cumulative return for each security
;; (def cum-ret (atom {}))
(defn log-10 [n]
  (/ (Math/log n) (Math/log 10)))

;; (defn aprc
;;   [date prc ret security]
;;   (let [date date
;;         price (Double/parseDouble prc)
;;         ret (Double/parseDouble ret)
;;         security security]
;;           ;; check whether the initial-price map already has the security
;;     (if-not (contains? (deref initial-price) security)
;;       (do ;; security appears the first time 
;;         (swap! initial-price (fn [security-map] (conj security-map [security {:price price}])))
;;         (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret ret}]))))
;;             ;; security does not appear the first time
;;       (let [prev-cumret (get-in (deref cum-ret) [security :cumret])
;;             log-ret (log-10 (+ 1 ret)) ; log base 10 
;;             ]
;;         (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret (+ log-ret prev-cumret)}])))))

;;     (def security-initial-price (get-in (deref initial-price) [security :price]))
;;     (def curr-cumret (get-in (deref cum-ret) [security :cumret]))
;;     (def aprc (* security-initial-price (Math/pow Math/E curr-cumret)))
;;     aprc))

;; (defn aprc
;;   [date prc ret security]
;;   (let [date date
;;         price (Double/parseDouble prc)
;;         ret (Double/parseDouble ret)
;;         security security]
;;           ;; check whether the initial-price map already has the security
;;     (if-not (contains? (deref initial-price) security)
;;       (do ;; security appears the first time 
;;         (swap! initial-price (fn [security-map] (conj security-map [security {:price price}])))
;;         (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret ret}]))))
;;             ;; security does not appear the first time
;;       (let [prev-cumret (get-in (deref cum-ret) [security :cumret])
;;             log-ret (log-10 (+ 1 ret)) ; log base 10 
;;             ]
;;         (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret (+ log-ret prev-cumret)}])))))

;;     (def security-initial-price (get-in (deref initial-price) [security :price]))
;;     (def curr-cumret (get-in (deref cum-ret) [security :cumret]))
;;     (def aprc (* security-initial-price (Math/pow Math/E curr-cumret)))

;;     (assoc line :INIT-PRICE security-initial-price :APRC aprc :CUM-RET curr-cumret)))

;; (defn aprc
;;   [date prc ret security]
;;   (let [date date
;;         price (Double/parseDouble prc)
;;         ret (Double/parseDouble ret)
;;         security security]
;;           ;; check whether the initial-price map already has the security
;;     (if-not (contains? (deref initial-price) security)
;;       (do ;; security appears the first time 
;;         (swap! initial-price (fn [security-map] (conj security-map [security {:price price}])))
;;         (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret ret}]))))
;;             ;; security does not appear the first time
;;       (let [prev-cumret (get-in (deref cum-ret) [security :cumret])
;;             log-ret (log-10 (+ 1 ret)) ; log base 10 
;;             ]
;;         (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret (+ log-ret prev-cumret)}])))))

;;     (def security-initial-price (get-in (deref initial-price) [security :price]))
;;     (def curr-cumret (get-in (deref cum-ret) [security :cumret]))
;;     (def aprc (* security-initial-price (Math/pow Math/E curr-cumret)))

;;     (assoc line :INIT-PRICE security-initial-price :APRC aprc :CUM-RET curr-cumret)))

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

;; (defn -main
;;   []
;;   (let [df1 (ck/dataframe "/Volumes/T7/data-sorted-cleaned/data-Compustat.csv")
;;         df2 (ck/dataframe "/Users/lyc/Downloads/data-to-Leo.csv")
;;         df-join (ck/inner-join df1 df2 ["gvkey" "tic" "datadate"] ["gvkey" "tic" "datadate"])]
;;     (ck/compute df-join 8 "../resources/Compustat.csv")
;;     ))
