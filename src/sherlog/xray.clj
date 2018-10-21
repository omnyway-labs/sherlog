(ns sherlog.xray
  (:require
   [clojure.string :as str]
   [sherlog.cred :as cred]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.xray AWSXRayClientBuilder]
   [com.amazonaws.services.xray.model
    GetTraceSummariesRequest
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
  {:value (int (* 1000 (.getValue h)))
   :count (.getCount h)})

(defn as-service [s]
  (when-let [stat (u/ignore-errors (.getSummaryStatistics s))]
    (let [total (.getTotalCount stat)]
      {:started            (.getStartTime s)
       :name               (.getName s)
       :total              total
       :2xx                (.getOkCount stat)
       :4xx                (.. stat getErrorStatistics getTotalCount)
       :5xx                (.. stat getFaultStatistics getTotalCount)
       :response-time      (-> (/ (* 1000 (.getTotalResponseTime stat))
                                  total)
                               (Math/ceil)
                               (int))
       :response-histogram (->> (.getResponseTimeHistogram s)
                                (map as-histogram))})))

(defn as-graph-result [result]
  {:token    (.getNextToken result)
   :services (map as-service (.getServices result))})

(defn as-trace [t]
  {:id     (.getId t)
   :time   (int (Math/ceil (* (.getResponseTime t) 1000)))
   :url    (.. t getHttp getHttpURL)
   :status (.. t getHttp getHttpStatus)})

(defn as-trace-result [result]
  {:token    (.getNextToken result)
   :traces  (map as-trace (.getTraceSummaries result))})

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
        (->> (conj acc services)
             (flatten)
             (remove nil?))
        (recur (get-service-graph duration token)
               (conj acc services)))))

(defn list-traces* [duration pattern token]
  (let [{:keys [start end]} (u/time-boundary duration)]
    (->> (doto (GetTraceSummariesRequest.)
           (.withStartTime start)
           (.withEndTime   end)
           (.withNextToken token)
           (.withSampling false)
           (.withFilterExpression pattern))
         (.getTraceSummaries (get-client))
         (as-trace-result))))

(defn list-traces [duration pattern]
  (loop [{:keys [token traces]} (list-traces* duration nil)
           acc []]
      (if-not token
        (->> (conj acc traces)
             (flatten)
             (remove nil?))
        (recur (list-traces* duration pattern token)
               (conj acc traces)))))

(defn trace-seq []
  )


(defn search-trace []
  )

(defn init! [config]
  (cred/init! config)
  (reset! client (make-client (or (:region config) "us-east-1"))))
