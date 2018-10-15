(ns sherlog.core
  (:require
   [sherlog.log :as log]
   [sherlog.xray :as xray]))

(defn tail [log-group]
  (->> (log/latest-log-stream log-group)
       (log/log-seq log-group)))

(defn search [log-group pattern duration]
  (log/search log-group pattern duration))

(defn stats [duration]
  (->> (xray/stats duration)
       (flatten)
       (remove nil?)))

(defn init! [auth]
  (log/init! auth)
  (xray/init! auth))

(comment
  (init! {:auth-type :profile
          :profile   :prod-core
          :region    "us-east-1"}))
