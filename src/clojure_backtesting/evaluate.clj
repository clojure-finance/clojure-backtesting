(ns clojure-backtesting.evaluate)

(defn daily-returns [coll]
    ;; get daily returns
  )

(defn risk-measure [coll]
    ;; return risk
  )

(defn mean [coll]
    ;; return mean
  )

;; Sharpe ratio 
;; = (R_p - R-f) / sd_p * sqrt(252)
;; Annualised:  trading-days = 252
(defn sharpe-ratio [coll trading-days]
  (let [dr (daily-returns coll)]
    (* (Math/sqrt trading-days) ((mean dr) (standard-deviation dr)))

; TESTED
(defn square [n] 
    (* n n))

; TESTED
(defn mean [a] 
    (/ (reduce + a) (count a)))

; parameter: collection
; output: standard deviation
; TESTED
(defn standard-deviation [a] 
    (Math/sqrt (/ 
                  (reduce + (map square (map - a (repeat (mean a))))) 
                  (- (count a) 1 ))))

(defn debug [str]
  (println str))