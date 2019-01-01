(ns sherlog.metric
  (:require
   [sherlog.metric.client :as client]
   [sherlog.metric.data :as data]
   [sherlog.metric.alarm :as alarm]))

(defn find-metrics
  ([namespace]
   (data/list-all namespace))
  ([namespace pattern]
   (->> (data/list-all namespace)
        (map #(re-matches (re-pattern (str pattern ".*")) %))
        (remove nil?))))

(defn show [namespace metric dimension duration]
  (->> (data/get-stats namespace metric dimension duration)
       (sort-by :timestamp)
       (reverse)))

(defn list-alarms []
  (alarm/list-alarms))

(defn create-alarm []
  )

(defn delete-alarm []
  )

(defn disable-alarm []
  )



(defn init! [config]
  (client/init! config))
