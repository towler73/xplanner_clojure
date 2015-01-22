(ns dashboard.templates
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.def :refer [defhtml defelem]]))

(defhtml navbar []
         [:nav.navbar.navbar-default
          [:div.container-fluid
           [:div.navbar-header
            [:a.navbar-brand {:href "#"} "Xplanner"]]
           [:div.collapse.navbar-collapse
            [:form.navbar-form.navbar-left
             [:div.form-group
              [:select.form-control
               [:option "Iteration 1"]
               [:option "Iteration 2"]]]]
            [:ul.nav.navbar-nav
             [:li#stories-tab.active {:role "presentation"} [:a#stories {:href "#"} "Stories"]]
             [:li#teams-tab {:role "presentation"} [:a#teams {:href "#"} "Teams"]]
             ]]]
          ])

(defn main [req iterationDetail]
  (html5
    (html
      [:head
       [:title "Home"]
       (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")
       (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js" "js/app.js")]
      [:body
       [:div.container-fluid
        [:div.row
         [:div.col-md-12
          (navbar)]
         [:div#content.col-md-12
          ;[:table.table.table-condensed
          ; [:thead
          ;  [:tr
          ;   [:th "ID"]
          ;   [:th "Order"]
          ;   [:th "Ticket"]
          ;   [:th "Story"]
          ;   [:th "Est"]
          ;   [:th "BZ"]
          ;   [:th "SA"]
          ;   [:th "DEV"]
          ;   [:th "Team"]]]
          ; [:tbody
          ;  (map (fn [story]
          ;         [:tr
          ;          [:td (:id story)]
          ;          [:td (:orderno story)]
          ;          [:td (:ticket story)]
          ;          [:td (:name story)]
          ;          [:td (if (> (:estimated_hours story) 4) {:class "bg-warning"}) (:estimated_hours story)]
          ;          [:td (:customer_initials story)]
          ;          [:td (:tracker_initials story)]
          ;          [:td (:developer_initials story)]
          ;          [:td (:team_name story)]])
          ;       (:stories iterationDetail))
          ;  ]
          ]]]])))




