(defproject clojask-script "0.1.0-SNAPSHOT"
  :description "Converts CRSP- and Compustat-style CSV datasets into the per-date layout clojure-backtesting reads."
  :url "https://github.com/clojure-finance/clojure-backtesting"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.clojure-finance/clojask "2.0.3"]]
  ;; Onyx (clojure-finance fork) needs JDK 17+ and this flag for Aeron/Agrona.
  :jvm-opts ["--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"]
  :repl-options {:init-ns clojask-script.core
                 :timeout 18000}
  :main clojask-script.core/-main)
