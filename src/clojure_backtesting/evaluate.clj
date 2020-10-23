(ns clojure-backtesting.evaluate)

(defn get-daily-returns [coll]
    ;; get daily returns collection
  (println "daily returns")
  )

(defn get-portfolio-values [coll]
    ;; get portfolio values collection
  (println "portfolio values")
)

(defn get-highest [pv]
    ;; get peak value before largest drop
  (println "peak value before largest drop")
)

(defn get-lowest [pv]
    ;; get lowest value before new high
  (println "lowest value before new high")
)

; TESTED
(defn square [n] 
  (* n n))

; TESTED
(defn mean [a] 
  (/ (reduce + a) (count a)))

;; Standard deviation
; parameter: collection
; output: standard deviation
; TESTED
(defn standard-deviation [a] 
  (Math/sqrt (/ 
                (reduce + (map square (map - a (repeat (mean a))))) 
                (- (count a) 1 ))))

;; Sharpe ratio 
;; = (R_p - R-f) / sd_p * sqrt(252)
;; Annualised:  trading-days = 252
;;
;; parameter: portfolio_record, number of trading days
;; output: sharpe ratio (float)
; TO-TEST
(defn sharpe-ratio [coll trading-days]
  (let [dr (get-daily-returns coll)]
    (* (Math/sqrt trading-days) ((mean dr) (standard-deviation dr))))
)
;; Maximum drawdown
;; = (P - L) / P
;; P = peak value before largest drop, L = lowest value before newest high
;; 
;; parameter: portfolio_record
; TO-TEST
(defn maxdrawdown [coll]
  (let [pv (get-portfolio-values coll) 
        p (get-highest pv)
        l (get-lowest pv)]
    (/ ((- (p l)) p))
  )
)