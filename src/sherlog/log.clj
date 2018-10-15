(ns sherlog.log
  (:require
   [clojure.string :as str]
   [sherlog.cred :as cred]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.logs AWSLogsClientBuilder]
   [com.amazonaws.services.logs.model
    FilterLogEventsRequest
    DescribeLogStreamsRequest
    GetLogEventsRequest
    OrderBy]))

(defonce client (atom nil))

(defn make-client [region]
  (-> (AWSLogsClientBuilder/standard)
      (.withCredentials (cred/cred-provider))
      (.withRegion region)
      .build))

(defn get-client []
  @client)

(defn as-events [events]
  {:token (.getNextToken events)
   :events (map #(.getMessage %) (.getEvents events))})

(defn as-log-events [events]
  {:token (.getNextForwardToken events)
   :events (map #(.getMessage %) (.getEvents events))})

(defn get-log-events [log-group log-stream start-time token]
  (->> (doto (GetLogEventsRequest.)
          (.withLogStreamName log-stream)
          (.withLogGroupName log-group)
          (.withStartTime start-time)
          (.withNextToken token))
       (.getLogEvents (get-client))
       (as-log-events)))

(defn filter-log [log-group pattern start-time token]
  (->> (doto (FilterLogEventsRequest.)
         (.withFilterPattern pattern)
         (.withLogGroupName log-group)
         (.withStartTime start-time)
         (.withNextToken token))
       (.filterLogEvents (get-client))
       (as-events)))

(defn latest-log-stream [log-group]
  (->> (doto (DescribeLogStreamsRequest.)
        (.withOrderBy (OrderBy/valueOf "LastEventTime"))
        (.withDescending true)
        (.withLogGroupName log-group))
       (.describeLogStreams (get-client))
       (.getLogStreams)
       (first)
       (.getLogStreamName)))

(defn log-seq
  "Returns a lazy sequence of log events from Cloudwatch"
  ([group stream]
   (let [{:keys [token events]}
         (get-log-events group stream (u/start-time) nil)]
     (u/rate-limit!)
     (log-seq group stream events token)))
  ([group stream next-token]
   (let [{:keys [token events]}
         (get-log-events group stream nil next-token)]
     (u/rate-limit!)
     (log-seq group stream events token)))
  ([group stream events token]
   (lazy-seq (concat events (log-seq group stream token)))))

(defn search [log-group pattern duration]
  (let [start (u/now-minus-secs duration)]
    (loop [{:keys [token events]} (filter-log log-group pattern start nil)
           acc []]
      (if-not token
        (conj acc events)
        (recur (filter-log log-group pattern start token)
               (conj acc events))))))

(defn init! [config]
  (cred/init! config)
  (reset! client (make-client (or (:region config) "us-east-1"))))
