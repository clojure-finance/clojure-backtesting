(defproject clojure-backtesting "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "1.0.0"]
                 [com.hypirion/clj-xchart "0.2.0"]
                 [clojure.java-time "0.3.2"] ;https://github.com/dm3/clojure.java-time
                 [clj-time "0.9.0"]
                 [clojupyter/clojupyter "0.3.2"]
                 ]
  :aliases			{"clojupyter"			["run" "-m" "clojupyter.cmdline"]}
  :main clojure-backtesting.user
  :aot [clojure-backtesting.user]
  :plugins [[lein-jupyter "0.1.16"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
