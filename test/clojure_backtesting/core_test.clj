(ns clojure-backtesting.core-test
  (:require [clojure.test :refer :all]
            [clojure-backtesting.data :refer :all]
            [clojure-backtesting.order :refer :all]
            [clojure-backtesting.evaluate :refer :all]
            [clojure-backtesting.plot :refer :all]
            ;;[clojure-backtesting.parameters :refer :all]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [java-time :as t]
            ))

; (deftest a-test
;   (testing "FIXME, I fail."
;     (is (= 0 1))))

(deftest math-calculation
  (testing 
    (is (= 3 (mean [1 2 3 4 5])))
    (is (= 7/2 (mean [1 2 3 4 5 6])))
    (is (= 1.0 (standard-deviation [1 2 3])))
    ;(is (= 1.581 (standard-deviation [1 2 3 4 5])))
  )
)
