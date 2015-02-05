(defproject dashboard "0.1.0-SNAPSHOT"
  :description "dashboard"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2665"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [lein-environ "1.0.0"]                     ; access environment vars
                 [mysql/mysql-connector-java "5.1.6"]
                 [yesql "0.4.0"]                            ; db access
                 [com.taoensso/sente "1.2.0"]               ; web-sockets/ajax comms
                 [http-kit "2.1.19"]                        ; http-server
                 [compojure "1.3.1"]                        ; routing
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.3"]               ; ring middleware
                 [hiccup "1.0.5"]                           ; html rendering
                 [jayq "2.5.2"]
                 [reagent "0.5.0-alpha"]
                 [clj-dbcp      "0.8.1"]
                 [clj-liquibase "0.5.2"]]

  :plugins [[lein-ancient "0.5.5"]                          ; check for outdated dependencies
            [lein-cljsbuild "1.0.4"]                        ; compile clojure scirpt
            [lein-environ "1.0.0"]]                         ; user of environment vars

  :main ^:skip-aot dashboard.main

  :target-path "target/%s"

  :cljsbuild {
                :builds [{
                          :source-paths ["src/cljs"]
                          :compiler {
                                     :output-to "resources/public/js/app.js"
                                     :output-dir "resources/public/js/out"
                                     :preamble ["reagent/react.js"]
                                     :optimizations :whitespace
                                     :pretty-print true
                                     }
                          }]
                }

  :profiles {:uberjar {:aot :all}})
