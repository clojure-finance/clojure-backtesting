(ns clojure-backtesting.specs
    (:require [clojure.test :refer :all]
              [clojure-backtesting.data :refer :all]
              [clojure-backtesting.order :refer :all]
              [clojure-backtesting.evaluate :refer :all]
              [clojure-backtesting.plot :refer :all]
              ;;[clojure-backtesting.parameters :refer :all]
              [clojure.string :as str]
              [clojure.pprint :as pprint]
              [java-time :as t]
              [clojure.spec.alpha :as s]
              [clojupyter.kernel.version :as ver]
              )(:gen-class))

              
; example of clojure.spec
(s/def ::even? (s/and integer? even?))
(s/def ::odd? (s/and integer? odd?))
(s/def ::a integer?)
(s/def ::b integer?)
(s/def ::c integer?)

(s/def ::date? not-empty)
(s/def ::num? (s/or :float float? 
    :int   int? 
    :ratio ratio?)
)

(defn ^:private positive?
    [n]
    (< 0 n)
)

(def s (s/cat :forty-two #{42}
    :odds (s/+ ::odd?)
    :m (s/keys :req-un [::a ::b ::c])
    :oes (s/* (s/cat :o ::odd? :e ::even?))
    :ex (s/alt :odd ::odd? :even ::even?))
)

; (def portfolio_value (atom [{:date date :tot_value init-capital :daily_ret 0}])
(def portfolio-test
    (s/cat :cash {:tot_val not-empty}
        :tic (s/* (s/cat :price not-empty :aprc not-empty :quantity not-empty :tot_val not-empty)))
)