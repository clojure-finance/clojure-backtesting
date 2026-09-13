(ns clojure-backtesting.data-management
  (:require [clojure-backtesting.counter :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.parameters :refer :all]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; cache management
(defn- cache-pop
  "reset information of the oldest date"
  []
  (let [date (.poll cache-queue)]
    (reset! (nth (get data-cache date) 0) nil)
    (reset! (nth (get data-cache date) 1) nil)))

(defn- cache-add-info
  [date info]
  (while (>= (.size cache-queue) CACHE-SIZE) (cache-pop))
  (.add cache-queue date)
  (reset! (nth (get data-cache date) 0) info))

(defn- cache-add-map
  [date info]
  (reset! (nth (get data-cache date) 1) info))

(def ^:private compustat-cache
  "[datadate {security-id row}] for the most recently used Compustat file."
  (atom nil))

(defn- get-compustat-data
  "Returns the Compustat rows dated `date`, indexed by TICKER-KEY.
   Only the last date is cached, which is enough because consecutive trading
   days almost always map to the same filing date."
  [date]
  (let [[cached-date index] (deref compustat-cache)]
    (if (= cached-date date)
      index
      (let [index (with-open [rdr (io/reader (get data-files2 date))]
                    (into {}
                          (map (fn [line]
                                 (let [row (zipmap headers2 (edn/read-string line))]
                                   [(TICKER-KEY row) row])))
                          (line-seq rdr)))]
        (reset! compustat-cache [date index])
        index))))

(defn- compare-two-date [date1 date2]
  (let [date1-list (clojure.string/split date1 #"-")
        date2-list (clojure.string/split date2 #"-")
        difference (+ (* (- (Integer/parseInt (nth date1-list 0)) (Integer/parseInt (nth date2-list 0))) 12) (- (Integer/parseInt (nth date1-list 1)) (Integer/parseInt (nth date2-list 1))))]
    (if (and (<= difference 3) (>= difference -3))
      true
      false)))
(defn merge-data
  "Left-joins the latest Compustat filing dated at or before `date` onto every
   CRSP row, matching on TICKER-KEY. Rows without a match are returned
   unchanged. Nothing is joined when there is no earlier filing yet or the
   latest one is more than three months old. Note the join keys on the
   filing's period-end date, not on when it became public, so there is still
   some look-ahead between period end and the announcement date."
  [crsp date]
  (let [comp-date (first (first (rsubseq data-files2 <= date)))]
    (if (and comp-date (compare-two-date date comp-date))
      (let [comp (get-compustat-data comp-date)]
        (mapv (fn [row] (merge row (get comp (TICKER-KEY row)))) crsp))
      crsp)))

(defn- get-info-by-date
  "Get the full tics info.\n
   Returns a vector of maps."
  [date]
  (if-let [file (get data-files date)]
    (if-let [ret (deref (nth (get data-cache date) 0))]
      ;; cache hit
      ret
      ;; cache miss
      (let [data (with-open [rdr (io/reader file)]
                   (mapv #(zipmap headers (edn/read-string %)) (line-seq rdr)))
            data (if headers2 (merge-data data date) data)]
        (cache-add-info date data)))
    nil))

(defn- get-info-map-by-date
  [date]
  (if-let [ret (deref (nth (get data-cache date) 1))]
    ;; cache hit
    ret
    ;; cache miss
    (if-let [info (get-info-by-date date)]
      (cache-add-map date (zipmap (map TICKER-KEY info) info))
      nil)))

(defn get-info
  "Returns the whole information for the all the tics today.\n
   A sequence of maps."
  []
  (get-info-by-date (get-date)))

(defn permno-tic
  [permno]
  (mapv :TICKER (filter #(= permno (:PERMNO %)) (get-info))))

(defn tic-permno
  [tic]
  (mapv :PERMNO (filter #(= tic (:TICKER %)) (get-info))))

(defn get-info-map
  "Returns the whole information for the all the tics today.\n
   A map of permno:info."
  [& [info]]

  (if info
    (zipmap (map TICKER-KEY info) info)
    (get-info-map-by-date (get-date))))

(defn available-permnos
  "Gets all available permnos today in a sequence."
  []
  (keys (get-info-map)))

(defn get-permno-info
  "Returns the information for the specified permno today.\n
   A map if any, otherwise nil."
  ([permno]
   (get (get-info-map) permno))
  ([date permno]
   (get (get-info-map-by-date date) permno)))

(defn get-permno-price
  "Returns the price of a given security today, otherwise nil."
  ([permno]
   (PRICE-KEY (get-permno-info permno)))
  ([date permno]
   (PRICE-KEY (get-permno-info date permno))))

(defn get-permno-by-key
  "Returns the value of the key of a given security today, otherwise nil."
  ([permno key]
   (get (get-permno-info permno) key))
  ([date permno key]
   (get (get-permno-info date permno) key)))

(defn get-prev-n-days
  "Returns a sequence of sequence of maps that contains data of the previous n days (not including today).\n
   Date in descending order, ie from the most recent to the oldest.\n
   If no n, return a lazy sequence of all prev days.
   "
  ;n  number of counting ahead
  ([]
   (let [date (get-date)
         dates (map first (rsubseq data-files < date))]
     (map get-info-by-date dates)))
  ([n]
   (let [date (get-date)
         dates (take n (map first (rsubseq data-files < date)))]
     (map get-info-by-date dates))))

(defn get-prev-n-days-map
  "Returns a sequence of maps (security: info) of maps that contains data of the previous n days (not including today).\n
   Date in descending order, ie from the most recent to the oldest.\n
   If no n, return a lazy sequence of all prev days.
   "
  ;n  number of counting ahead
  ([]
   (let [date (get-date)
         dates (map first (rsubseq data-files < date))]
     (map get-info-map-by-date dates)))
  ([n]
   (let [date (get-date)
         dates (take n (map first (rsubseq data-files < date)))]
     (map get-info-map-by-date dates))))

(defn get-permno-prev-n-days
  "This function returns a sequence of vector of the previous n records of a specific security (not including today).\n
   Date in descending order, ie from the most recent to the oldest.\n
   Note that the returned length may be smaller than n, if the security is missing on some days.\n
   @permno: name of the stock\n
   @n: number of counting ahead"
  [permno n]
  (let [data (get-prev-n-days-map n)]
    (filter #(not= % nil) (map #(get % permno) data))))
