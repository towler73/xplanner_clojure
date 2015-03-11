(ns dashboard.app
  (:require-macros
  [cljs.core.async.macros :as asyncm :refer (go go-loop)]
  [jayq.macros :refer (ready)])
  (:require
    ;; <other stuff>
    [cljs.core.async :as async :refer (<! >! put! chan pub sub unsub)]
    [taoensso.sente :as sente :refer (cb-success?)]
    [jayq.core :refer [$ on add-class remove-class hide show]]
    [reagent.core :as reagent :refer [atom]]
    ))

(enable-console-print!)

;; event management


(def publisher (chan))

(def publication (pub publisher #(:topic %)))

(defn publish-event
  ([topic-key] (publish-event topic-key {}))
  ([topic-key data]
    (println topic-key data)
    (go (>! publisher (assoc data :topic topic-key))))
  )

(defn subscribe
  [key do-this]
  (let [event (chan)]
    (sub publication key event)
    (go-loop []
             (let [data (<! event)]
               (do-this data))
             (recur)))
  )

;; Asnyc connection

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"                   ; Note the same path as before
                                  {:type :auto              ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk chsk)
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def chsk-state state)                                    ; Watchable, read-only atom
  )

(println "Clojurescript working")
;; Routing
(defmulti event-msg-handler :id)                            ; Dispatch on event-id
;; Wrap for logging, catching, etc.:
(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println "Event: " event)
  (event-msg-handler ev-msg))

(do                                                         ; Client-side methods
  (defmethod event-msg-handler :default                     ; Fallback
    [{:as ev-msg :keys [event]}]
    (println "Unhandled event: " event))

  (defmethod event-msg-handler :chsk/state
    [{:as ev-msg :keys [?data]}]
    (if (= ?data {:first-open? true})
      (println "Channel socket successfully established!")
      (println "Channel socket state change: " ?data))
    (when (= (get ?data :first-open?) true)
      (publish-event :load-project {:project-id 298})))

  (defmethod event-msg-handler :chsk/recv
    [{:as ev-msg :keys [?data]}]
    (println "Push event from server: " ?data))

  ;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...
  )



;;react views

(defn logged-in-as []
  (let [user (atom {})]
    (subscribe :load-project (fn [data] (chsk-send! [:dashboard/logged-in-user] 5000 (fn [cb-reply] (reset! user cb-reply)))))

    (fn []
      [:p.navbar-text.navbar-right "Signed in as " (:name @user)]
      )
    )

  )


(defn nav-bar []
  (let [iterations (atom {})
        current-iteration-id (atom 0)
        active-tab (atom :stories)]
    (subscribe :load-iteration #(reset! current-iteration-id (:iteration-id %)))
    (subscribe :show-section #(reset! active-tab (:section %)))
    (subscribe :load-project (fn [data] (chsk-send! [:dashboard/project-iterations data] 5000 (fn [cb-reply]
                                                                                                (reset! iterations (:iterations cb-reply))
                                                                                                (reset! current-iteration-id (:current-iteration-id cb-reply))
                                                                                                (publish-event :load-iteration {:iteration-id (:current-iteration-id cb-reply)})))))
    (fn []
      [:nav.navbar.navbar-default
       [:div.container-fluid
        [:div.navbar-header
         [:a.navbar-brand {:href "#"} "Yplanner"]]
        [:div.collapse.navbar-collapse
         [:form.navbar-form.navbar-left
          [:div.form-group
           [:select.form-control {:value @current-iteration-id :on-change #(publish-event :load-iteration {:iteration-id (-> % .-target .-value)})}
            (map (fn [iteration]
                   ^{:key (:id iteration)} [:option {:value (:id iteration)} (:name iteration)])
                 @iterations)]]]
         [:ul.nav.navbar-nav
          [:li#stories-tab {:role "presentation" :class (when (= :stories @active-tab) "active")} [:a#stories {:href "#" :on-click #(publish-event :show-section {:section :stories})} "Stories"]]
          [:li#teams-tab {:role "presentation" :class (when (= :teams @active-tab) "active")} [:a#teams {:href "#" :on-click #(publish-event :show-section {:section :teams})} "Teams"]]
          ]
         [logged-in-as]]
        ]
       ])))

(defn showStories [id]
  (println "show Stories called")
  (go (>! publisher {:topic :hide-stories :id id}))
  )

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
   ;[:td [:button.btn.btn-primary.btn-xs {:type "submit" :on-click #(swap! story assoc :estimated_hours (inc (:estimated_hours @story)))} "Test"]]]
   [:td [:button.btn.btn-primary.btn-xs {:type "submit" :on-click #(showStories (:id @story))} "Test"]]]
  )

(defn storyTable []
  (let [visible? (atom true)
        stories (atom {})]
    (subscribe :show-section #(if (= :stories (:section %)) (reset! visible? true) (reset! visible? false)))
    (subscribe :load-iteration (fn [data] (chsk-send! [:dashboard/stories data] 5000 (fn [cb-reply] (reset! stories cb-reply)))))
    (fn []
      [:table.table.table-condensed (when-not @visible? {:class "hidden"})
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
       ])))

(defn teamRow [team]
  (let [editing? (atom false)
        editing-team-estimate-value (atom "")
        save #(do (swap! team assoc :team_estimate @editing-team-estimate-value)
                  (chsk-send! [:dashboard/save-team-estimate (select-keys @team [:iteration_id :id :team_estimate])] 5000 (fn [cb-reply] (println cb-reply)))
                  (reset! editing? false)
                  nil)
        cancel #(do (reset! editing? false) nil)
        ]

    (fn [team]
      [:tr
       [:td [:button.btn.btn-default.btn-xs {:type "submit" :on-click #(do (reset! editing? true) (reset! editing-team-estimate-value (:team_estimate @team)) nil)} [:span.glyphicon.glyphicon-edit]]]
       [:td (:id @team)]
       [:td (:name @team)]
       [:td (:cool_name @team)]
       (if @editing?
         [:td [:input#team-estimate-input.form-control.input-sm {:type        "text"
                                                             :value       @editing-team-estimate-value
                                                             :on-change   #(reset! editing-team-estimate-value (-> % .-target .-value))
                                                             :on-blur     save
                                                             :on-key-down #(case (.-which %)
                                                                            13 (save)
                                                                            27 (cancel)
                                                                            nil)
                                                             }]]
         [:td.text-center (:team_estimate @team)]
         )
       [:td.text-center (:estimated_hours @team)]
       [:td.text-center (:in-progress @team)]
       [:td.text-center (:issue-found @team)]
       [:td.text-center (:implemented @team)]
       [:td.text-center (:passed-qa @team)]
       [:td [:svg {:width 200 :height 10}
             (let [total-width (* 5 (:estimated_hours @team))
                   complete-width (* 5 (+ (:passed-qa @team 0) (:passed-uat @team 0) (:release-ready @team 0)))
                   impl-width (* 5 (:implemented @team 0))
                   if-width (* 5 (:issue-found @team 0))
                   ip-width (* 5 (:in-progress @team 0))
                   team-estimate-x (* 5 (:team_estimate @team 0))]
               [:g
                [:rect {:width total-width :height 10 :style {:fill "#FFFF9D"}}]
                [:rect {:width complete-width :height 10 :style {:fill "#00A388"}}]
                [:rect {:x complete-width :width impl-width :height 10 :style {:fill "#79BD8F"}}]
                [:rect {:x (+ complete-width impl-width) :width if-width :height 10 :style {:fill "#FF6138"}}]
                [:rect {:x (+ complete-width impl-width if-width) :width ip-width :height 10 :style {:fill "#BEEB9F"}}]
                [:line {:x1 team-estimate-x :y1 0 :x2 team-estimate-x :y2 10 :style {:stroke "#000" :stroke-width "1"}}]
                ]

               )
             ]]])))

(defn teamsTable []
  (let [visible? (atom false)
        teams (atom {})]
    (subscribe :show-section #(if (= :teams (:section %)) (reset! visible? true) (reset! visible? false)))
    (subscribe :load-iteration (fn [data] (chsk-send! [:dashboard/iteration-teams data] 5000 (fn [cb-reply] (reset! teams cb-reply)))))
    (fn []
      [:table.table.table-condensed (when-not @visible? {:class "hidden"})
       [:thead
        [:tr
         [:th "Actions"]
         [:th "ID"]
         [:th "Name"]
         [:th "AKA"]
         [:th "TE"]
         [:th "SU"]
         [:th "IP"]
         [:th "IF"]
         [:th "I"]
         [:th "PQA"]
         [:th "Progress"]
         ]
        ]
       [:tbody
        (map (fn [team]
               ^{:key (select-keys team [:id :iteration_id])} [teamRow (reagent/wrap team swap! teams assoc (:id team))])
             (vals @teams))
        ]])))

(defn iteration []
  (fn []
    [:div.col-md-12
     [:div#storyTable
      [storyTable (reagent/wrap (:stories @iterationDetail) swap! iterationDetail assoc :stories)]]
     ]))


;; requests
(defn updateIterationTeams []
  (chsk-send! [:dashboard/iteration-teams {:iteration-id 668460}] 5000 (fn [cb-reply]
                                                                         (reset! teams cb-reply)))

  )

;;init
(def router_ (reagent/atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-chsk-router! ch-chsk event-msg-handler*)))

(defn start! []
  (start-router!))

(start!)


(ready
  (reagent/render-component [nav-bar] (.getElementById js/document "nav-bar"))
  (reagent/render-component [storyTable] (.getElementById js/document "storiesDetail"))
  (reagent/render-component [teamsTable] (.getElementById js/document "teamsDetail"))



  ;(on ($ :#teams) :click
  ;    (fn []
  ;      ;(swap! iterationDetail reset! :stories {1 {:id 1 :name "test" :estimated_hours 2} 2 {:id 2 :name "AQR" :estimated_hours 2}})
  ;
  ;      ;(chsk-send! [:dashboard/stories {:iteration-id 668460}] 5000 (fn [cb-reply]
  ;      ;                                                               (reset! iterationDetail cb-reply)))
  ;
  ;      ;(println "detail" @iterationDetail)
  ;      (remove-class ($ :li#stories-tab) :active)
  ;      (add-class ($ :li#teams-tab) :active)
  ;      (hide ($ :#storiesDetail) 500 #(show ($ :#teamsDetail) 500))
  ;      (updateIterationTeams)
  ;      ;(chsk-send! [:example/button1 {:had-a-callback? "nope"}] 5000 (fn [cb-reply] (println "reply: " cb-reply)))
  ;      ))
  ;
  ;(on ($ :#stories) :click
  ;    (fn []
  ;      (remove-class ($ :li#teams-tab) :active)
  ;      (add-class ($ :li#stories-tab) :active)
  ;      (hide ($ :#teamsDetail) 500 #(show ($ :#storiesDetail) 500))
  ;      ))
  )

