(ns clojask-script.core
  (:require [clojask.dataframe :as ck]
            [clojask-io.input :as input]
            [clojask.api.gb-aggregate :as gb-agg]
            [clojask.onyx-comps :refer [start-onyx-groupby]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; =============== Configurable Parameters =====================
;; Please change the below parameters based on the instructions
;; in the README.md file.

;; compulsory parameters
(def input-file "/Volumes/T7/data-sorted-cleaned/data-CRSP.csv")
(def output-dir "CRSP")
(def type-of-dataset "CRSP")
(def security-identifier "PERMNO")
(def date-identifier "date")
(def data-format-string "yyyy-MM-dd")

;; optional parameters for certain type
(def closing-price "PRC")
(def opening-price "OPENPRC")
(def return-identifier "RET")
(def daily-min-price nil)
(def daily-max-price nil)


;; =============== END of Configurable Parameters ==============


;; =============== Do not change the code below ================

;; (def input-file "/Users/lyc/Desktop/RA clojure/data-sorted-cleaned/data-CRSP.csv")
;; (def input-file "/Users/lyc/Downloads/joined-compustat.csv")
;; (def output-dir "/Volumes/T7/Compustat/")
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

(defn CRSP-action
  []
  (assert (and input-file output-dir type-of-dataset security-identifier date-identifier data-format-string) "Please input all the compulsory parameters.")
  (assert (and return-identifier (or closing-price opening-price)) "Please input the return identifier and at least one price identifier.")
  (let [df (ck/dataframe input-file)]
    (when opening-price
      (ck/set-type df opening-price "double")
      (ck/rename-col df opening-price "OPENPRC"))
    (when closing-price
      (ck/set-type df closing-price "double")
      (ck/rename-col df closing-price "PRC"))
    (ck/set-type df return-identifier "double")
    (ck/rename-col df return-identifier "RET")
    (when daily-min-price
      (ck/rename-col df daily-min-price "BIDLO")
      (ck/set-type df daily-min-price "double"))
    (when daily-max-price
      (ck/rename-col df daily-max-price "ASKHI")
      (ck/set-type df daily-max-price "double"))
    (ck/rename-col df security-identifier "PERMNO")
    (ck/rename-col df date-identifier "date")
    (ck/group-by df "date")
    (io/make-parents (str output-dir "grouped/" "header"))
    (let [headers (mapv keyword (ck/get-col-names df))
          tmp (spit (str output-dir "header") (str headers))
          groupby-keys (.getGroupbyKeys (:row-info df))
          groupby-index (vec (take (count headers) (iterate inc 0)))
          res (start-onyx-groupby 8 300 df (str output-dir "grouped/") groupby-keys groupby-index false)]
      (if (= res "success")
        (do
          (println "Dataset generated successfully."))))))

(defn Compustat-action
  []
  (assert (and input-file output-dir type-of-dataset security-identifier date-identifier data-format-string) "Please input all the compulsory parameters.")
  (let [df (ck/dataframe input-file)]
    (ck/rename-col df security-identifier "PERMNO")
    (ck/rename-col df date-identifier "datadate")
    (ck/group-by df "datadate")
    (io/make-parents (str output-dir "grouped/" "header"))
    (let [headers (mapv keyword (ck/get-col-names df))
          tmp (spit (str output-dir "header") (str headers))
          groupby-keys (.getGroupbyKeys (:row-info df))
          groupby-index (vec (take (count headers) (iterate inc 0)))
          res (start-onyx-groupby 8 300 df (str output-dir "grouped/") groupby-keys groupby-index false)]
      (if (= res "success")
        (do
          (println "Dataset generated successfully."))))))

(defn -main
  "Main function"
  []
  (def output-dir (if (str/ends-with? output-dir "/") output-dir (str output-dir "/")))
  (println (str "Input file: " input-file))
  (println (str "Output dir: " output-dir))
  (cond
    (= type-of-dataset "CRSP") (CRSP-action)
    (= type-of-dataset "Compustat") (Compustat-action)
    :else (throw (Exception. "Undefined type of dataset."))))

;; (defn -main
;;   []
;;   (let [df1 (ck/dataframe "/Volumes/T7/data-sorted-cleaned/data-Compustat.csv")
;;         df2 (ck/dataframe "/Users/lyc/Downloads/data-to-Leo.csv")
;;         df-join (ck/inner-join df1 df2 ["gvkey" "tic" "datadate"] ["gvkey" "tic" "datadate"])]
;;     (ck/compute df-join 8 "../resources/Compustat.csv")
;;     ))
