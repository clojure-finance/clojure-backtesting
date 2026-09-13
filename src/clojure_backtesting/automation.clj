(ns clojure-backtesting.automation
  (:require [java-time :as jt]
            [clojure-backtesting.counter :refer :all]))

(defmacro action
  "Similar to clojure.core/defn, but saves the function's definition in the var's
   :source meta-data."
  [act]
  `(do
     (with-meta ~act {:source (first (drop 1 (quote ~&form)))})))

(defmacro condition
  "Similar to clojure.core/defn, but saves the function's definition in the var's
   :source meta-data."
  [act]
  `(do
     (with-meta ~act {:source (first (drop 1 (quote ~&form)))})))

;; ============ Automated orders ============

(def automated-conditions (atom {}))
(def auto-counter (atom 0))
(def dispatch-history (atom []))

(defn set-automation
  "This function set an automated order request that will be triggered by a certain condition.
   Will return a unique int as identifier to the condition"
  [condition order-function & {:keys [max-dispatch expiration] :or {max-dispatch nil expiration nil}}]
  (swap! auto-counter inc)
  (swap! automated-conditions assoc (deref auto-counter) [condition order-function (atom max-dispatch) (if expiration
                                                                                                         (jt/plus (jt/local-date "yyyy-MM-dd" (get-date)) (jt/days expiration))
                                                                                                         nil)])
  (deref auto-counter))

(defn cancel-automation
  "This function removes an automation from the list."
  [num]
  (swap! automated-conditions dissoc num)
  true)

(defn check-automation
  "This function is ought to be called before next-date and end-date."
  []
  (doseq [[counter [condition order-function max-dispatch expiration-date]] (deref automated-conditions)]
    (if (and (not= expiration-date nil) (> (compare (get-date) expiration-date) 0))
      (cancel-automation counter)
      (if (and (or (= (deref max-dispatch) nil) (> (deref max-dispatch) 0)) (condition))
        (do (order-function)
            (println (format "Automation %d dispatched." counter))
            (swap! dispatch-history conj {:date (get-date) :automation counter})
            (if (deref max-dispatch) ;; if it is not nil
              (swap! max-dispatch dec)))
        (if (and (not= (deref max-dispatch) nil) (<= (deref max-dispatch) 0))
          (cancel-automation counter))))))

(defn reset-automation
  []
  (reset! automated-conditions {})
  (reset! auto-counter 0)
  (reset! dispatch-history []))
