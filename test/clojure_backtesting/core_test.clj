(ns clojure-backtesting.core-test
  (:require [clojure.test :refer :all]
            [clojure-backtesting.counter :refer :all]
            
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            ;;[clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]))

(deftest a-test
  (testing "FIXME, I fail."
    (reset! data-set (add_aprc (read-csv-row "/Users/lyc/Desktop/RA clojure/clojure-backtesting/resources/CRSP-extract.csv")))
    ; (println (take 20 (deref data-set)))
    (init_portfolio "1980-12-16" 5000)
    (println get_date)
    (is (= "1980-12-16" (get_date)))
    (is (= "1980-12-17" (next_date)))
))
