(ns dashboard.core
  (:require [org.httpkit.server :as http-kit-server]
            [compojure.core     :as comp :refer (defroutes GET POST)]
            [compojure.route    :as route]
            [ring.middleware.defaults]
            [taoensso.sente     :as sente]
            [hiccup.core        :as hiccup]
            [hiccup.page        :as page]
            [hiccup.element     :as ele]))

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

(defn home [req]
  (page/html5
    (hiccup/html
      [:head
       [:title "Home"]
       (page/include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")
       (page/include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js" "js/main.js")]
      [:body
       [:div#content]
       (ele/javascript-tag "$(function() {dashboard.core.main();});")
       ])))

(defroutes my-routes
           (GET  "/"      req (home req))
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


;; init

(defonce http-server_ (atom nil))

(defn stop-http-server! []
  (when-let [stop-f @http-server_]
    (stop-f :timeout 100)))

(defn start-http-server! []
  (stop-http-server!)
  (let [s (http-kit-server/run-server (var my-ring-handler) {:port 0})
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