(ns dashboard.client-macros
  (:require [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop sub pub  )]))

(defmacro subscribe-to
  [publication key do-this]
  (let [macro-event (gensym 'event)]
    `(let [~macro-event (chan)]
       (sub ~publication ~key ~macro-event)
       (go-loop []
         ~(apply do-this (<! ~macro-event))
         (recur))))
  )
