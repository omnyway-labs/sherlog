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
        alarms  (metric/describe-alarms (map :name filters))]
    (into [] (set/join filters alarms {:name :name}))))

(defn create [& {:keys [log-group metric-name
                        pattern operator
                        threshold actions
                        period statistic
                        missing-data]}]
  (log/create-filter :log-group log-group
                     :name      (name metric-name)
                     :pattern   pattern
                     :namespace log-group
                     :value     "1")
  (metric/create-alarm  :alarm-name (name metric-name)
                        :metric-name metric-name
                        :namespace  log-group
                        :operator   operator
                        :threshold  threshold
                        :actions    actions
                        :period     period
                        :statistic  statistic
                        :missing-data missing-data))

(defn delete [log-group]
  (let [filters (log/list-filters log-group)]
    (metric/delete-alarm (map :name filters))
    (doseq [{:keys [name]} filters]
      (log/delete-filter log-group name))))

(defn init! [config]
  (metric-client/init! config)
  (log-client/init! config))
