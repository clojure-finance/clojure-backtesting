(ns clojure-backtesting.data
  (:require [clojure.data.csv :as csv] ;; Useful for CSV handling
            [clojure.java.io :as io]
            [clojure.set :as set]      ;;
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure-backtesting.parameters :refer :all]
            [clojure.pprint :as pprint]
            [clojure.string :as str])  ;; For input-output handling
  (:import [java.util PriorityQueue]
           [java.util Base64]))

;; This file is to construct the basic data structure for backtesting 

;; (def data-set (atom [])) ;; main dataset (to be changed by the user)

(defn csv->map
  "Convert parsed CSV vectors into maps with headers as keys, by row"
  [csv-data]
  (map zipmap ;; make the first row as headers and the following rows as values in a map structure e.g. {:tic AAPL} 
       (->> (first csv-data) ;; take the first row of the csv-data
            (map keyword) ;; make the header be the "key" in the map 
            repeat)      ;; repeat the process for all the headers
       (rest csv-data))) ;; use the rest rows as values of the map

(defn- csv->map-col
  "Convert parsed CSV vectors into maps with headers as keys, by column"
  [csv-data]
  (zipmap
   (->> (first csv-data)
        (map keyword))
   (apply map vector (rest csv-data))))

(defn update-by-keys
  "Update values in a map by applying function (f) on keys"
  [map keys f]
  (reduce (fn [map k] (update map k f)) map keys))

(defn parse-int
  "Parse integer from string. May return nil."
  [str]
  (try (Integer/parseInt str)
       (catch Exception e nil)))

; Returns a list of maps, e.g. ({:col1 1, :col2 2} {:col1 3, :col2 4})
(defn read-csv-row
  "Read CSV data into memory by row"
  [filename]
  (with-open [reader (io/reader filename)]
    (->> (csv/read-csv reader)
         csv->map
         doall)))

; Returns a map, e.g. {:col1 [1 3], :col2 [2 4]}
;; (defn read-csv-col
;;   "Read CSV data into memory by column"
;;   [filename]
;;   (with-open [reader (io/reader filename)]
;;     (->> (csv/read-csv reader)
;;          csv->map-col
;;          doall)))

;; ;; Here, I want to define a function that can convert between the column based
;; ;; dataset and the row based dataset
;; (defn row->col
;;   "This function can parse the seq like ({}{}{}) to {: [] : []}"
;;   [row-based]
;;     (-> (keys (first row-based))
;;     (zipmap (apply map vector (map vals (rest row-based))))))

;; ;; filter the data by security and date
;; ; (defn data-filter
;; ;   ([sec data]
;; ;     (->> data
;; ;     (filter #(= (:tic %) sec))))
;; ;   ([sec year month day data]
;; ;    (->> data
;; ;   (filter #(and (= (:tic %) sec) (= (t/before? (:datadate %) (t/date-time year month day))true))))))

;; (defn- count-days
;;   [row-data]
;;   (count row-data))

;; (defn average
;;  "This function returns the average value of a vector."
;;   [vec]
;;   (/ (reduce + vec) (count vec)))

;; ;; (defn- moving-average
;; ;;  "This function returns the moving average of len(window) days. 
;; ;;  The first len(window) days are recorded as 0."
;; ;;   [window vec]
;; ;;   (concat (repeat (- window 1) 0) (map average (partition window 1 vec))))

;; (defn left-join
;;   "When passed 2 rels, returns the rel corresponding to the natural
;;   left-join. When passed an additional keymap, joins on the corresponding
;;   keys. (Deprecated: should use join instead)"
;;   ([xrel yrel]
;;    (if (and (seq xrel) (seq yrel))
;;      (let [ks (set/intersection (set (keys (first xrel)))
;;                                 (set (keys (first yrel))))
;;            idx (set/index yrel ks)]
;;        (reduce (fn [ret x]
;;                  (if-let [found (idx (select-keys x ks))]
;;                    (reduce #(conj %1 (merge %2 x)) ret found)
;;                    (conj ret x)))
;;                #{} xrel))
;;      xrel))
;;   ([xrel yrel km]
;;    (let [idx (set/index yrel (vals km))]
;;      (reduce (fn [ret x]
;;                (if-let [found (idx (set/rename-keys (select-keys x (keys km)) km))]
;;                  (reduce #(conj %1 (merge %2 x)) ret found)
;;                  (conj ret x)))
;;              #{} xrel))))

;; ; (def file1 "./resources/CRSP-extract_test.csv")
;; ; (def file2 "./resources/Compustat-extract_test.csv")

;; ;;file 1 and 2 directories
;; (comment
;;   (def file1 "./resources/CRSP-extract.csv")
;;   (def file2 "./resources/Compustat-extract.csv")

;;   (def c (first (read-csv-row file1)))
;;   (def d (first (read-csv-row file2))))

;; helper function, natural logarithm
(defn log-10 [n]
  (/ (Math/log n) (Math/log 10)))

;; extract dataset: sorted by security, then by date
;; for each security:
;; add col 'cum-ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum-ret)
;; (defn add-aprc 
;;   "This function adds the adjusted price column to the dataset (CRSP-extract)."
;;   [data]
;;   ; get price on 1st day
;;   (def initial-price 0)
;;   (def cum-ret 0)
;;   (def curr-security "DEFAULT")
;;  ; traverse row by row in dataset
;;   (map (fn [line]
;;         (let [;line-new (select-keys line [:date TICKER-KEY PRICE-KEY :RET])
;;               price (Double/parseDouble (get line PRICE-KEY))
;;               ret (Double/parseDouble (get line :RET))
;;               security (get line TICKER-KEY)]
;;           (if (not= curr-security security)
;;               (do
;;                 (def curr-security security)
;;                 (def initial-price price)
;;                 (def cum-ret 0)
;;               )
;;           )
;;           ;(def log-ret (Math/log (+ 1 ret))) ; natural log
;;           (def log-ret (log-10 (+ 1 ret))) ; log base 10
;;           (def cum-ret (+ cum-ret log-ret))
;;           (def aprc (* initial-price (Math/pow Math/E cum-ret)))
;;           (assoc line :INIT-PRICE initial-price :APRC aprc :LOG-RET log-ret :CUM-RET cum-ret)
;;           ; (swap! data-set-adj conj (assoc line-new "APRC" aprc "LOG-RET" log-ret "CUM-RET" cum-ret))
;;         )
;;       )
;;     data
;;   )
;; )

;; large dataset: sorted by date, then by security
;; for each security:
;; add col 'cum-ret' -> cumulative return = log(1+RET) (sum this every day)
;; add col ' aprc' -> adjusted price = stock price on 1st day of given time period * exp(cum-ret)
;; (defn add-aprc-by-date
;;   "This function adds the adjusted price column to the dataset (data-CRSP-sorted-cleaned)."
;;   [data]
;;   ; get price on 1st day for each security
;;   (def initial-price (atom {}))
;;   ; record cumulative return for each security
;;   (def cum-ret (atom {}))
;;  ; traverse row by row in dataset
;;   (map (fn [line]
;;         (let [date (get line :date)
;;               price (Double/parseDouble (get line PRICE-KEY))
;;               ret (Double/parseDouble (get line :RET))
;;               security (get line TICKER-KEY)]
;;           ;; check whether the initial-price map already has the security
;;           (if-not (contains? (deref initial-price) security)
;;             (do ;; security appears the first time 
;;               (swap! initial-price (fn [security-map] (conj security-map [security {:price price}])))
;;               (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret ret}])))
;;             )
;;             ;; security does not appear the first time
;;             (let [prev-cumret (get-in (deref cum-ret) [security :cumret])
;;                   log-ret (log-10 (+ 1 ret)) ; log base 10 
;;                  ] 
;;               (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret (+ log-ret prev-cumret)}])))
;;             )
;;           )
          
;;           (def security-initial-price (get-in (deref initial-price) [security :price]))
;;           (def curr-cumret (get-in (deref cum-ret) [security :cumret]))
;;           (def aprc (* security-initial-price (Math/pow Math/E curr-cumret)))

;;           (assoc line :INIT-PRICE security-initial-price :APRC aprc :CUM-RET curr-cumret)
;;         )
;;       )
;;     data
;;   )
;; )

;; =========== below is Leo's refactoring using Clojask ==========
;; 2022/10/6

;; cache
(def data-cache {})
(def cache-queue (PriorityQueue.))

;; Global Variables for the dataset
(def data-files {})
(def data-files2 {})
(def headers nil)
(def headers2 nil)

;; Global Hidden Variables
(def tics-today (atom nil))
(def tics-map-today (atom nil))
(def tics-tomorrow (atom nil))

(defn reset-daily-var
  []
  (reset! tics-today (deref tics-tomorrow))
  (reset! tics-tomorrow nil)
  (reset! tics-map-today nil))

(def decoder (Base64/getUrlDecoder))
(defn decode-str
  [s]
  (String. (.decode decoder s)))

(defn decode-filename
  [filename]
  (first (read-string (decode-str filename))))

;; Global functions to set the variables
(defn get-file-date
  [file]
  (let [;; file-name (str file)
        ;; file-name (subs file-name (+ (str/last-index-of file-name "/") 1))
        file-name (.getName file)
        ;; tmp (read-string file-name)
        tmp (decode-filename file-name)
        ]
    ;; (first tmp)
    tmp
    ))

(defn load-dataset
  [dir name & [func]]
  (let [dir (if (str/ends-with? dir "/") dir (str dir "/"))
        file-dir (io/file (str dir "grouped"))
        files (rest (vec (file-seq file-dir)))
        file-date (mapv get-file-date files)
        header (read-string (slurp (str dir "header")))
        _headers (mapv keyword header)
        _data-files (into (sorted-map) (zipmap file-date files))
        tmp (if func (func dir _headers _data-files)) ;; change the file by the function
        header (read-string (slurp (str dir "header")))
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
  ; record cumulative return for each security
(def cum-ret (atom {}))

(defn add-aprc-file
  "This function adds the adjusted price column to the dataset (data-CRSP-sorted-cleaned)."
  [data price-index ret-index security-index]
  ; get price on 1st day for each security
 ; traverse row by row in dataset
  (mapv (fn [line]
        (let [
              ;; date (nth line date-index)
              price (get line price-index)
              ret (get line ret-index)
              security (get line security-index)]
          ;; check whether the initial-price map already has the security
          (if-not (contains? (deref initial-price) security)
            (do ;; security appears the first time 
              (swap! initial-price (fn [security-map] (conj security-map [security {:price price}])))
              (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret ret}])))
            )
            ;; security does not appear the first time
            (let [prev-cumret (get-in (deref cum-ret) [security :cumret])
                  log-ret (log-10 (+ 1 ret)) ; log base 10 
                 ] 
              (swap! cum-ret (fn [security-map] (conj security-map [security {:cumret (+ log-ret prev-cumret)}])))
            )
          )

          (def security-initial-price (get-in (deref initial-price) [security :price]))
          (def curr-cumret (get-in (deref cum-ret) [security :cumret]))
          (def aprc (* security-initial-price (Math/pow Math/E curr-cumret)))

          (vec (concat line [security-initial-price aprc curr-cumret]))
        )
      )
    data
  )
)


(defn add-aprc
  "Data augmentation of adding aprc to each file"
  [dir headers data-files]
  (if (and (.contains headers :INIT-PRICE) (.contains headers :APRC) (.contains headers :CUM-RET))
    (println "The dataset is already furnished by add-aprc. No more modification is needed.")
    (let [price-index (.indexOf headers :PRC)
          ret-index (.indexOf headers :RET)
          security-index (.indexOf headers TICKER-KEY)]
      (println "The below process will take a few hours to run for the first time.")
      (doseq [[date file] data-files]
        (let [rdr (io/reader file)
              data (map read-string (line-seq rdr))
              data (set data)
              new-data (add-aprc-file data price-index ret-index security-index)
              new-data (str/join "\n" (mapv str new-data))]
          (spit file new-data)
          (println date)))
      (spit (str dir "header") (str (vec (concat headers [:INIT-PRICE :APRC :CUM-RET])))))
    ))

(defn delete-dup
  "Delete the duplicated row of each file"
  [dir headers data-files]
  (let []
    (doseq [[date file] data-files]
      (let [rdr (io/reader file)
            data (map read-string (line-seq rdr))
            new-data (set data)
            new-data (str/join "\n" (mapv str new-data))]
        (spit file new-data)
        (println date)))
    ;; (spit (str dir "header") (str (vec (concat headers [:INIT-PRICE :APRC :CUM-RET]))))
    ))