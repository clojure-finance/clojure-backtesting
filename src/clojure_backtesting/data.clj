(ns clojure-backtesting.data
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure-backtesting.parameters :refer :all]
            [clojure.string :as str])
  (:import [java.util PriorityQueue]
           [java.util Base64]))

(defn update-by-keys
  "Update values in a map by applying function (f) on keys"
  [map keys f]
  (reduce (fn [map k] (update map k f)) map keys))

(defn ->double
  "Coerces a number or a numeric string to a double. Returns nil for nil or
   a blank string, so optional columns the preprocessor left as strings can
   be read without knowing their type."
  [x]
  (cond (number? x) (double x)
        (and (string? x) (not (str/blank? x))) (Double/parseDouble x)
        :else nil))

(defn mean
  "Arithmetic mean of a non-empty sequence of numbers."
  [xs]
  (/ (reduce + 0.0 xs) (count xs)))

(defn sample-sd
  "Sample standard deviation (n-1 denominator); 0.0 for fewer than two values."
  [xs]
  (let [n (count xs)]
    (if (< n 2)
      0.0
      (let [m (mean xs)]
        (Math/sqrt (/ (reduce + 0.0 (map #(let [d (- % m)] (* d d)) xs))
                      (dec n)))))))

;; cache
(def data-cache {})
(def cache-queue (PriorityQueue.))

;; Global Variables for the dataset
(def data-files {})
(def data-files2 {})
(def headers nil)
(def headers2 nil)

(def decoder (Base64/getUrlDecoder))
(defn decode-str
  [s]
  (String. (.decode decoder s)))

(defn decode-filename
  [filename]
  (first (edn/read-string (decode-str filename))))

;; Global functions to set the variables
(defn get-file-date
  "The date a grouped data file holds, decoded from its name."
  [file]
  (decode-filename (.getName file)))

(defn load-dataset
  [dir name & [func]]
  (let [dir (if (str/ends-with? dir "/") dir (str dir "/"))
        file-dir (io/file (str dir "grouped"))
        files (rest (vec (file-seq file-dir)))
        file-date (mapv get-file-date files)
        header (edn/read-string (slurp (str dir "header")))
        _headers (mapv keyword header)
        _data-files (into (sorted-map) (zipmap file-date files))
        tmp (if func (func dir _headers _data-files)) ;; change the file by the function
        header (edn/read-string (slurp (str dir "header")))
        _headers (mapv keyword header)
        _data-files (into (sorted-map) (zipmap file-date files))]
    (cond
      (= name "main") (do
                        (def headers _headers)
                        (def data-files _data-files)
                        (def data-cache (into (sorted-map) (zipmap file-date (map (fn [_] [(atom nil) (atom nil)]) file-date))))
                        (str "Date range: " (first (first data-files)) " ~ " (first (last data-files))))
      (= name "compustat") (do
                             (def headers2 _headers)
                             (def data-files2 _data-files)
                             (str "Date range: " (first (first data-files2)) " ~ " (first (last data-files2)))))))

;; for each security:
;; add col 'cum-ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum-ret)
(def initial-price (atom {}))
;; record cumulative return for each security
(def cum-ret (atom {}))

(defn add-aprc-file
  "Appends INIT-PRICE, APRC and CUM-RET to every row of one daily file.
   CUM-RET is the cumulative natural-log return since the security's first
   appearance (0.0 on that first day), so APRC = INIT-PRICE * exp(CUM-RET)
   and APRC(t) / APRC(t-1) = 1 + RET(t). Per-security state lives in the
   `initial-price` and `cum-ret` atoms, which `add-aprc` resets."
  [data price-index ret-index security-index]
  (mapv (fn [line]
          (let [price (get line price-index)
                ret (get line ret-index)
                security (get line security-index)]
            (if (contains? (deref initial-price) security)
              (swap! cum-ret update-in [security :cumret]
                     + (if (number? ret) (Math/log (+ 1.0 ret)) 0.0))
              (do ;; security appears the first time
                (swap! initial-price assoc security {:price price})
                (swap! cum-ret assoc security {:cumret 0.0})))
            (let [init (get-in (deref initial-price) [security :price])
                  cumret (get-in (deref cum-ret) [security :cumret])
                  aprc (* init (Math/exp cumret))]
              (vec (concat line [init aprc cumret])))))
        data))

(defn add-aprc
  "Data augmentation of adding aprc to each file"
  [dir headers data-files]
  (if (and (.contains headers :INIT-PRICE) (.contains headers :APRC) (.contains headers :CUM-RET))
    (println "The dataset is already furnished by add-aprc. No more modification is needed.")
    (let [price-index (.indexOf headers :PRC)
          ret-index (.indexOf headers :RET)
          security-index (.indexOf headers TICKER-KEY)]
      (reset! initial-price {})
      (reset! cum-ret {})
      (println "The below process will take a few hours to run for the first time.")
      (doseq [[date file] data-files]
        (let [data (with-open [rdr (io/reader file)]
                     (set (map edn/read-string (line-seq rdr))))
              new-data (add-aprc-file data price-index ret-index security-index)]
          (spit file (str/join "\n" (map str new-data)))
          (println date)))
      (spit (str dir "header") (str (vec (concat headers [:INIT-PRICE :APRC :CUM-RET])))))))
