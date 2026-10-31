(ns clojure-backtesting.dataset-switch-test
  "Loading a second main dataset in the same session."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure-backtesting.sample-data :as sample]
            [clojure-backtesting.yahoo.build :as build]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.data-management :refer :all]
            [clojure-backtesting.parameters :refer :all]))

(deftest a-second-dataset-starts-with-an-empty-cache
  (let [tmp (fn [] (io/file (System/getProperty "java.io.tmpdir") (str "clojure-backtesting-switch-" (System/nanoTime))))
        a (tmp)
        b (tmp)]
    (try
      (sample/write-sample-dataset a)
      (build/build-dataset! "resources/sample-yahoo/raw" b)
      (load-dataset (str (io/file a "main")) "main" add-aprc)
      (is (> (count data-files) CACHE-SIZE) "enough days to fill the cache")
      (doseq [date (keys data-files)]
        (get-permno-info date "10001"))
      ;; a dataset over other dates: evicting must not reach for the first one's
      (load-dataset (str (io/file b "main")) "main")
      (doseq [date (take 100 (keys data-files))]
        (is (some? (get-permno-info date "ALFA")) date))
      (finally
        (doseq [d [a b] x (reverse (file-seq d))] (io/delete-file x true))))))
