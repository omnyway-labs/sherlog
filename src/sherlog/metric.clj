(ns sherlog.metric
  (:require
   [clojure.string :as str]
   [sherlog.cred :as cred]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.cloudwatch
    AmazonCloudWatchClientBuilder
    AmazonCloudWatch]
   [com.amazonaws.services.cloudwatch.model
    Dimension
    MetricDatum
    PutMetricDataRequest
    PutMetricDataResult
    StandardUnit
    ListMetricsRequest
    ListMetricsResult
    GetMetricStatisticsRequest
    GetMetricDataRequest
    MetricDataQuery
    Metric
    MetricStat
    Statistic
    DescribeAlarmsRequest
    PutMetricAlarmRequest
    ComparisonOperator
    Dimension]))

(defonce client (atom nil))

(defn make-client [region]
  (reset! client (-> (AmazonCloudWatchClientBuilder/standard)
                     (.withCredentials (cred/cred-provider))
                     (.withRegion region)
                     .build)))

(defn get-client []
  @client)

(def resolutions
  {:high (int 1)
   :low  (int 60)})

(def dimensions
  {:timer    {:name  "metricType"
              :value "timerStats"}
   :gauge    {:name  "metricType"
              :value "Gauge"}
   :currency {:name  "Currency"
              :value "USD"}})

(defn as-statistic [stat]
  (condp = stat
    :avg   "Average"
    :max   "Maxiumum"
    :min   "Minimum"
    :count "SampleCount"
    :sum   "Sum"))

(defn as-dimension [dim]
  {(.getName dim) (.getValue dim)})

(defn as-metric [r]
  {:name (.getMetricName r)
   :dimensions (-> (map as-dimension (.getDimensions r))
                   (into {}))
   :namespace (.getNamespace r)})

(defn as-metrics [result]
  {:next-token (.getNextToken result)
   :metrics    (map as-metric (.getMetrics result))})

(defn as-data-point [x]
  {:timestamp (.getTimestamp x)
   :avg       (format "%.2f" (.getAverage x))
   :max       (format "%.2f" (.getMaximum x))
   :min       (format "%.2f" (.getMinimum x))})

(defn make-dimension [name value]
  (doto (Dimension.)
    (.withName name)
    (.withValue value)))

(defn get-stats [namespace metric dimension duration]
  (let [{:keys [start period]} (u/estimate-period duration)
        {:keys [name value]}   (get dimensions dimension dimension)]
    (->> (doto (GetMetricStatisticsRequest.)
           (.withNamespace namespace)
           (.withMetricName metric)
           (.withStatistics ["Average" "Maximum" "Minimum"])
           (.withDimensions [(make-dimension name value)])
           (.withStartTime start)
           (.withEndTime   (u/now))
           (.withPeriod    (int period)))
         (.getMetricStatistics (get-client))
         (.getDatapoints)
         (map as-data-point))))

(defn- make-query [namespace metric]
  (let [metric-stat (doto (MetricStat.)
                      (.withMetric (doto (Metric.)
                                     (.withNamespace namespace)
                                     (.withName     metric)))
                      (.withPeriod (int 300))
                      (.withStat "Sum"))]
    (doto (MetricDataQuery.)
      (.withId "test")
      (.withMetricStat metric-stat))))

(defn get-metric-data [namespace metric start end]
  (->> (doto (GetMetricDataRequest.)
         (.withStartTime start)
         (.withEndTime end)
         (.withMetricDataQueries [(make-query namespace metric)]))
       (.getMetricData (get-client))
       (.getMetricDataResults)))

(defn list-metrics* [namespace token]
  (->> (doto (ListMetricsRequest.)
         (.withNamespace namespace)
         (.withNextToken token))
       (.listMetrics (get-client))
       (as-metrics)))

(defn list-metrics [namespace]
  (loop [{:keys [next-token metrics]}  (list-metrics* namespace nil)
         acc  []]
    (if-not next-token
      (conj acc metrics)
      (recur (list-metrics* namespace next-token)
             (conj acc metrics)))))

(defn list-all [namespace]
  (->> (list-metrics namespace)
       flatten
       (map :name)
       distinct
       sort))

(defn as-operator [c]
  (condp = c
    ">=" (ComparisonOperator/valueOf "GreaterThanOrEqualToThreshold")
    ">"  (ComparisonOperator/valueOf "GreaterThanThreshold")
    "<=" (ComparisonOperator/valueOf "LessThanOrEqualToThreshold")
    "<"  (ComparisonOperator/valueOf "LessThanThreshold")))

(defn as-alarms [xs]
  {:token  (.getNextToken xs)
   :alarms (map (fn [a]
                  {:name      (.getAlarmName a)
                   :metric    (.getMetricName a)
                   :period    (.getPeriod a)
                   :statistic (.getStatistic a)
                   :threshold (.getThreshold a)
                   :actions   (map u/arn-name (.getAlarmActions a))
                   :missing   (.getTreatMissingData a)})
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

(defn create-alarm [{:keys [alarm-name namespace
                            metric-name operator
                            threshold
                            action period]}]
  (->> (doto (PutMetricAlarmRequest.)
         (.withAlarmName alarm-name)
         (.withComparisonOperator (as-operator operator))
         (.withMetricName metric-name)
         (.withPeriod period)
         (.withEvaluationPeriods (int 1))
         (.withActionsEnabled true)
         (.withStatistic "Average")
         (.withThreshold threshold)
         (.withAlarmActions action)
         (.withTreatMissingData "ignore"))
       (.putMetricAlarmRequest (get-client))))

(defn init! [config]
  (cred/init! config)
  (make-client (or (:region config) "us-east-1")))

(comment
  (init! {:auth-type :profile
          :profile :prod-core
          :region "us-east-1"}))
