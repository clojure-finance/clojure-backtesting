(ns clojask-script.core
  (:require [clojask.dataframe :as ck]
            [clojask-io.input :as input]
            [clojask.api.gb-aggregate :as gb-agg]
            [clojask.onyx-comps :refer [start-onyx-groupby]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [clojask.classes.RowInfo RowIntf]))

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
    (io/make-parents (str output-dir "grouped/" "header"))
    ;; column names must be read before group-by: afterwards clojask reports
    ;; only the grouping column
    (let [headers (mapv keyword (ck/get-col-names df))
          _ (ck/group-by df "date")
          tmp (spit (str output-dir "header") (str headers))
          groupby-keys (.getGroupbyKeys ^RowIntf (:row-info df))
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
    (io/make-parents (str output-dir "grouped/" "header"))
    ;; column names must be read before group-by: afterwards clojask reports
    ;; only the grouping column
    (let [headers (mapv keyword (ck/get-col-names df))
          _ (ck/group-by df "datadate")
          tmp (spit (str output-dir "header") (str headers))
          groupby-keys (.getGroupbyKeys ^RowIntf (:row-info df))
          groupby-index (vec (take (count headers) (iterate inc 0)))
          res (start-onyx-groupby 8 300 df (str output-dir "grouped/") groupby-keys groupby-index false)]
      (if (= res "success")
        (do
          (println "Dataset generated successfully."))))))

(def ^:private parameter-vars
  {:input-file #'input-file
   :output-dir #'output-dir
   :type-of-dataset #'type-of-dataset
   :security-identifier #'security-identifier
   :date-identifier #'date-identifier
   :data-format-string #'data-format-string
   :closing-price #'closing-price
   :opening-price #'opening-price
   :return-identifier #'return-identifier
   :daily-min-price #'daily-min-price
   :daily-max-price #'daily-max-price})

(defn- apply-config!
  "Overrides the parameters above with the entries of an EDN map, e.g.
   {:input-file \"/data/crsp.csv\" :output-dir \"/data/CRSP\"}."
  [path]
  (let [config (edn/read-string (slurp path))]
    (doseq [[k v] config]
      (if-let [v-var (get parameter-vars k)]
        (alter-var-root v-var (constantly v))
        (throw (ex-info (str "Unknown parameter " k) {:known (keys parameter-vars)}))))))

(defn -main
  "Main function. Takes an optional path to an EDN file whose entries
   override the parameters defined at the top of this file."
  [& [config-path]]
  (when config-path
    (apply-config! config-path))
  (def output-dir (if (str/ends-with? output-dir "/") output-dir (str output-dir "/")))
  (println (str "Input file: " input-file))
  (println (str "Output dir: " output-dir))
  (cond
    (= type-of-dataset "CRSP") (CRSP-action)
    (= type-of-dataset "Compustat") (Compustat-action)
    :else (throw (Exception. "Undefined type of dataset."))))
