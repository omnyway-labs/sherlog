(ns sherlog.metric.alarm
  (:require
   [sherlog.metric.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.cloudwatch.model
    Metric
    MetricStat
    Statistic
    DescribeAlarmsRequest
    PutMetricAlarmRequest
    ComparisonOperator
    Dimension
    SetAlarmStateRequest
    DisableAlarmActionsRequest
    DescribeAlarmHistoryRequest
    DeleteAlarmsRequest]))

(defn- as-operator [c]
  (condp = c
    ">=" (ComparisonOperator/valueOf "GreaterThanOrEqualToThreshold")
    ">"  (ComparisonOperator/valueOf "GreaterThanThreshold")
    "<=" (ComparisonOperator/valueOf "LessThanOrEqualToThreshold")
    "<"  (ComparisonOperator/valueOf "LessThanThreshold")))

(defn- as-statistic [s]
  (condp = s
    :avg   "Average"
    :sum   "Sum"
    :max   "Maximum"
    :total "Sum"
    nil "Average"))

(defn- as-missing [m]
  (condp = m
    :ignore        "ignore"
    :not-breaching "notBreaching"
    nil            "ignore"))

(defn as-state [state]
  (condp = state
    :alarm             "ALARM"
    :insufficient-data "INSUFFICIENT_DATA"
    :ok                "OK"))

(defn as-alarms [xs]
  {:token  (.getNextToken xs)
   :alarms (map (fn [a]
                  {:name      (.getAlarmName a)
                   :metric    (.getMetricName a)
                   :period    (.getPeriod a)
                   :statistic (.getStatistic a)
                   :threshold (.getThreshold a)
                   :actions   (map u/arn-name (.getAlarmActions a))
                   :missing   (.getTreatMissingData a)
                   :state     (.getStateValue a)})
                (.getMetricAlarms xs))})

(defn list-alarms* [token]
  (->> (doto (DescribeAlarmsRequest.)
         (.withNextToken token))
       (.describeAlarms (get-client))
       (as-alarms)))

(defn list-alarms []
  (loop [{:keys [token alarms]}  (list-alarms* nil)
         acc  []]
    (if-not token
      (conj acc alarms)
      (recur (list-alarms* token)
             (conj acc alarms)))))

(defn create [& {:keys [alarm-name namespace
                        metric-name operator
                        threshold
                        actions
                        period
                        statistic
                        treat-missing-data]}]
  (->> (doto (PutMetricAlarmRequest.)
         (.withAlarmName alarm-name)
         (.withMetricName metric-name)
         (.withComparisonOperator (as-operator operator))
         (.withPeriod period)
         (.withEvaluationPeriods (int 1))
         (.withActionsEnabled true)
         (.withStatistic (as-statistic statistic))
         (.withThreshold threshold)
         (.withAlarmActions actions)
         (.withTreatMissingData (as-missing treat-missing-data)))
       (.putMetricAlarmRequest (get-client))))

(defn delete [alarm-names]
  (->> (doto (DeleteAlarmsRequest.)
         (.withAlarmNames alarm-names))
       (.deleteAlarms (get-client))))

(defn set-state [alarm-name state]
  (->> (doto (SetAlarmStateRequest.)
         (.withAlaramName alarm-name)
         (.withStateValue (as-state state)))
       (.setAlarmState (get-client))))

(defn disable [alarm-name]
  (doto (DisableAlarmActionsRequest.)
    (.withAlarmNames [alarm-name])))

(defn enable [alarm-name]
  (doto (DisableAlarmActionsRequest.)
    (.withAlarmNames [alarm-name])))

(defn history [alarm-name]
  (->> (doto (DescribeAlarmHistoryRequest.)
         (.withAlarmName alarm-name)
         (.withHistoryItemType "StateUpdate")
         (.withStartDate "")
         (.withEndDate ""))
       (.describeAlarmHistory (get-client))))
