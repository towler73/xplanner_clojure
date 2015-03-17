(ns db.schema
  (:require
    [clj-dbcp.core :as cp]
    [clj-liquibase.change :as ch]
    [clj-liquibase.cli :as cli])
  (:use
  [clj-liquibase.core :only (defchangelog)]))

(def create-iteration-team-table ["1" "bmorgan"
                                  [(ch/create-table :iteration_team
                                                    [[:id :int :null false :pk true :autoinc true]
                                                     [:iteration_id :int :null false]
                                                     [:team_id :int :null false]
                                                     [:business_estimate :int]
                                                     [:team_estimate :int]])]])

(def add-team-lead-flag ["2" "bmorgan"
                         [(ch/add-columns :team_member [[:lead [:tinyint 1]]])]])

(defchangelog dashboard-changelog "db.schema"
              [create-iteration-team-table
               add-team-lead-flag])

(def ds (cp/make-datasource :mysql {:host "localhost" :database "xplanner" :user "root" :password "l4cr0ss3"}))


(defn -main
  [& [cmd & args]]
  (apply cli/entry cmd {:datasource ds :changelog dashboard-changelog} args))
