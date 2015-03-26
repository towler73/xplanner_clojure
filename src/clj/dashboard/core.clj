(ns dashboard.core
  (:require
    [compojure.core :refer (routes GET POST)]
    [compojure.route :as route]
    [compojure.response :refer [render]]
    [ring.middleware.defaults]
    [ring.util.response :refer [response redirect redirect-after-post]]
    [clojure.core.match :refer [match]]
    [dashboard.templates :as templates]
    [dashboard.db :as db]
    [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
    [buddy.auth.backends.session :refer [session-backend]]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [buddy.auth :refer [authenticated? throw-unauthorized]]))

(defn- logf [fmt & xs] (println (apply format fmt xs)))


(defn authenticate-user
  [request authdata]
  (let [username (:username authdata)
        password (:password authdata)]
    username))

(defn unauthorized-handler
  [request metadata]
  (redirect "/login"))

(def backend (session-backend {:unauthorized-handler unauthorized-handler}))

(defn home [req]
  (if-not (authenticated? req)
    (throw-unauthorized)
    (templates/main)
    )
  )


(defn login! [req db]
  (let [username (get-in req [:form-params "username"])
        password (get-in req [:form-params "password"])
        session (:session req)]
    (if (seq (db/user db username))
      (let [session (assoc session :identity (keyword username))]
        (-> (redirect "/") (assoc :session session)))
      (templates/login)
      )
    )
  )



(defn make-routes [sockets db]
  (routes
    (GET "/" req home)
    (GET "/login" _ (templates/login))
    (POST "/login" req (login! req db))
    ;;
    (GET "/chsk" req ((:ring-ajax-get-or-ws-handshake sockets) req))
    (POST "/chsk" req ((:ring-ajax-post sockets) req))
    ;;
    (route/resources "/")                                   ; Static files, notably public/main.js (our cljs target)
    (route/not-found "<h1>Page not found</h1>")))

(defn create-ring-handler [sockets db]
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                  {:read-token (fn [req] (-> req :params :csrf-token))})]

    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
    ;; that they're included yourself if you're not using `wrap-defaults`.
    ;;
    (-> (make-routes sockets db) (wrap-authorization backend) (wrap-authentication backend) (ring.middleware.defaults/wrap-defaults ring-defaults-config))))


;; Routing
(defn event-msg-handler [db]
  (fn [{:as ev-msg :keys [id ?data ?reply-fn event ring-req]}]
    (logf "Event: %s" event)
    (if-not (authenticated? ring-req)
      (when ?reply-fn
        (?reply-fn {:unauthenticated-user event}))
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
                                               (?reply-fn {:iterations (db/project-iterations db (:project-id ?data)) :current-iteration-id (:id (db/initial-iteration db (:project-id ?data)))}))
             [:dashboard/save-team-estimate] (do (db/save-team-estimate db (:iteration_id ?data) (:id ?data) (:team_estimate ?data))
                                                 (when ?reply-fn
                                                   (?reply-fn {:saved? true})))
             [:dashboard/logged-in-user] (do
                                           (println (first (db/user db (name (:identity ring-req)))))
                                           (?reply-fn (first (db/user db (name (:identity ring-req))))))
             :else (when ?reply-fn
                     (?reply-fn {:umatched-event-as-echoed-from-from-server event})))))
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

