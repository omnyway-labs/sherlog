(ns sherlog.metric.timeseries
  (:require
   [sherlog.metric.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
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
    GetMetricWidgetImageRequest
    MetricDataQuery
    Metric
    MetricStat
    Statistic]))

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

(defn- as-data-point [x]
  {:timestamp (.getTimestamp x)
   :avg       (format "%.2f" (.getAverage x))
   :max       (format "%.2f" (.getMaximum x))
   :min       (format "%.2f" (.getMinimum x))})

(defn- make-dimension [name value]
  (doto (Dimension.)
    (.withName name)
    (.withValue value)))

(defn- make-metric-request [namespace metric dimension duration]
  (let [{:keys [start period]} (u/estimate-period duration)
        {:keys [name value]}  (get dimensions dimension)]
    (if dimension
      (doto (GetMetricStatisticsRequest.)
        (.withNamespace namespace)
        (.withMetricName metric)
        (.withStatistics ["Average" "Maximum" "Minimum"])
        (.withDimensions [(make-dimension name value)])
        (.withStartTime start)
        (.withEndTime   (u/now))
        (.withPeriod    (int period)))
      (doto (GetMetricStatisticsRequest.)
        (.withNamespace namespace)
        (.withMetricName metric)
        (.withStatistics ["Average" "Maximum" "Minimum"])
        (.withStartTime start)
        (.withEndTime   (u/now))
        (.withPeriod    (int period))))))

(defn stats [namespace metric dimension duration]
  (->> (make-metric-request namespace metric dimension duration)
       (.getMetricStatistics (get-client))
       (.getDatapoints)
       (map as-data-point)))

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

(defn get-data [namespace metric start end]
  (->> (doto (GetMetricDataRequest.)
         (.withStartTime start)
         (.withEndTime end)
         (.withMetricDataQueries [(make-query namespace metric)]))
       (.getMetricData (get-client))
       (.getMetricDataResults)))

(defn get-image []
  (->> (doto (GetMetricWidgetImageRequest.)
         (.withMetricWidget "")
         (.withOutputFormat "png"))
       (.getMetricWidgetImage (get-client))))
