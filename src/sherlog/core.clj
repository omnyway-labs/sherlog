(ns sherlog.core
  (:require
   [sherlog.log :as log]
   [sherlog.xray :as xray]
   [sherlog.metric :as metric]))

(defn tail [log-group]
  (->> (log/latest-log-stream log-group)
       (log/log-seq log-group)))

(defn search [log-group pattern duration]
  (log/search log-group pattern duration))

(defn stats [duration]
  (xray/stats duration))

(defn trace [trace-id]
  )

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

(comment
  (show-metric "AWS/Billing" "EstimatedCharges" :currency "1d"))
