(ns clojure-backtesting.large-data
  (:require [clojure-backtesting.data :refer :all]
            ;[clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            ;[clojure-backtesting.evaluate :refer :all]
            ;[clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clj-time.core :as clj-t]
            [java-time :as t]))

(def dataset-col (atom {}))

(defn set-main
  "Doing the initialzation for the lazy mode."
  [name]
  (def pending-order (atom {}))
  (def main-name name)
  (reset! lazy-mode true))

(defn- lazy-read-csv
  [csv-file]
  (let [in-file (io/reader csv-file)
        csv-seq (csv/read-csv in-file)
        lazy (fn lazy [wrapped]
               (lazy-seq
                 (if-let [s (seq wrapped)]
                   (cons (first s) (lazy (rest s)))
                   )))]
    (lazy csv-seq)))

(defn read-csv-lazy
  "Read CSV data into memory by row"
  [filename]
  (csv->map (lazy-read-csv filename)))

(defn load-large-dataset
  [address name]
  (swap! dataset-col assoc name (add-aprc (read-csv-lazy address)))
  true
  )

(defn curr-date
  "returns the curr date"
  [& [name key]]
  (get (first (get (deref dataset-col) (or name main-name))) (or key :date)))

(defn curr-tic
  [& [name key]]
  (get (first (get (deref dataset-col) (or name main-name))) (or key :TICKER)))

(defn get-line
  [& name]
    (first (get (deref dataset-col) (or name main-name))))

(defn- check-line
  "Check the line for possible tradeing in the pending list"
  [line]
  (let [tic (get line :TICKER) date (get line :date) info (get (deref pending-order) tic)]
    (if (not= info nil)
      (if (>= (compare (t/format "yyyy-MM-dd" (t/plus (t/local-date "yyyy-MM-dd" (get info :date)) (t/days (get info :expiration)))) date) 0)
        (order-internal date tic (get info :quantity) (get info :remaining) (get info :leverage) [line] (get info :print) (get info :direct))
        )
      (swap! pending-order dissoc tic))))

(defn next-tic
  "Go to the next tic after the current line."
  [tic & [name]]
  (loop [count 0 remaining (rest (get (deref dataset-col) (or name main-name)))]
     (if (or (<= MAXRANGE count) (empty? remaining))
       (do
        (println "Cannot find the requested ticker within MAXRANGE or the file reaches the end.")
         nil)
       (let [first-line (first remaining)
             next-remaining (rest remaining)
             cur-tic (get first-line :TICKER)]
         (check-line first-line) ; Do not forget this!!!!
         ;(println first-line)
         (if (= cur-tic tic)
             ;(reset! data-set remaining)
             ;(println "-----")
           (do
             ;(println count)
             (swap! dataset-col assoc (or name main-name) remaining)
             first-line)
           (recur (inc count) next-remaining)))))
  )

(defn next-day
  "Go to the next day of the dataset."
  [& name]
  (let [date (curr-date)]
   (loop [count 0 remaining (rest (get (deref dataset-col) (or name main-name)))]
     (if (or (<= MAXRANGE count) (empty? remaining))
       (do
         (println "Cannot find a date later than today or the file reaches the end.")
         nil)
       (let [first-line (first remaining)
             next-remaining (rest remaining)
             cur-date (get first-line :date)]
         (check-line first-line) ; Do not forget this!!!!
         ;(println first-line)
         (if (not= cur-date date)
             ;(reset! data-set remaining)
             ;(println "-----")
           (do
             ;(println count)
             (swap! dataset-col assoc (or name main-name) remaining)
             first-line)
           (recur (inc count) next-remaining))))))
  )

(defn next-line
  "Go to the next line of the dataset"
  [& name]
  (let [name (or name main-name)]
    (check-line (first (get (deref dataset-col) name)))
    (swap! dataset-col assoc name (rest (get (deref dataset-col) name)))
    (first (get (deref dataset-col) name))))


;; (defn next-date
;;   "Go to the next date available in the dateset"
;;   []
;;   )

(defn lazy-traverse
  [name & {:keys [peek ticker max-cover address next-line] :or {peek false ticker nil max-cover 100000 address nil next-line -1}}]
  ;; (if address
  ;;   (def large-data (add-aprc (read-csv-lazy address))))
  (println "-------")
  (loop [count 0 remaining (get (deref dataset-col) name) cur-date nil cur-tic nil]
     (if (and (>= max-cover count) (empty? remaining))
       (do
        (println "end of file")
         nil)
       (let [first-line (first remaining)
             next-remaining (rest remaining)
             date (get first-line :date)
             tic (get first-line :TICKER)]
         ;(println first-line)
         (if (not= cur-date date)
           (do
             ;(reset! data-set remaining)
             ;(println "-----")
             (if (not= cur-date "nil")
               (do
                 (println count)
                 (println date)
                 (swap! dataset-col assoc "large" remaining)
                 (recur (inc count) next-remaining date tic))))
            (recur (inc count) next-remaining cur-date tic))))))

(defn order-lazy 
;;  ([quantity & {:keys [remaining leverage print direct] :or {remaining false leverage LEVERAGE print false direct true}}]
;;   (order-lazy (curr-tic) quantity :remaining remaining :leverage leverage :print print :direct direct))
 ([tic quantity & {:keys [expiration remaining leverage dataset print direct] :or {expiration ORDER-EXPIRATION remaining false leverage LEVERAGE dataset (deref data-set) print false direct true}}]
  (swap! pending-order assoc tic {:date (curr-date) :expiration expiration :quantity quantity :remaining remaining :leverage leverage :print print :direct direct})))
