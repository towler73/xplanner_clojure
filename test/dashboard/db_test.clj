(ns dashboard.db-test
  (:require [clojure.test :refer :all]
            [dashboard.db :refer :all]))

(deftest team-summary-first-team-test
  (is (= {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epics #{"Epic 1"} :estimated_hours 4 :in-progress 4}
        (team-summary-reduction {} {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epic_name "Epic 1" :estimated_hours 4 :status "w"})))
  )

(deftest team-summary-multiple-teams-same-epic-test
  (is (= {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epics #{"Epic 1"} :estimated_hours 8 :in-progress 8}
         (team-summary-reduction {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epics #{"Epic 1"} :estimated_hours 4 :in-progress 4} {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epic_name "Epic 1" :estimated_hours 4 :status "w"})))
  )

(deftest team-summary-multiple-teams-different-epic-test
  (is (= {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epics #{"Epic 2" "Epic 1"} :estimated_hours 8 :in-progress 4 :implemented 4}
         (team-summary-reduction {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epics #{"Epic 1"} :estimated_hours 4 :in-progress 4} {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epic_name "Epic 2" :estimated_hours 4 :status "c"})))
  )

(deftest summarzie-team-data-test
  (is (= {1 {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epics #{"Epic 2" "Epic 1"} :estimated_hours 8 :in-progress 4 :implemented 4 :team_leads "John Smith"}
          2 {:id 2 :name "Team 2" :cool_name "Cool Name 2" :iteration_id 10 :team_estimate 100 :epics #{"Epic 1"} :estimated_hours 4 :passed-qa 4 :team_leads "Sally Hall, Larry Long"}}
         (summarize-team-data {1 [{:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epic_name "Epic 1" :estimated_hours 4 :status "w"}
                                  {:id 1 :name "Team 1" :cool_name "Cool Name 1" :iteration_id 10 :team_estimate 100 :epic_name "Epic 2" :estimated_hours 4 :status "c"}]
                               2 [{:id 2 :name "Team 2" :cool_name "Cool Name 2" :iteration_id 10 :team_estimate 100 :epic_name "Epic 1" :estimated_hours 4 :status "q"}]}
                              {1 "John Smith"
                               2 "Sally Hall, Larry Long"})))
  )

