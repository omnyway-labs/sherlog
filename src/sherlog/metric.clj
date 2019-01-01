(ns sherlog.metric
  (:refer-clojure :exclude [find])
  (:require
   [sherlog.metric.client :as client]
   [sherlog.metric.timeseries :as ts]
   [sherlog.metric.alarm :as alarm]
   [sherlog.metric.namespace :as namespace]
   [sherlog.log :as log]))

(defn find
  ([namespace]
   (namespace/list-metrics namespace))
  ([namespace pattern]
   (->> (namespace/list-metrics namespace)
        (map #(re-matches (re-pattern (str pattern ".*")) %))
        (remove nil?))))

(defn stats [namespace metric dimension duration]
  (->> (ts/stats namespace metric dimension duration)
       (sort-by :timestamp)
       (reverse)))

(defn get-data [namespace metric start end]
  (ts/get-data namespace metric start end))

(def list-alarms #'alarm/list)

(def create-alarm #'alarm/create)

(defn delete-alarm [alarm-name]
  (alarm/delete [alarm-name]))

(def disable-alarm #'alarm/disable)

(def enable-alarm #'alarm/enable)

(def alarm-history #'alarm/history)

(defn create-metric-alarm [& {:keys [log-group
                                     metric-name
                                     pattern
                                     operator
                                     threshold
                                     actions
                                     period
                                     statistic
                                     missing-data]}]
  (log/create-filter :log-group log-group
                     :name      (name metric-name)
                     :pattern   pattern
                     :namespace log-group
                     :value     "1")
  #_(alarm/create  :alarm-name (name metric-name)
                 :namespace  log-group
                 :operator   operator
                 :threshold  threshold
                 :actions    actions
                 :period     period
                 :statistic  statistic
                 :missing-data missing-data))

(defn list-metric-alrams [log-group]
  (let [metrics (namespace/list-metrics namespace)]
    ))

(defn init! [config]
  (client/init! config))
