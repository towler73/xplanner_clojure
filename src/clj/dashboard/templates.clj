(ns dashboard.templates
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.form :refer [hidden-field]]
            [hiccup.def :refer [defhtml defelem]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn header [title additions]
  [:head
   [:title title]
   (include-css "css/bootstrap.min.css")
   (include-css "css/dashboard.css")
   additions]
  )

(defn main []
  (html5
    (html
      (header "XPlanner 2" (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js" "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js" "js/app.js"))
      [:body
       [:div.container-fluid
        [:div.row
         [:div#nav-bar.col-md-12]
         [:div#iterationDetail.col-md-12]
         [:div#storiesDetail.col-md-12]
         [:div#teamsDetail.col-md-12]
         [:div#content.col-md-12]
         ]]])))


(defn login []
  (html5
    (html
      (header "XPlanner 2 Login" nil)
      [:body
       [:div.container-fluid
        [:div.row
         [:div#login.col-md-4.col-md-offset-4
          [:div#login-panel.panel.panel-info
           [:div.panel-heading "XPlanner" [:sup "2"] " Login"]
           [:div.panel-body
            [:form {:action "login" :method "post"}
             (hidden-field "csrf-token" *anti-forgery-token*)
             [:div.form-group
              [:label {:for "username"} "Username"]
              [:input#username.form-control {:name "username" :type "text" :placeholder "Enter Username"}]]
             [:div.form-group
              [:label {:for "password"} "Password"]
              [:input#password.form-control {:name "password" :type "password" :placeholder "Enter Password"}]]
             [:button.btn.btn-primary {:type "submit"} "Submit"]
             ]]]]]]])))








