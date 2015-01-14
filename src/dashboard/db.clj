(ns dashboard.db
  (:require [yesql.core :refer [defqueries]]))

(defqueries "sql/db.sql")

(def db-spec {:subprotocol "mysql" :subname "//localhost:3306/xplanner?user=root&password=l4cr0ss3"})


(defn iterationDetail
  [iterationId]
    {:iteration (iteration db-spec iterationId) :stories (iteration-stories db-spec iterationId)})