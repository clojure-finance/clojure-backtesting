(ns build
  "Build tasks, run with clojure -T:build <task>."
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'com.github.clojure-finance/clojure-backtesting)
(def version "1.1.3")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar
  "Builds the library jar (sources only) and its pom under target/classes."
  [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :scm {:url "https://github.com/clojure-finance/clojure-backtesting"
                      :connection "scm:git:git://github.com/clojure-finance/clojure-backtesting.git"
                      :developerConnection "scm:git:ssh://git@github.com/clojure-finance/clojure-backtesting.git"
                      :tag version}
                :basis (b/create-basis {:project "deps.edn"})
                :src-dirs ["src"]
                :pom-data [[:licenses
                            [:license
                             [:name "MIT"]
                             [:url "https://opensource.org/licenses/MIT"]]]
                           [:description
                            "A day-stepping backtesting framework for daily security data from CRSP-style files or Yahoo Finance, with point-in-time fundamentals."]
                           [:url
                            "https://github.com/clojure-finance/clojure-backtesting"]]})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uber
  "Builds an AOT-compiled standalone jar running clojure-backtesting.user/-main."
  [_]
  (let [basis (b/create-basis {:project "deps.edn"})]
    (b/copy-dir {:src-dirs ["src"]
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :class-dir class-dir
                    :ns-compile '[clojure-backtesting.user]
                    :java-opts ["-Dclojure.compiler.direct-linking=true"]})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main 'clojure-backtesting.user})))

(defn deploy
  "Deploys the jar to Clojars; credentials from CLOJARS_USERNAME / CLOJARS_PASSWORD."
  [_]
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (b/pom-path {:lib lib
                                     :class-dir class-dir})}))
