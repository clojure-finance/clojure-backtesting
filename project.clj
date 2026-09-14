(defproject clojure-backtesting "1.1.0"
  :description "A day-stepping backtesting framework for CRSP-style daily security data."
  :url "https://github.com/clojure-finance/clojure-backtesting"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clojure.java-time "1.4.3"]
                 [metasoarous/oz "1.6.0-alpha31"]]
  :main clojure-backtesting.user/-main
  :aot [clojure-backtesting.user]
  :repl-options {:init-ns clojure-backtesting.user
                 :timeout 180000}
  :plugins [[org.clojars.benfb/lein-gorilla "0.6.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
