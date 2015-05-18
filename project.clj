(defproject dashboard "0.1.0-SNAPSHOT"
  :description "dashboard"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [org.clojure/clojurescript "0.0-3269" :classifier "aot" :exclusions [org.clojure/tools.reader org.clojure/data.json]]
                 [org.clojure/tools.reader "0.9.2" :classifier "aot"]
                 [org.clojure/data.json "0.2.6" :classifier "aot"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [lein-environ "1.0.0"]                     ; access environment vars
                 [mysql/mysql-connector-java "5.1.35"]
                 [yesql "0.4.1"]                            ; db access
                 [com.taoensso/sente "1.4.1"]               ; web-sockets/ajax comms
                 [http-kit "2.1.19"]                        ; http-server
                 [compojure "1.3.4"]                        ; routing
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.5"]               ; ring middleware
                 [hiccup "1.0.5"]                           ; html rendering
                 [jayq "2.5.4"]
                 [reagent "0.5.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "0.13.1-0"]
                 [com.stuartsierra/component "0.2.3"]
                 [hikari-cp "1.2.3"]
                 [org.slf4j/slf4j-simple "1.7.12"]
                 [org.slf4j/jcl-over-slf4j "1.7.12"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [buddy/buddy-auth "0.5.2"]
                 [org.clojars.frozenlock/reagent-modals "0.2.3"]
                 [hickory "0.5.4"]
                 [org.apache.maven.doxia/doxia-converter "1.2" :exclusions [commons-logging xerces/xercesImpl]]
                 [clojure-tools "1.1.3"]
                 [secretary "1.2.3"]
                 [clj-dbcp      "0.8.1"]
                 [clj-liquibase "0.5.2"]
                 [figwheel "0.3.1" :exclusions [junit org.clojure/clojurescript]]
                 [org.clojure/data.xml "0.0.8"]]

  :plugins [[lein-ancient "0.5.5"]                          ; check for outdated dependencies
            [lein-cljsbuild "1.0.4"]                        ; compile clojure scirpt
            [lein-environ "1.0.0"]
            [lein-figwheel "0.2.5" :exclusions [org.clojure/clojure org.clojure/tools.reader org.codehaus.plexus/plexus-utils]]]

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

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :source-paths ["dev"]}})
