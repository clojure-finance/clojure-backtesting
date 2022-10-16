(ns clojure-backtesting.automation
  (:require [java-time :as jt]
            [clojure-backtesting.counter :refer :all]
            ;; [clojure-backtesting.order :refer :all]
            ))

(defmacro action
  "Similar to clojure.core/defn, but saves the function's definition in the var's
   :source meta-data."
  [act]
    ;; (with-meta act {:source &form}))
  `(do
    ;;  (defn a# [] (~act))
    ;;    (alter-meta! (var a#) assoc :source (quote ~&form))
    ;;    [(var a#) ~act]
    ;;  [(quote ~&form) ~act]
     (with-meta ~act {:source (first (drop 1 (quote ~&form)))})))

(defmacro condition
  "Similar to clojure.core/defn, but saves the function's definition in the var's
   :source meta-data."
  [act]
    ;; (with-meta act {:source &form}))
  `(do
    ;;  (defn a# [] (~act))
    ;;    (alter-meta! (var a#) assoc :source (quote ~&form))
    ;;    [(var a#) ~act]
    ;;  [(quote ~&form) ~act]
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
                                                                                                        ;;  (t/format "yyyy-MM-dd" (t/plus (get-date) (t/days expiration)))
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

;; limit order wrappers
;; (defn stop-buy
;;   "This function executes a stop buy order."
;;   [tic prc qty mode & [expiration]]
;;   (if (= mode "non-lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is greater than prc
;;      #(and  (> (get-in (deref portfolio) [tic :aprc]) prc))
;;      #(order tic qty)
;;      :max-dispatch 1))
;;   (if (= mode "lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is greater than prc
;;      #(> (get-in (deref portfolio) [tic :aprc]) prc)
;;      #(order tic qty)
;;      :max-dispatch 1))
;;   (if (and (not= mode "lazy") (not= mode "non-lazy"))
;;     (println "Stop order failed, <mode> could only be \"lazy\" or \"non-lazy\".")))

;; (defn limit-buy
;;   "This function executes a limit buy order."
;;   [tic prc qty mode]
;;   (if (= mode "non-lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is smaller than prc
;;      #(< (get-in (deref portfolio) [tic :aprc]) prc)
;;      #(order tic qty)
;;      :max-dispatch 1))
;;   (if (= mode "lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is smaller than prc
;;      #(< (get-in (deref portfolio) [tic :aprc]) prc)
;;      #(order tic qty)
;;      :max-dispatch 1))
;;   (if (and (not= mode "lazy") (not= mode "non-lazy"))
;;     (println "Stop order failed, <mode> could only be \"lazy\" or \"non-lazy\".")))

;; (defn stop-sell
;;   "This function executes a stop sell order."
;;   [tic prc qty mode]
;;   (if (= mode "non-lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is smaller than prc
;;      #(< (or (get-in (deref portfolio) [tic :aprc]) prc) prc)
;;      #(order tic (* qty -1))
;;      :max-dispatch 1))
;;   (if (= mode "lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is greater than prc
;;      #(< (or (get-in (deref portfolio) [tic :aprc]) prc) prc)
;;      #(order tic (* qty -1))
;;      :max-dispatch 1))
;;   (if (and (not= mode "lazy") (not= mode "non-lazy"))
;;     (println "Stop order failed, <mode> could only be \"lazy\" or \"non-lazy\".")))

;; (defn limit-sell
;;   "This function executes a limit sell order."
;;   [tic prc qty mode]
;;   (if (= mode "non-lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is smaller than prc
;;      #(> (or (get-in (deref portfolio) [tic :aprc]) prc) prc)
;;      #(order tic (* qty -1))
;;      :max-dispatch 1))
;;   (if (= mode "lazy")
;;     (set-automation
;;       ; check if ticker adjusted price is greater than prc
;;      #(> (or (get-in (deref portfolio) [tic :aprc]) prc) prc)
;;      #(order tic (* qty -1))
;;      :max-dispatch 1))
;;   (if (and (not= mode "lazy") (not= mode "non-lazy"))
;;     (println "Stop order failed, <mode> could only be \"lazy\" or \"non-lazy\".")))