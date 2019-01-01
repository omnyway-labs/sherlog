(ns sherlog.metric.namespace
  (:require
   [sherlog.metric.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.cloudwatch.model
    ListMetricsRequest
    ListMetricsResult]))

(defn- as-dimension [dim]
  {(.getName dim) (.getValue dim)})

(defn- as-metric [r]
  {:name (.getMetricName r)
   :dimensions (-> (map as-dimension (.getDimensions r))
                   (into {}))
   :namespace (.getNamespace r)})

(defn- as-metrics [result]
  {:next-token (.getNextToken result)
   :metrics    (map as-metric (.getMetrics result))})

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
      (-> (conj acc metrics)
          (flatten))
      (recur (list-metrics* namespace next-token)
             (conj acc metrics)))))

(defn list-metric-names [namespace]
  (->> (list-metrics namespace)
       (map :name)
       distinct
       sort))
