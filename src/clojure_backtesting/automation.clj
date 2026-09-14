(ns clojure-backtesting.automation
  (:require [clojure-backtesting.counter :refer :all]))

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
  "Registers `condition`, a no-argument function checked at the end of every
   trading day, and `order-function`, run when it returns true. Returns an
   integer id for cancel-automation. `:max-dispatch` caps how many times it
   can run; `:expiration` is the number of trading days after today it
   stays active. Both default to unlimited."
  [condition order-function & {:keys [max-dispatch expiration] :or {max-dispatch nil expiration nil}}]
  (swap! auto-counter inc)
  (swap! automated-conditions assoc (deref auto-counter)
         [condition order-function (atom max-dispatch)
          (when expiration (date-after-n-trading-days (get-date) expiration))])
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
