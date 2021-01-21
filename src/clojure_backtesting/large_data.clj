(ns clojure-backtesting.large-data
  (:require [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clj-time.core :as clj-t]
            [java-time :as t]))

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

(defn next-tic
  "Go to the next tic after the current line."
  (
   [])
  ([tic ]
   ))

(defn curr-date
  "returns the date of the curr date"
  []
  )

;; (defn next-date
;;   "Go to the next date available in the dateset"
;;   []
;;   )

(defn test-init
  [address]
  (def dataset-col (atom {}))
  (swap! dataset-col assoc "large" (add-aprc (read-csv-lazy address))))

(defn lazy-traverse
  [& {:keys [peek ticker max-cover address next-line] :or {peek false ticker nil max-cover 100000 address nil next-line -1}}]
  (if address
    (def large-data (add-aprc (read-csv-lazy address))))
  (println "-------")
  (loop [count 0 remaining large-data cur-date nil cur-tic nil]
     (if (and (>= max-cover count) (empty? remaining))
       (do
        (println "end of file")
         nil)
       (let [first-line (first remaining)
             next-remaining (rest remaining)
             date (get first-line :date)
             tic (get first-line :TIC)]
         ;(println first-line)
         (if (not= cur-date date)
           (do
             ;(reset! data-set remaining)
             ;(println "-----")
             (if (not= cur-date "nil")
               (do
                 (println count)
                 (println date)
                 (def large-data remaining)
                 (recur (inc count) next-remaining date))))
            (recur (inc count) next-remaining cur-date))))))
