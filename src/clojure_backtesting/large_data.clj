(ns clojure-backtesting.large-data
  (:require [clojure-backtesting.data :refer :all]
            ;[clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.portfolio :refer :all]
            ;[clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
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

(defn load-large-dataset
  [address name & [extra-function]]
  (if extra-function
    (swap! dataset-col assoc name (extra-function (read-csv-lazy address)))
    (swap! dataset-col assoc name (read-csv-lazy address)))
  true
  )

(defn- curr-date
  "returns the curr date"
  [& [name key]]
  (get (first (get (deref dataset-col) (or name main-name))) (or key :date)))

(defn curr-tic
  [& [name key]]
  (get (first (get (deref dataset-col) (or name main-name))) (or key :TICKER)))

(defn get-line
  [& [name]]
    (first (get (deref dataset-col) (or name main-name))))

(defn- check-line
  "Check the line for possible tradeing in the pending list"
  [line]
  (let [tic (get line :TICKER) date (get line :date) info (get (deref pending-order) tic)]
    (if (not= info nil)
      (if (>= (compare (t/format "yyyy-MM-dd" (t/plus (t/local-date "yyyy-MM-dd" (get info :date)) (t/days (get info :expiration)))) date) 0)
        (let [order (order-internal date tic (get info :quantity) (get info :remaining) (get info :leverage) [line] (get info :print) (get info :direct))]
          (if order
            (do
              (swap! order-record conj order)
              (swap! pending-order dissoc tic))))
        (swap! pending-order dissoc tic)) ;; Delete the expired order
      )
    (if (get (deref portfolio) tic) ;; update portfolio daily
      (order-internal (get-date) tic "special" false false [line] false false))))

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
  [& [name]]
  (let [date (curr-date name) dataset (get (deref dataset-col) (or name main-name))]
    (reset! available-tics {})
    (if (empty? dataset)
      (do
        (println "Cannot find a date later than today or the file reaches the end.")
        nil)
      (do
        (if (or (not name) (= name main-name))
         (set-date date))
        (loop [count 0 remaining dataset]
          (if (or (<= MAXRANGE count) (empty? remaining))
            (do
              (println "Exceed the maximum-line buffer for one date or the dataset reaches the end.")
              (swap! dataset-col assoc (or name main-name) remaining))
            (let [first-line (first remaining)
                  next-remaining (rest remaining)
                  cur-date (get first-line :date)
                  ticker (get first-line :TICKER)]
              (check-line first-line) ; Do not forget this!!!!
         ;(println first-line)        
              (if (and (not= cur-date date) (valid-line first-line))
             ;(reset! data-set remaining)
             ;(println "-----")
                (do
             ;(println count)
                  (swap! dataset-col assoc (or name main-name) remaining)
                  date)
                (do
                  (swap! available-tics assoc ticker {:reference first-line})
                  (recur (inc count) next-remaining)))))))))
  )

(defn next-line
  "Go to the next line of the dataset"
  [& [name]]
  (loop [count 0 remaining (rest (get (deref dataset-col) (or name main-name)))]
    (if (empty? remaining)
      (do
        (println "You have reached the end of the file")
        nil)
      (let [first-line (first remaining)
            next-remaining (rest remaining)]
        (check-line first-line) ; Do not forget this!!!!
         ;(println first-line)
        (if (valid-line first-line)
             ;(reset! data-set remaining)
             ;(println "-----")
          (do
             ;(println count)
            (swap! dataset-col assoc (or name main-name) remaining)
            first-line)
          (if (< count 1000)
            (recur (inc count) next-remaining)
            (do
              (swap! dataset-col assoc (or name main-name) remaining)
              (recur 0 next-remaining))))))))


(defn next-date
  "Wrapper function for next-day in large-data and internal-next-date for counter."
  []
  (if (deref lazy-mode)
    (next-day)
    (do
      (updateHoldingTickers)
      (internal-next-date)
      (checkTerminatingCondition)
    )
  )
)

(defn order-lazy 
;;  ([quantity & {:keys [remaining leverage print direct] :or {remaining false leverage LEVERAGE print false direct true}}]
;;   (order-lazy (curr-tic) quantity :remaining remaining :leverage leverage :print print :direct direct))
 ([tic quantity & {:keys [expiration remaining leverage dataset print direct] :or {expiration ORDER-EXPIRATION remaining false leverage LEVERAGE dataset (deref data-set) print false direct true}}]
  (swap! pending-order assoc tic {:date (get-date) :expiration expiration :quantity quantity :remaining remaining :leverage leverage :print print :direct direct})))

;; ============ Supporting functions for Compustat ============

(defn get-compustat-line
  "Get the corresponding line from Compustat for the input line in CRSP"
  ;; (get-compustat-line (get (get (deref available-tics) "IBM") :reference) "compustat")
  [line compustat]
  ;; line is the line in CRSP
  ;; compustat is the name of dataset
  (let [date (get line :date) tic (get line :TICKER) lower (t/format "yyyy-MM-dd" (t/minus (t/local-date "yyyy-MM-dd" date) (t/years 1))) upper (t/format "yyyy-MM-dd" (t/minus (t/local-date "yyyy-MM-dd" date) (t/months 3)))]
    (loop [count 0 data (get (deref dataset-col) compustat)]
      ;; Loop body
      (let [first-line (first data)
            remaining (rest data)
            line-date (get first-line :datadate)]
        (if (> (compare line-date lower) 0)
          (swap! dataset-col assoc compustat data) ;; jump to the first line of the compustat
          (if (> count 5000) ; limit the size of compustat to 5000 * 200 bytes
            (do
              (swap! dataset-col assoc compustat data)
              (recur 0 remaining))
            (recur (inc count) remaining))))
      )
    ;; start at the first line of the earliest time
    (loop [data (get (deref dataset-col) compustat) line {} niq "" cshoq ""]
      (let [first-line (first data)
            remaining (rest data)
            line-date (get first-line :datadate)
            ticker (get first-line :tic)
            new-niq (get first-line :niq)
            new-cshoq (get first-line :cshoq)]
        (if (< (compare line-date upper) 0) ;; still eariler than three months
          (if (= ticker tic)
            (if (or new-niq new-cshoq)
              (if (and new-niq new-cshoq)
                (recur remaining first-line new-niq new-cshoq)
                (if new-niq
                  (recur remaining first-line new-niq cshoq)
                  (recur remaining first-line niq new-cshoq)))
              (recur remaining first-line niq cshoq))
            (recur remaining line niq cshoq))
          (merge line {:niq niq :cshoq cshoq}))
        ))
    ))