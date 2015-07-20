(ns dashboard.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.string :as s]
            [util.wiki :as wiki]
            [clojure.string :as str]))

;; TODO Deal with projects
(def default-project-id 298)

(defqueries "sql/db.sql")

(defn iteration-stories-map
  [db iterationId]
  (let [stories (iteration-stories db iterationId)
        stories-with-html (map #(assoc % :html_description (wiki/wiki->html (:description %))) stories)]
    (zipmap (map :id stories) stories-with-html)))

(defn initial-iteration [db project-id]
  (if-let [current (seq (current-iteration db project-id))]
    (first current)
    (first (last-iteration db project-id))
    ))

(def status-map {"p" "New"
                 "n" "Incomplete"
                 "r" "Ready for Review"
                 "e" "Ready for Dev"
                 "w" "In Progress"
                 "c" "Implemented"
                 "q" "Passed QA"
                 "i" "Passed UAT"
                 "f" "Issue Found"
                 "v" "Release Ready"})

(defn team-lead-map [db]
  (group-by :team_id (team-leads db))
  )

(defn team-summary-reduction
  ([team] (team-summary-reduction {} team))
  ([summary team]
   (let [status (status-map (:status team))
         status-key (keyword (s/replace (s/lower-case status) " " "-"))]
     (->
       summary
       (conj (select-keys team [:id :name :cool_name :iteration_id :team_estimate]))
       (assoc :epics (conj (get summary :epics (hash-set)) (:epic_name team)))
       (assoc :estimated_hours (+ (:estimated_hours summary 0) (:estimated_hours team 0)))
       (assoc status-key (+ (status-key summary 0) (:estimated_hours team 0))))
     ))
  )

(defn summarize-team-data
  [team-map team-leads]
  (into {} (for [[k v] team-map] [k (assoc (reduce team-summary-reduction {} v) :team_leads (get team-leads k))]))
  )

(defn iteration-teams-summary
  [db iterationId]
  (let [team-stories (iteration-teams db iterationId)
        team-map (group-by :id (sort-by :id team-stories))
        team-leads (team-lead-map db)]
    (summarize-team-data team-map team-leads)
    )
  )

(defn save-team-estimate
  [db iteration-id team-id team-estimate]
  (println (empty? (select-iteration-team db iteration-id team-id)))
  (if (empty? (select-iteration-team db iteration-id team-id))
    (insert-team-estimate! db iteration-id team-id team-estimate)
    (update-team-estimate! db team-estimate iteration-id team-id)
    ))

(defn project-epics
  [db project-id]

  )
