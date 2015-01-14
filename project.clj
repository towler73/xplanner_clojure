(defproject dashboard "0.1.0-SNAPSHOT"
  :description "dashboard"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [yesql "0.4.0"]
                 [com.taoensso/sente "1.2.0"]
                 [http-kit "2.1.19"]
                 [compojure "1.3.1"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.3"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot dashboard.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
