(ns dashboard.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.string :as s]
            [util.wiki :as wiki]))


(defqueries "sql/db.sql")

(defn iteration-stories-map
  [db iterationId]
  (let [stories (iteration-stories db iterationId)
        stories-with-html (map #(assoc % :html_description (wiki/wiki->html (:description %))) stories)]
    (zipmap (map :id stories) stories-with-html)))

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

(defn sum-units
  [team team-result]
  (assoc team-result :estimated_hours (+ (:estimated_hours team-result 0) (:estimated_hours team 0)))
  )

(defn team-summary-reduction
  ([team] (team-summary-reduction {} team))
  ([summary team]
   (assoc summary :estimated_hours (+ (:estimated_hours summary 0) (:estimated_hours team 0)))
   (let [status (status-map (:status team))
         status-key (keyword (s/replace (s/lower-case status) " " "-"))]
     (assoc summary status-key (+ (status-key summary 0) (:estimated_hours team 0))))
    ))

(defn sum-units-by-status
  [team team-result]
  (let [status (status-map (:status team))
        status-key (keyword (s/replace (s/lower-case status) " " "-"))]
    (assoc team-result status-key (+ (status-key team-result 0) (:estimated_hours team 0))))
  )

(defn add-units-by-status
  [team]
  (dissoc (merge team (sum-units-by-status team {})) :status)
  )

(defn reduce-team-stories
  ([teams] (reduce-team-stories teams (sorted-map)))
  ([teams result]
   (let [team (first teams)
         remaining-teams (rest teams)
         team-id (:id team)]
     (if-not team
       result
       (if-let [team-result (get result team-id)]
         (recur remaining-teams (assoc result team-id (sum-units-by-status team (sum-units team team-result))))
         (recur remaining-teams (assoc result team-id (add-units-by-status team))))))))

(defn iteration-teams-summary
  [db iterationId]
  (let [team-stories (iteration-teams db iterationId)]
    (reduce-team-stories team-stories)
    )
  )

(defn save-team-estimate
  [db iteration-id team-id team-estimate]
  (println (empty? (select-iteration-team db iteration-id team-id)))
  (if (empty? (select-iteration-team db iteration-id team-id))
    (insert-team-estimate! db iteration-id team-id team-estimate)
    (update-team-estimate! db team-estimate iteration-id team-id)
    ))
