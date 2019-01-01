(ns sherlog.log.filter
  (:refer-clojure :exclude [list test])
  (:require
   [clojure.string :as str]
   [sherlog.log.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.logs.model
    DescribeMetricFiltersRequest
    PutMetricFilterRequest
    DeleteMetricFilterRequest
    MetricTransformation
    TestMetricFilterRequest]))

(defn- make-metric-transformation [namespace name value]
  (doto (MetricTransformation.)
    (.withMetricName name)
    (.withMetricValue value)
    (.withMetricNamespace namespace)))

(defn- as-metric [metric]
  {:log-group (.getLogGroupName metric)
   :created   (.getCreationTime metric)
   :pattern   (.getFilterPattern metric)
   :name      (.getFilterName metric)})

(defn- as-metric-filters [metrics]
  {:token   (.getNextToken metrics)
   :filters (map as-metric (.getMetricFilters metrics))})

(defn create [& {:keys [log-group name pattern namespace value]}]
  (let [metric-xf [(make-metric-transformation namespace name value)]]
    (->> (doto (PutMetricFilterRequest.)
           (.withLogGroupName log-group)
           (.withFilterName name)
           (.withFilterPattern pattern)
           (.withMetricTransformations metric-xf))
         (.putMetricFilter (get-client)))))

(defn delete [log-group name]
  (->> (doto (DeleteMetricFilterRequest.)
         (.withLogGroupName log-group)
         (.withFilterName name))
       (.deleteMetricFilter (get-client))))

(defn- find* [log-group token]
  (->> (doto (DescribeMetricFiltersRequest.)
         (.withLogGroupName log-group)
         (.withNextToken token))
       (.describeMetricFilters (get-client))
       (as-metric-filters)))

(defn list [log-group]
  (loop [{:keys [token filters]} (find* log-group nil)
           acc []]
      (if-not token
        (-> (conj acc filters)
            (flatten))
        (recur (find* log-group token)
               (conj acc filters)))))

(defn test [pattern messages]
  (println "Matching Pattern:" pattern)
  (->> (doto (TestMetricFilterRequest.)
         (.withLogEventMessages messages)
         (.withFilterPattern pattern))
       (.testMetricFilter (get-client))
       (.getMatches)
       (count)))
