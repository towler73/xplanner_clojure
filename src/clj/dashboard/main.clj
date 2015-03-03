(ns dashboard.main
  (:gen-class)
  (:require [dashboard.components.web :refer [new-web-server]]
            [dashboard.components.sente :refer [new-channel-sockets]]
            [dashboard.components.db :refer [new-database]]
            [com.stuartsierra.component :as component]))

(defn dashboard-system []
  (component/system-map
    :db (new-database "localhost")
    :sockets (component/using (new-channel-sockets) [:db])
    :web-server (component/using (new-web-server 9000) [:sockets])))

(defn -main [& args]
  (component/start (dashboard-system)))
