(ns sherlog.core
  (:require
   [sherlog.log :as log]
   [sherlog.xray :as xray]
   [sherlog.metric :as metric]))

(defn tail
  ([log-group]
   (let [stream (log/latest-log-stream log-group)]
     (log/log-seq log-group stream)))
  ([log-group log-stream]
   (log/log-seq log-group log-stream)))

(defn search
  ([log-group pattern duration]
   (log/search-group log-group pattern duration))
  ([log-group streams pattern duration]
   (log/search-streams log-group streams pattern duration)))

(defn list-filters [log-group type]
  (condp = type
    :metric       (log/list-metric-filters log-group)
    :subscription (log/list-subscriptions log-group)))

(defn delete-filter [log-group name]
  (log/delete-metric-filter log-group name))

(defn find-api-traces [duration pattern]
  (xray/list-traces duration pattern))

(defn find-api-stats [duration]
  (xray/stats duration))

(defn find-metrics
  ([namespace]
   (metric/list-all namespace))
  ([namespace pattern]
   (->> (metric/list-all namespace)
        (map #(re-matches (re-pattern (str pattern ".*")) %))
        (remove nil?))))

(defn show-metric [namespace metric dimension duration]
  (->> (metric/get-stats namespace metric dimension duration)
       (sort-by :timestamp)
       (reverse)))

(defn init! [auth]
  (log/init! auth)
  (xray/init! auth)
  (metric/init! auth))

(comment
  (init! {:auth-type :profile
          :profile   :prod-core
          :region    "us-east-1"}))
