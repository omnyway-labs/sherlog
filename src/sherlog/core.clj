(ns sherlog.core
  (:require
   [sherlog.log :as log]))


(defn tail [service]
  (clog/init! (env/get-aws-config))
  (let [log-stream (log/latest-log-stream log-group)]
    (log/log-seq log-group log-stream)))

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
