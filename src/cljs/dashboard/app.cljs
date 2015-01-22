(ns dashboard.app
  (:require-macros
  [cljs.core.async.macros :as asyncm :refer (go go-loop)]
  [jayq.macros :refer (ready)])
  (:require
    ;; <other stuff>
    [cljs.core.async :as async :refer (<! >! put! chan)]
    [taoensso.sente  :as sente :refer (cb-success?)]
    [jayq.core :refer [$ on add-class remove-class hide show]]
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                  {:type :auto ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(println "Clojurescript working")
;; Routing
(defmulti event-msg-handler :id) ; Dispatch on event-id
;; Wrap for logging, catching, etc.:
(defn     event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println "Event: " event)
  (event-msg-handler ev-msg))

(do ; Client-side methods
  (defmethod event-msg-handler :default ; Fallback
    [{:as ev-msg :keys [event]}]
    (println "Unhandled event: " event))

  (defmethod event-msg-handler :chsk/state
    [{:as ev-msg :keys [?data]}]
    (if (= ?data {:first-open? true})
      (println "Channel socket successfully established!")
      (println "Channel socket state change: " ?data))
    (when (= (get ?data :first-open?) true)
      (chsk-send! [:dashboard/stories {:iteration-id 677953}] 5000 (fn [cb-reply]
                                              (reset! iterationDetail cb-reply)))))

  (defmethod event-msg-handler :chsk/recv
    [{:as ev-msg :keys [?data]}]
    (println "Push event from server: " ?data))

  ;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...
  )

;;react views

(def iterationDetail (atom {:stories {} :teams {}}))

(defn storyRow [story]
  [:tr
   [:td (:id @story)]
   [:td (:orderno @story)]
   [:td (:ticket @story)]
   [:td (:name @story)]
   [:td (if (> (:estimated_hours @story) 4) {:class "bg-warning"}) (:estimated_hours @story)]
   [:td (:customer_initials @story)]
   [:td (:tracker_initials @story)]
   [:td (:developer_initials @story)]
   [:td (:team_name @story)]
   [:td [:button.btn.btn-primary.btn-xs {:type "submit" :on-click #(swap! story assoc :estimated_hours (inc (:estimated_hours @story)))} "Test"]]]
  )

(defn storyTable [stories]
  [:table.table.table-condensed
   [:thead
    [:tr
     [:th "ID"]
     [:th "Order"]
     [:th "Ticket"]
     [:th "Story"]
     [:th "Est"]
     [:th "BZ"]
     [:th "SA"]
     [:th "DEV"]
     [:th "Team"]
     [:th "Action"]]]
   [:tbody
    (map (fn [story]
           ^{:key (:id story)} [storyRow (reagent/wrap story swap! stories assoc (:id story))])
         (vals @stories))
    ]
   ])

(defn teamRow [team]
  [:tr
   [:td (:id @team)]
   [:td (:name @team)]
   [:td (:cool-name @team)]
   [:td 4]
   [:td 4]
   [:td (:story_units @team)]
   [:td (:completed_story_units @team)]
   [:td ""]])

(defn teamsTable [teams]
  [:table.table.table-condensed
   [:thead
    [:tr
     [:th "ID"]
     [:th "Name"]
     [:th "AKA"]
     [:th "BE"]
     [:th "TE"]
     [:th "SU"]
     [:th "CU"]]
    ]
   [:tbody
    (map (fn [team]
           ^{:key (:id team)} [teamRow (reagent/wrap team swap! teams assoc (:id team))])
         (vals @teams))
    ]])

(defn iteration []
  (fn []
    [:div.col-md-12
     [:div#storyTable
     [storyTable (reagent/wrap (:stories @iterationDetail) swap! iterationDetail assoc :stories)]]
     [:div#teamsTable {:style {:display "none"}}
     [teamsTable (reagent/wrap (:teams @iterationDetail) swap! iterationDetail assoc :teams)]]
     ]))



;;init
(def     router_ (reagent/atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start! []
  (start-router!))

(start!)



(ready
  (reagent/render-component [iteration] (.getElementById js/document "content"))



  (on ($ :#teams) :click
      (fn []
        ;(swap! iterationDetail reset! :stories {1 {:id 1 :name "test" :estimated_hours 2} 2 {:id 2 :name "AQR" :estimated_hours 2}})

        ;(chsk-send! [:dashboard/stories {:iteration-id 668460}] 5000 (fn [cb-reply]
        ;                                                               (reset! iterationDetail cb-reply)))

        ;(println "detail" @iterationDetail)
        (remove-class ($ :li#stories-tab) :active)
        (add-class ($ :li#teams-tab) :active)
        (hide ($ :#storyTable) 500 #(show ($ :#teamsTable) 500))
        ;(chsk-send! [:example/button1 {:had-a-callback? "nope"}] 5000 (fn [cb-reply] (println "reply: " cb-reply)))
        ))

  (on ($ :#stories) :click
      (fn []
        (remove-class ($ :li#teams-tab) :active)
        (add-class ($ :li#stories-tab) :active)
        (hide ($ :#teamsTable) 500 #(show ($ :#storyTable) 500))
        ))
  )

