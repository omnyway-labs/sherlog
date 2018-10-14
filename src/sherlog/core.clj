(ns sherlog.core
  (:require
   [sherlog.log :as log]))


(defn tail [log-group]
  (->> (log/latest-log-stream log-group)
       (log/log-seq log-group)))

(defn trace []
  )

(defn search [log-group pattern duration]
  (log/search log-group pattern duration))

(defn init! [auth]
  (log/init! auth))


(comment
  (init! {:auth-type :profile
          :profile :staging
          :region  "us-east-1"}))
