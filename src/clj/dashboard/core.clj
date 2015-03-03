(ns dashboard.core
  (:require
            [compojure.core     :refer (routes GET POST)]
            [compojure.route    :as route]
            [ring.middleware.defaults]
            [clojure.core.match :refer [match]]
            [dashboard.templates :as templates]
            [dashboard.db :as db]
            [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]))

(defn- logf [fmt & xs] (println (apply format fmt xs)))



(defn make-routes [sockets]
  (routes
          (GET "/" req (templates/main))
          ;;
          (GET "/chsk" req ((:ring-ajax-get-or-ws-handshake sockets) req))
          (POST "/chsk" req ((:ring-ajax-post sockets) req))
          ;(POST "/login" req (login! req))
          ;;
          (route/resources "/")                             ; Static files, notably public/main.js (our cljs target)
          (route/not-found "<h1>Page not found</h1>")))

(defn create-ring-handler [db]
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                  {:read-token (fn [req] (-> req :params :csrf-token))})]

    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
    ;; that they're included yourself if you're not using `wrap-defaults`.
    ;;
    (ring.middleware.defaults/wrap-defaults (make-routes db) ring-defaults-config)))


;; Routing
(defn event-msg-handler [db]
  (fn [{:as ev-msg :keys [id ?data ?reply-fn event]}]
    (logf "Event: %s" event)
    (match [id]
           [:dashboard/stories] (when ?reply-fn
                                  (println "data: " ?data)
                                  (?reply-fn (db/iteration-stories-map db (:iteration-id ?data))))
           [:dashboard/iteration-teams] (when ?reply-fn
                                          (println "data: " ?data)
                                          (?reply-fn (db/iteration-teams-summary db (:iteration-id ?data))))
           [:dashboard/project-iterations] (when ?reply-fn
                                             (println "data: " ?data)
                                             ;todo fix current iteration id
                                             (?reply-fn {:iterations (db/project-iterations db (:project-id ?data)) :current-iteration-id 668460}))
           [:dashboard/save-team-estimate] (do (db/save-team-estimate db (:iteration_id ?data) (:id ?data) (:team_estimate ?data))
                                               (when ?reply-fn
                                                 (?reply-fn {:saved? true})))
           :else (when ?reply-fn
                   (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))
  )


;(defn start-broadcaster! []
;  (go-loop [i 0]
;           (<! (async/timeout 10000))
;           (println (format "Broadcasting server>user: %s" @connected-uids))
;           (doseq [uid (:any @connected-uids)]
;             (chsk-send! uid
;                         [:some/broadcast
;                          {:what-is-this "A broadcast pushed from server"
;                           :how-often    "Every 10 seconds"
;                           :to-whom uid
;                           :i i}]))
;           (recur (inc i))))

