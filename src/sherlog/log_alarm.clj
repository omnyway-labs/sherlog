(ns sherlog.log-alarm
  (:refer-clojure :exclude [list])
  (:require
   [clojure.set :as set]
   [sherlog.metric :as metric]
   [sherlog.log :as log]
   [sherlog.metric.client :as metric-client]
   [sherlog.log.client :as log-client]))

(defn list [log-group]
  (let [filters (log/list-filters log-group)
        alarms   (->> (metric/list-alarms)
                      (filter #(= (:namespace %) log-group)))]
    (into [] (set/join filters alarms {:name :metric}))))

(defn- parse-trigger [trigger]
  (let [[op stat threshold] trigger]
    {:operator  (name op)
     :statistic (keyword stat)
     :threshold threshold}))

(defn create [{:keys [log-group
                      metric-name
                      alarm-name
                      filter trigger
                      actions
                      period
                      missing-data]}]
  (log/create-filter :log-group log-group
                     :name      (name metric-name)
                     :pattern   filter
                     :namespace log-group
                     :value     "1")
  (let [{:keys [operator statistic threshold]} (parse-trigger trigger)]
    (metric/create-alarm  :alarm-name (name alarm-name)
                          :metric-name metric-name
                          :namespace  log-group
                          :operator   operator
                          :threshold  threshold
                          :actions    actions
                          :period     (or period 60)
                          :statistic  statistic
                          :missing-data (or missing-data
                                            :not-breaching))))

(defn delete [log-group]
  (let [filters (log/list-filters log-group)]
    (->> (map :name filters)
         (metric/delete-alarms))
    (doseq [{:keys [name]} filters]
      (log/delete-filter log-group name))))

(defn init! [config]
  (metric-client/init! config)
  (log-client/init! config))
