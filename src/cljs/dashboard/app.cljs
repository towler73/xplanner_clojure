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
    [reagent-modals.modals :as modals]
    [clojure.string :as str]
    [secretary.core :as secretary :refer-macros [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import goog.History))

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

;; Global state
(def init-state (atom {:initialized false}))
(def app-state (atom {:current-page {:key nil page nil} :current-project-id nil :current-iteration-id nil}))

(defn put-state! [k v]
  (swap! app-state assoc k v))


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
      (swap! init-state assoc :initialized true)
      (println "init-state " @init-state)
      (when-let [data (:initialization-data @init-state)]
        (do
          (publish-event :change-page-state data)
          (swap! init-state assoc :initiaization-data nil))
        )
      ))

  (defmethod event-msg-handler :chsk/recv
    [{:as ev-msg :keys [?data]}]
    (println "Push event from server: " ?data))

  ;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...
  )

;;react views


(defn nav-bar []
  (let [iterations (:project-iterations @app-state)
        active-tab (:key (:current-page @app-state))
        current-project-id (:current-project-id @app-state)
        current-iteration-id (:current-iteration-id @app-state)]
    [:nav.navbar.navbar-default
     [:div.container-fluid
      [:div.navbar-header
       [:a.navbar-brand {:href "#"} "Xplanner" [:sup "2"]]]
      [:div.collapse.navbar-collapse
       [:form.navbar-form.navbar-left
        [:div.form-group
         [:select.form-control {:value current-iteration-id :on-change #(set! (.-location js/window) (section-iteration-route {:project-id (:current-project-id @app-state) :page (name (:key (:current-page @app-state))) :iteration-id (-> % .-target .-value)}))}
          (map (fn [iteration]
                 ^{:key (:id iteration)} [:option {:value (:id iteration)} (:name iteration)])
               iterations)]]]
       [:button#refresh-btn.btn.btn-primary.navbar-btn.navbar-left {:type button :on-click #(publish-event :change-page-state {:project-id current-project-id :iteration-id current-iteration-id :page-key active-tab})} [:span.glyphicon.glyphicon-refresh]]
       [:ul.nav.navbar-nav
        [:li#stories-tab {:role "presentation" :class (when (= :stories active-tab) "active")} [:a#stories {:href (str "#/project/" current-project-id "/iteration/" current-iteration-id "/stories")} "Stories"]]
        [:li#teams-tab {:role "presentation" :class (when (= :teams active-tab) "active")} [:a#teams {:href (str "#/project/" current-project-id "/iteration/" current-iteration-id "/teams")} "Teams"]]
        ]
       [:p.navbar-text.navbar-right "Signed in as " (:name (:current-user @app-state))]
       ]
      ]
     ]
    ))


(defn story-view [story]
  [:div.modal-content
   [:div.modal-header
    [:button.close {:type "button" :data-dismiss "modal"} "×"]
    [:h4.modal-title (:name story)]]
   [:div.modal-body
    [:p {:dangerouslySetInnerHTML {:__html (:html_description story)}}]]])

(defn storyRow [story]
  [:tr
   [:td [:button.btn.btn-default.btn-xs {:on-click #(modals/modal! (story-view @story))} [:span.glyphicon.glyphicon-eye-open]]]
   [:td (:id @story)]
   [:td (:orderno @story)]
   [:td (:ticket @story)]
   [:td (:epic_name @story)]
   [:td (:name @story)]
   [:td (if (> (:estimated_hours @story) 4) {:class "bg-warning"}) (:estimated_hours @story)]
   [:td (:customer_initials @story)]
   [:td (:tracker_initials @story)]
   [:td (:developer_initials @story)]
   [:td (:team_name @story)]
   [:td (:release_name @story)]
   ]
  )

(defn sort-map-by [data-map sort-key]
  (sort-by #(sort-key (val %)) < data-map)
  )

(defn sortable-header [sort-fn label key]
  [:th [:a {:on-click #(sort-fn key)} label]]
  )

(defn storyTable []
  (let [stories (:stories @app-state)
        sort-fn (fn [sort-key] (put-state! :stories (sort-map-by stories sort-key)))
        story-sortable-header (partial sortable-header sort-fn)]
    [:table.table.table-condensed
     [:thead
      [:tr
       [:th "Action"]
       [story-sortable-header "ID" :id]
       [story-sortable-header "Order" :orderno]
       [story-sortable-header "Ticket" :ticket]
       [story-sortable-header "Epic" :epic_name]
       [story-sortable-header "Story" :name]
       [story-sortable-header "Est" :estimated_hours]
       [:th "BZ"]
       [:th "SA"]
       [:th "DEV"]
       [story-sortable-header "Team" :team_name]
       [story-sortable-header "Release" :release_name]
       ]]
     [:tbody
      (map (fn [story]
             ^{:key (:id story)} [storyRow (reagent/wrap story swap! stories assoc (:id story))])
           (vals stories))
      ]
     ]))

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
       [:td [:button.btn.btn-default.btn-xs {:type "submit" :on-click #(do
                                                                        (reset! editing? true)
                                                                        (reset! editing-team-estimate-value (:team_estimate @team))
                                                                        nil)} [:span.glyphicon.glyphicon-edit]]]
       [:td (:name @team)]
       [:td (:cool_name @team)]
       [:td (str/join "," (:epics @team))]
       [:td (str/join ", " (map :name (:team_leads @team)))]
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
  (let [teams (:teams @app-state)
        sort-fn (fn [sort-key] (put-state! :teams (into {} (sort-map-by teams sort-key))))
        team-sortable-header (partial sortable-header sort-fn)]
    [:table.table.table-condensed
     [:thead
      [:tr
       [:th "Actions"]
       (team-sortable-header "Team" :name)
       (team-sortable-header "AKA" :cool_name)
       [:th "Epics"]
       [:th "Leads"]
       [:th.text-center "Planned"]
       [:th.text-center "Current"]
       [:th.text-center "In Progress"]
       [:th.text-center "Issue Found"]
       [:th.text-center "Implemented"]
       [:th.text-center "Passed QA"]
       [:th "Progress"]
       ]
      ]
     [:tbody
      (map (fn [team]
             ^{:key (select-keys team [:id :iteration_id])} [teamRow (reagent/wrap team swap! teams assoc (:id team))])
           (vals teams))
      ]]))


(defn modal-dialog []
  [modals/modal-window])


;; Page View
(def page-map {:stories storyTable
               :teams   teamsTable})

(defn current-page-will-mount []
  (put-state! :current-page {:key :stories :page [storyTable]}))

(defn current-page-render []
  [(:current-page @app-state) :page])

(defn current-page []
  (reagent/create-class {:component-will-mount current-page-will-mount :render current-page-render}))

(defn load-iteration-stories [iteration-id]
  (chsk-send! [:dashboard/stories {:iteration-id iteration-id}] 5000 (fn [cb-reply] (put-state! :stories (sort-map-by cb-reply :orderno))))
  )

(defn load-iteration-teams [iteration-id]
  (chsk-send! [:dashboard/iteration-teams {:iteration-id iteration-id}] 5000 (fn [cb-reply] (put-state! :teams (into {} (sort-map-by cb-reply :name)))))
  )

(defn load-page-data [page iteration-id]
  (if (= page :stories)
    (load-iteration-stories iteration-id)
    (load-iteration-teams iteration-id)))

(subscribe :change-page-state (fn [data]
                                (let [{:keys [current-project-id]} @app-state
                                      {:keys [page-key iteration-id project-id]} data
                                      {:keys [initialized]} @init-state]
                                  (if initialized
                                    (do
                                      (put-state! :current-project-id project-id)
                                      (put-state! :current-iteration-id iteration-id)
                                      (put-state! :current-page {:key page-key :page [(page-key page-map)]})

                                      (when (not= current-project-id project-id)
                                        (chsk-send! [:dashboard/logged-in-user] 5000 (fn [cb-reply] (put-state! :current-user cb-reply)))
                                        (chsk-send! [:dashboard/project-iterations {:project-id project-id}] 5000 (fn [cb-reply] (put-state! :project-iterations (:iterations cb-reply)))))
                                      (load-page-data page-key iteration-id)
                                      )
                                    (swap! init-state assoc :initialization-data data))
                                  )))

;; routing

(secretary/set-config! :prefix "#")

(defroute section-iteration-route "/project/:project-id/iteration/:iteration-id/:page" [project-id iteration-id page]
          (publish-event :change-page-state {:project-id project-id :page-key (keyword page) :iteration-id iteration-id}))



;; browser history

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(hook-browser-navigation!)


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
  (reagent/render-component [current-page] (.getElementById js/document "storiesDetail"))
  (reagent/render-component [modal-dialog] (.getElementById js/document "content"))
  )

