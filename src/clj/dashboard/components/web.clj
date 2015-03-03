(ns dashboard.components.web
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-kit-server]
            [dashboard.core :refer [create-ring-handler]]))

(defrecord WebServer [port server db sockets]
  component/Lifecycle
  (start [component]
    (let [server (http-kit-server/run-server (create-ring-handler (:sockets component)) {:port port})]
      (assoc component :server server)))
  (stop [component]
    (when server
      (server)
      component))
  )

(defn new-web-server [port]
  (map->WebServer {:port port}))
