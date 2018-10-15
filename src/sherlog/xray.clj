(ns sherlog.xray
  (:require
   [clojure.string :as str]
   [sherlog.cred :as cred]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.xray AWSXRayClientBuilder]
   [com.amazonaws.services.xray.model
    GetTraceGraphRequest
    GetServiceGraphRequest]))

(defonce client (atom nil))

(defn make-client [region]
  (-> (AWSXRayClientBuilder/standard)
      (.withCredentials (cred/cred-provider))
      (.withRegion region)
      .build))

(defn get-client []
  @client)

(defn as-histogram [h]
  {:value (.getValue h)
   :count (.getCount h)})

(defn as-service [s]
  (u/ignore-errors
   (let [stat (.getSummaryStatistics s)]
     {:started            (.getStartTime s)
      :name               (.getName s)
      :total              (.getTotalCount stat)
      :ok                 (.getOkCount stat)
      :error              (.. stat getErrorStatistics getTotalCount)
      :response-time      (.getTotalResponseTime stat)
      :response-histogram (->> (.getResponseTimeHistogram s)
                               (map as-histogram))})))

(defn as-graph-result [result]
  {:token    (.getNextToken result)
   :services (map as-service (.getServices result))})

(defn get-service-graph [duration token]
  (let [{:keys [start end]} (u/time-boundary duration)]
    (->> (doto (GetServiceGraphRequest.)
           (.withStartTime start)
           (.withEndTime   end)
           (.withNextToken token))
         (.getServiceGraph (get-client))
         (as-graph-result))))

(defn stats [duration]
  (loop [{:keys [token services]} (get-service-graph duration nil)
           acc []]
      (if-not token
        (conj acc services)
        (recur (get-service-graph duration token)
               (conj acc services)))))

(defn init! [config]
  (cred/init! config)
  (reset! client (make-client (or (:region config) "us-east-1"))))
