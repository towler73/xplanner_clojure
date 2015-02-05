(ns dashboard.db-test
  (:require [clojure.test :refer :all]
            [dashboard.db :refer :all]))

(deftest sum-units-test
  (is (= {:estimated_hours 10} (sum-units {:estimated_hours 5} {:estimated_hours 5})))
  (is (= {:estimated_hours 5} (sum-units {:estimated_hours 5} {})))
  (is (= {:estimated_hours 5} (sum-units {} {:estimated_hours 5}))))

(deftest sum-units-by-status-test
  (is (= {:ready-for-dev 5} (sum-units-by-status {:estimated_hours 5 :status "e"} {})))
  (is (= {:ready-for-dev 5 :implemented 4 :passed-qa 6}
         (sum-units-by-status {:estimated_hours 2 :status "c"} {:ready-for-dev 5 :implemented 2 :passed-qa 6})))
  (is (= {:ready-for-dev 5 :implemented 2 :passed-qa 6}
         (sum-units-by-status {:estimated_hours 2 :status "c"} {:ready-for-dev 5 :passed-qa 6})))
  (testing "merging original with results of sum-units-by-status"
    (let [team {:id 1 :estimated_hours 4 :status "c"}]
      (is (= {:id 1 :estimated_hours 4 :implemented 4} (add-units-by-status team)))
      )
    )

  )

(deftest sum-units-and-sum-status-test
  (let [team {:id 1 :estimated_hours 4 :status "c"}]
    (is (= {:estimated_hours 4 :implemented 4} (sum-units-by-status team (sum-units team {}))))
    (is (= {:id 1 :estimated_hours 8 :implemented 8} (sum-units-by-status team (sum-units team {:id 1 :estimated_hours 4 :implemented 4}))))
    )
  )

(deftest group-by-team
  (is (= {1 {:id 1 :estimated_hours 6 :ready-for-dev 1 :implemented 5}
          2 {:id 2 :estimated_hours 4 :passed-qa 4}}
         (reduce-team-stories (list {:id 1 :estimated_hours 1 :status "e"}
                                    {:id 1 :estimated_hours 1 :status "c"}
                                    {:id 1 :estimated_hours 4 :status "c"}
                                    {:id 2 :estimated_hours 1 :status "q"}
                                    {:id 2 :estimated_hours 3 :status "q"}
                                    )))))


