(ns dashboard.db
  (:require [yesql.core :refer [defqueries]]))

(defqueries "sql/db.sql")

(def db-spec {:subprotocol "mysql" :subname "//localhost:3306/xplanner?user=xplanner&password=xp"})


(defn iterationDetail
  [iterationId]
  (let [stories (iteration-stories db-spec iterationId)]
    {:iteration (iteration db-spec iterationId) :stories (zipmap (map :id stories) stories)}))


