(ns dashboard.templates
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.def :refer [defhtml defelem]]))

(defn main []
  (html5
    (html
      [:head
       [:title "Home"]
       (include-css "css/bootstrap.min.css")
       (include-css "css/dashboard.css")
       (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js" "js/app.js")]
      [:body
       [:div.container-fluid
        [:div.row
         [:div#nav-bar.col-md-12]
         [:div#iterationDetail.col-md-12]
         [:div#storiesDetail.col-md-12]
         [:div#teamsDetail.col-md-12]
         ]]])))




