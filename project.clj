(defproject goodreads "0.1.0-SNAPSHOT"
  :description "Books recommendation tool"
  :license {:name "Proprietary"}
  :jvm-opts ^:replace ["-server"
                       "-XX:+UseConcMarkSweepGC"
                       "-Xmx256m"
                       "-XX:-OmitStackTraceInFastThrow"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [aleph "0.4.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.zip "0.1.3"]
                 [clj-http "3.10.0"]]
  :main ^:skip-aot goodreads.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :repl-options {:init-ns goodreads.core})
