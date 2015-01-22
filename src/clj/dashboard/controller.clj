(ns dashboard.controller
  (:require [dashboard.templates :as templates]
            [dashboard.db :as db]))

(defn index [req]
  (templates/main req (db/iterationDetail 677953)))