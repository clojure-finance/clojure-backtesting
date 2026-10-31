(defproject com.github.clojure-finance/clojure-backtesting "1.1.2"
  :description "A day-stepping backtesting framework for daily security data from CRSP-style files or Yahoo Finance, with point-in-time fundamentals."
  :url "https://github.com/clojure-finance/clojure-backtesting"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clojure.java-time "1.4.3"]
                 [metasoarous/oz "1.6.0-alpha31"]
                 [com.github.clojure-finance/clj-yfinance "0.1.10"]]
  :test-selectors {:default (complement :live)
                   :live :live}
  ;; lein deploy clojars, credentials from CLOJARS_USERNAME / CLOJARS_PASSWORD
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org/"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]
  :main clojure-backtesting.user/-main
  :repl-options {:init-ns clojure-backtesting.user
                 :timeout 180000}
  :plugins [[org.clojars.benfb/lein-gorilla "0.6.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
