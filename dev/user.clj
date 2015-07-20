(ns user
 "Tools for interactive development with the REPL. This file should
 not be included in a production build of the application."
 (:require
  [clojure.java.io :as io]
  [clojure.java.javadoc :refer [javadoc]]
  [clojure.pprint :refer [pprint]]
  [clojure.reflect :refer [reflect]]
  [clojure.repl :refer [apropos dir doc find-doc pst source]]
  [clojure.set :as set]
  [clojure.string :as str]
  [clojure.test :as test]
  [clojure.tools.namespace.repl :refer [refresh refresh-all]]
  [dashboard.main :refer [dashboard-system]]
  [com.stuartsierra.component :as component]
  [clojure.zip :as zip]
  [clojure.xml :as xml]
  ))

(def system
 "A Var containing an object representing the application under
 development."
 nil)

(defn init
 "Creates and initializes the system under development in the Var
 #'system."
 []
 (alter-var-root #'system (constantly (dashboard-system)))
 )

(defn start
 "Starts the system running, updates the Var #'system."
 []
 (alter-var-root #'system component/start)
 )

(defn stop
 "Stops the system if it is currently running, updates the Var
 #'system."
 []
 (alter-var-root #'system component/stop)
 )

(defn go
 "Initializes and starts the system running."
 []
 (init)
 (start)
 :ready)

(defn reset
 "Stops the system, reloads modified source files, and restarts it."
 []
 (stop)
 (refresh :after 'user/go))
(ns user
  (:import (java.io PrintWriter)))

(defn- tree-edit [zipper editor]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if (= :description (:tag (zip/node loc)))
        (let [new-loc (zip/edit loc editor)]
          (recur (zip/next new-loc)))
        (recur (zip/next loc))))
      )
    )

(defn- editor [node]
  (let [content (get (:content node) 0)]
    (assoc-in node [:content 0] (clojure.string/escape content {\< "&lt;" \> "&gt;" \& "&amp;"}))
    )
  )

(defn- components-write [out components]
  (binding [*out* (PrintWriter. out)]
    (xml/emit {:tag :component-set
               :content
                    [{:tag :components
                      :content components}]})
    (.flush *out*)))

(defn fix-components-xml-merge [res]
  (let [zipper (->> res xml/parse zip/xml-zip)]
    (->> (tree-edit zipper editor) zip/xml-zip zip/children
         (filter #(= (:tag %) :components))
         first :content)
    )
  )

(def components-merger [fix-components-xml-merge into components-write])


