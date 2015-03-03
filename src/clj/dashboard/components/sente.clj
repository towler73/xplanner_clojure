(ns dashboard.components.sente
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sente]
            [dashboard.core :refer [event-msg-handler]]))


(defrecord ChannelSockets [ring-ajax-post ring-ajax-get-or-ws-handshake ch-chsk chsk-send! connected-uids router db]
  component/Lifecycle
  (start [component]
    (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
          (sente/make-channel-socket! {})]
      (assoc component
        :ring-ajax-post ajax-post-fn
        :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn
        :ch-chsk ch-recv
        :chsk-send! send-fn
        :connected-uids connected-uids
        :router (atom (sente/start-chsk-router! ch-recv (event-msg-handler db))))))
  (stop [component]
    (if-let [stop-f @router]
      (assoc component :router (stop-f))
      component)))

(defn new-channel-sockets
  []
  (map->ChannelSockets {}))
