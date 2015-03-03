(ns dashboard.components.db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :refer [make-datasource close-datasource]]))

(defrecord Database [host datasource]
  component/Lifecycle

  (start [component]
    (println ";; Starting database")

    (let [datasource-options {:adapter  "mysql"
                              :url      "jdbc:mysql://localhost:3306/xplanner"
                              :username "xplanner"
                              :password "xp"}
          datasource (make-datasource datasource-options)]
      (assoc component :datasource datasource)
      )

    )
  (stop [component]
    (let [datasource (:datasource component)]
      (close-datasource datasource)
      )
    )

  )

(defn new-database [host]
  (map->Database {:host host}))
