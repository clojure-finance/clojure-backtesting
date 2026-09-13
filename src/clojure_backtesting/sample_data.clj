(ns clojure-backtesting.sample-data
  "A small synthetic dataset in the on-disk layout that clojask-script
   produces, so the examples and the end-to-end tests run without WRDS
   access. Three securities over the weekdays of January to March 1990:

   - 10001 AAA pays a $0.50 dividend on 1990-02-15
   - 10002 BBB has a 2-for-1 split on 1990-03-01 (CFACPR 2.0 -> 1.0)
   - 10003 CCC has no rows from 1990-02-05 to 1990-02-09

   plus a supplementary Compustat-style dataset with two filing dates for
   AAA and BBB only. Prices follow a seeded random walk, so the files are
   reproducible: `lein run -m clojure-backtesting.sample-data` rewrites
   resources/sample-data, furnished with the adjusted-price columns."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure-backtesting.data :as data]
            [java-time :as jt])
  (:import [java.util Base64 Random]))

(def main-columns ["PERMNO" "date" "TICKER" "PRC" "OPENPRC" "BIDLO" "ASKHI" "RET" "CFACPR"])
(def compustat-columns ["PERMNO" "datadate" "rdq" "atq" "ceqq" "niq"])

(def securities
  [{:permno "10001" :ticker "AAA" :start 50.0 :dividends {"1990-02-15" 0.50}}
   {:permno "10002" :ticker "BBB" :start 120.0 :splits {"1990-03-01" 2.0}}
   {:permno "10003" :ticker "CCC" :start 20.0 :gap #{"1990-02-05" "1990-02-06" "1990-02-07" "1990-02-08" "1990-02-09"}}])

(def compustat-rows
  "[PERMNO datadate rdq atq ceqq niq] as strings, the way the preprocessor
   keeps untyped columns."
  {"1989-09-30" [["10001" "1989-09-30" "1989-10-25" "1500.0" "900.0" "40.0"]
                 ["10002" "1989-09-30" "1989-10-30" "5200.0" "2100.0" "150.0"]]
   "1989-12-31" [["10001" "1989-12-31" "1990-01-24" "1550.0" "925.0" "45.0"]
                 ["10002" "1989-12-31" "1990-01-29" "5300.0" "2150.0" "160.0"]]})

(def trading-days
  "Weekdays from 1990-01-02 to 1990-03-30, as yyyy-MM-dd strings."
  (->> (jt/local-date 1990 1 2)
       (iterate #(jt/plus % (jt/days 1)))
       (take-while #(not (jt/after? % (jt/local-date 1990 3 30))))
       (remove #(> (jt/as % :day-of-week) 5))
       (mapv #(jt/format "yyyy-MM-dd" %))))

(def ^:private ^java.util.Base64$Encoder encoder (Base64/getUrlEncoder))

(defn group-file-name
  "clojask names each date's file with the base64url of the EDN group key."
  [date]
  (.encodeToString encoder (.getBytes (str [date]))))

(defn- round2 [x] (/ (Math/round (* (double x) 100.0)) 100.0))

(defn security-rows
  "The main-dataset rows for one security, in trading-day order. RET is the
   total return since the previous row, so it spans a gap and includes
   dividends and the split adjustment, exactly as CRSP defines it."
  [{:keys [permno ticker start dividends splits gap]}]
  (let [rng (Random. (Long/parseLong permno))
        total-split (reduce * 1.0 (vals (or splits {})))]
    (loop [days trading-days
           raw-close start ; close before any split
           cfacpr total-split ; cumulative factor: products of splits still to come
           last-row-close start ; close on the last emitted row
           rows []]
      (if (empty? days)
        rows
        (let [date (first days)
              raw-close (* raw-close (Math/exp (* 0.015 (.nextGaussian rng))))
              cfacpr (if-let [f (get splits date)] (/ cfacpr f) cfacpr)
              close (round2 (/ raw-close (/ total-split cfacpr)))]
          (if (contains? gap date)
            (recur (rest days) raw-close cfacpr last-row-close rows)
            (let [prev-cfacpr (if-let [f (get splits date)] (* cfacpr f) cfacpr)
                  factor (/ prev-cfacpr cfacpr)
                  dividend (get dividends date 0.0)
                  ret (- (/ (+ (* close factor) dividend) last-row-close) 1.0)
                  open (round2 (* (/ last-row-close factor) (+ 1.0 (* 0.005 (.nextGaussian rng)))))
                  low (round2 (* (min open close) (- 1.0 (* 0.004 (Math/abs (.nextGaussian rng))))))
                  high (round2 (* (max open close) (+ 1.0 (* 0.004 (Math/abs (.nextGaussian rng))))))]
              (recur (rest days) raw-close cfacpr close
                     (conj rows [permno date ticker close open low high ret cfacpr])))))))))

(defn main-rows-by-date []
  (->> securities
       (mapcat security-rows)
       (group-by second)
       (into (sorted-map))))

(defn write-grouped-dataset
  "Writes <dir>/header and one file per date under <dir>/grouped."
  [dir columns rows-by-date]
  (let [grouped (io/file dir "grouped")]
    (.mkdirs grouped)
    (spit (io/file dir "header") (str columns))
    (doseq [[date rows] rows-by-date]
      (spit (io/file grouped (group-file-name date)) (str/join "\n" (map str rows))))))

(defn write-sample-dataset
  "Writes the raw main dataset to <dir>/main and the supplementary dataset
   to <dir>/compustat. Returns dir. The main dataset still needs add-aprc,
   which (load-dataset main-dir \"main\" add-aprc) applies in place."
  [dir]
  (write-grouped-dataset (io/file dir "main") main-columns (main-rows-by-date))
  (write-grouped-dataset (io/file dir "compustat") compustat-columns (into (sorted-map) compustat-rows))
  dir)

(defn -main
  "Regenerates resources/sample-data (or the given directory), furnished
   with INIT-PRICE, APRC and CUM-RET so that it can be loaded read-only."
  [& [dir]]
  (let [dir (io/file (or dir "resources/sample-data"))]
    (doseq [f (reverse (file-seq dir))] (io/delete-file f true))
    (write-sample-dataset dir)
    (println (data/load-dataset (str (io/file dir "main")) "main" data/add-aprc))
    (println (data/load-dataset (str (io/file dir "compustat")) "compustat"))
    (println "Sample dataset written to" (str dir))))
