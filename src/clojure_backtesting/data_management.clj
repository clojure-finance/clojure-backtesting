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
  "file path -> {security-id row} for recently used Compustat files."
  (atom {}))

(defn- compustat-index
  "The rows of the Compustat file dated `datadate`, indexed by TICKER-KEY."
  [datadate]
  (let [file (get data-files2 datadate)
        path (str file)]
    (or (get (deref compustat-cache) path)
        (let [index (with-open [rdr (io/reader file)]
                      (into {}
                            (map (fn [line]
                                   (let [row (zipmap headers2 (edn/read-string line))]
                                     [(TICKER-KEY row) row])))
                            (line-seq rdr)))]
          (swap! compustat-cache (fn [cache] (assoc (if (< (count cache) 8) cache {}) path index)))
          index))))

(defn- months-between
  "Whole months from `from` to `to`, both yyyy-MM-dd strings; negative when
   `to` is the earlier date."
  [from to]
  (let [[y1 m1] (map #(Integer/parseInt %) (take 2 (str/split from #"-")))
        [y2 m2] (map #(Integer/parseInt %) (take 2 (str/split to #"-")))]
    (+ (* 12 (- y2 y1)) (- m2 m1))))

(defn- public-by?
  "Whether a Compustat row was public on `date`: its :rdq (report date) is
   on or before `date`, or it has none."
  [row date]
  (let [rdq (:rdq row)]
    (or (nil? rdq)
        (str/blank? (str rdq))
        (<= (compare (str rdq) date) 0))))

(defn merge-data
  "Left-joins onto every CRSP row the latest Compustat row for the same
   TICKER-KEY that was public on `date`: among the filings dated at or
   before `date`, at most MERGE-MAX-AGE-MONTHS old and at most four back,
   newest first, the first row whose :rdq (report date) is on or before
   `date`. Without an :rdq column the filing date itself is used. Rows
   with no match are returned unchanged."
  [crsp date]
  (let [datadates (->> (rsubseq data-files2 <= date)
                       (map first)
                       (take-while #(<= (months-between % date) MERGE-MAX-AGE-MONTHS))
                       (take 4))
        indexes (map compustat-index datadates)]
    (if (empty? datadates)
      crsp
      (mapv (fn [row]
              (let [id (TICKER-KEY row)
                    comp (some (fn [index]
                                 (let [c (get index id)]
                                   (when (and c (public-by? c date)) c)))
                               indexes)]
                (merge row comp)))
            crsp))))

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
  "{security-id row} for `date`, or nil for a date not in the dataset."
  [date]
  (when-let [entry (get data-cache date)]
    (if-let [ret (deref (nth entry 1))]
      ;; cache hit
      ret
      ;; cache miss
      (when-let [info (get-info-by-date date)]
        (cache-add-map date (zipmap (map TICKER-KEY info) info))))))

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
