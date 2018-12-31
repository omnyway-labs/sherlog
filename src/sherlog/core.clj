(ns sherlog.core
  (:refer-clojure :exclude [select])
  (:require
   [sherlog.log :as log]
   [sherlog.log.filter :as logf]
   [sherlog.metric :as metric]
   [sherlog.s3 :as s3]
   [sherlog.util :refer [->!]]))

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

(defn select
  ([s3-bucket prefix filters]
   (-> (s3/query s3-bucket prefix filters)
       (s3/read-streams)))
  ([s3-bucket prefix filters out-file]
   (-> (s3/query s3-bucket prefix filters)
       (s3/write-streams out-file))))

(defn list-filters [log-group]
  (logf/list-all log-group))

(defn delete-filter [log-group name]
  (logf/delete log-group name))

(defn create-filter [& {:keys [log-group name pattern
                               namespace value]}]
  (logf/create log-group name
               (log/make-pattern pattern)
               namespace
               value))

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

(defn list-alarms []
  (metric/list-alarms))

(defn init!
  ([auth]
   (log/init! auth)
   (metric/init! auth)
   (s3/init! auth))
  ([auth service]
   (condp = service
     :log    (log/init! auth)
     :metric (metric/init! auth)
     :s3     (s3/init! auth)
     nil)))
