(ns dashboard.core
  (:require [org.httpkit.server :as http-kit-server]
            [compojure.core     :refer (defroutes GET POST)]
            [compojure.route    :as route]
            [ring.middleware.defaults]
            [taoensso.sente     :as sente]
            [dashboard.controller :as controller]
            [dashboard.db :as db]
            [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]))

(defn- logf [fmt & xs] (println (apply format fmt xs)))


(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )



(defroutes my-routes
           (GET  "/"      req (controller/index req))
           ;;
           (GET  "/chsk"  req (ring-ajax-get-or-ws-handshake req))
           (POST "/chsk"  req (ring-ajax-post                req))
           ;(POST "/login" req (login! req))
           ;;
           (route/resources "/") ; Static files, notably public/main.js (our cljs target)
           (route/not-found "<h1>Page not found</h1>"))

(def my-ring-handler
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                  {:read-token (fn [req] (-> req :params :csrf-token))})]

    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
    ;; that they're included yourself if you're not using `wrap-defaults`.
    ;;
    (ring.middleware.defaults/wrap-defaults my-routes ring-defaults-config)))


;; Routing
(defmulti event-msg-handler :id)

(defn     event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (logf "Event: %s" event)
  (event-msg-handler ev-msg))


(do ; Server-side methods
  (defmethod event-msg-handler :default ; Fallback
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid     session)]
      (logf "Unhandled event: %s" event)
      (when ?reply-fn
        (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

  ;; Add your (defmethod event-msg-handler <event-id> [ev-msg] <body>)s here...
  (defmethod event-msg-handler :dashboard/stories
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid     session)]
      (logf "Get Stories event: %s" event)
      (when ?reply-fn
        (println "data: " ?data)
        (?reply-fn (db/iterationDetail (:iteration-id ?data)))))
    )
  )



(defn start-broadcaster! []
  (go-loop [i 0]
           (<! (async/timeout 10000))
           (println (format "Broadcasting server>user: %s" @connected-uids))
           (doseq [uid (:any @connected-uids)]
             (chsk-send! uid
                         [:some/broadcast
                          {:what-is-this "A broadcast pushed from server"
                           :how-often    "Every 10 seconds"
                           :to-whom uid
                           :i i}]))
           (recur (inc i))))

;; init

(defonce http-server_ (atom nil))

(defn stop-http-server! []
  (when-let [stop-f @http-server_]
    (stop-f :timeout 100)))

(defn start-http-server! []
  (stop-http-server!)
  (let [s (http-kit-server/run-server (var my-ring-handler) {:port 9000})
        uri (format "http://localhost:%s/" (:local-port (meta s)))]
    (reset! http-server_ s)
    (logf "Http-kit server is running at `%s`" uri)))

(defonce router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start! []
  (start-router!)
  (start-http-server!))