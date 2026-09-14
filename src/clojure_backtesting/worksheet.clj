(ns clojure-backtesting.worksheet
  "Re-runs Gorilla REPL worksheets and rewrites their output cells, so the
   examples under examples/ show results from the current code:

     lein run -m clojure-backtesting.worksheet examples/*.clj

   Each code cell is evaluated in order, in the namespace the worksheet's
   own ns form creates, with printed output captured. Plots are not opened;
   the plot cell shows a note instead."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure-backtesting.plot :as plot])
  (:import [java.io StringWriter PushbackReader StringReader]))

(def ^:private header ";; gorilla-repl.fileformat = 1")

(defn- parse
  "Splits a worksheet into [{:type :markdown :text ...} {:type :code :text ...}],
   dropping old output cells."
  [text]
  (loop [lines (str/split-lines text) segments [] mode nil buf []]
    (if (empty? lines)
      segments
      (let [l (first lines) more (rest lines)]
        (case mode
          nil (cond
                (= l ";; **") (recur more segments :markdown [])
                (= l ";; @@") (recur more segments :code [])
                (= l ";; ->") (recur more segments :console [])
                (= l ";; =>") (recur more segments :value [])
                :else (recur more segments nil []))
          :markdown (if (= l ";; **")
                      (recur more (conj segments {:type :markdown :text (str/join "\n" buf)}) nil [])
                      (recur more segments :markdown (conj buf l)))
          :code (if (= l ";; @@")
                  (recur more (conj segments {:type :code :text (str/join "\n" buf)}) nil [])
                  (recur more segments :code (conj buf l)))
          :console (recur more segments (if (= l ";; <-") nil :console) [])
          :value (recur more segments (if (= l ";; <=") nil :value) []))))))

(defn- json-str [s]
  (str "\"" (-> s
                (str/replace "\\" "\\\\")
                (str/replace "\"" "\\\"")
                (str/replace "\n" "\\n")
                (str/replace "\t" "\\t"))
       "\""))

(defn- html-escape [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- value-cell [v]
  (let [text (pr-str v)
        cls (cond (nil? v) "clj-nil"
                  (string? v) "clj-string"
                  (integer? v) "clj-long"
                  (number? v) "clj-double"
                  (keyword? v) "clj-keyword"
                  (boolean? v) "clj-boolean"
                  :else "clj-unkown")]
    (str ";; =>\n;;; {\"type\":\"html\",\"content\":"
         (json-str (str "<span class='" cls "'>" (html-escape text) "</span>"))
         ",\"value\":" (json-str text) "}\n;; <=")))

(defn- console-cell [out]
  (when (seq out)
    (str ";; ->\n"
         (str/join "\n" (map #(str ";;; " %) (str/split out #"\n" -1)))
         "\n;; <-")))

(defn- read-forms [code]
  (let [rdr (PushbackReader. (StringReader. code))
        eof (Object.)]
    (loop [forms []]
      (let [form (read {:eof eof :read-cond :allow} rdr)]
        (if (identical? form eof)
          forms
          (recur (conj forms form)))))))

(defn- run-cell
  "Evaluates a code cell in `ns`; returns [ns-after value printed-output]."
  [ns code]
  (let [out (StringWriter.)
        result (atom nil)
        ns-after (binding [*ns* ns *out* out]
                   (doseq [form (read-forms code)]
                     (reset! result (eval form)))
                   *ns*)]
    [ns-after @result (str out)]))

(defn- render-code [code value out]
  (str/join "\n" (remove nil? [";; @@" code ";; @@" (console-cell out) (value-cell value)])))

(defn run-worksheet
  "Re-evaluates the worksheet at `path` and rewrites it with fresh output cells."
  [path]
  (println "Running" path)
  (let [segments (parse (slurp path))
        rendered (loop [segments segments ns (find-ns 'user) acc []]
                   (if (empty? segments)
                     acc
                     (let [{:keys [type text]} (first segments)]
                       (if (= type :markdown)
                         (recur (rest segments) ns (conj acc (str ";; **\n" text "\n;; **")))
                         (let [[ns' value out] (try (run-cell ns text)
                                                    (catch Throwable t
                                                      (throw (ex-info (str path ": cell failed\n" text) {} t))))]
                           (recur (rest segments) ns' (conj acc (render-code text value out))))))))]
    (spit path (str header "\n\n" (str/join "\n\n" rendered) "\n"))))

(defn -main [& paths]
  (with-redefs [plot/plot (fn [& _] "The chart opens in the browser when this cell runs in the Gorilla REPL.")]
    (doseq [path paths]
      (run-worksheet path)))
  (shutdown-agents))
