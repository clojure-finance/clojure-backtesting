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
                ;;  [table "0.5.0"]
                 [clojupyter/clojupyter "0.3.2"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [metasoarous/oz "1.6.0-alpha31"]
                 [com.clojure-goes-fast/clj-memory-meter "0.1.3"]
                 [net.mikera/core.matrix "0.62.0"]
                 [com.github.clojure-finance/clojask-io "1.0.2"]
                 ]
  :aliases			{"clojupyter"			["run" "-m" "clojupyter.cmdline"]}
  :main clojure-backtesting.user/-main
  :aot [clojure-backtesting.user]
  :repl-options {:init-ns clojure-backtesting.user
                 :timeout 180000}
  :plugins [[lein-jupyter "0.1.16"]
            [org.clojars.benfb/lein-gorilla "0.6.0"]
            ]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Djdk.attach.allowAttachSelf" "-Dclojure.compiler.direct-linking=true"]}})
