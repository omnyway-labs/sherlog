(ns sherlog.log
  (:require
   [clojure.string :as str]
   [sherlog.cred :as cred])
  (:import
   [java.util Calendar TimeZone]
   [com.google.common.util.concurrent
    RateLimiter]
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.logs AWSLogsClient]
   [com.amazonaws.services.logs.model
    FilterLogEventsRequest
    DescribeLogStreamsRequest
    GetLogEventsRequest
    OrderBy]))

(defonce client (atom nil))

(defn make-client [region]
  (reset! client
          (-> (AWSLogsClient. (cred/cred-provider))
              (.withRegion (Regions/fromName region)))))

(defn get-client []
  @client)

(defn current-utc-ms []
  (-> (Calendar/getInstance (TimeZone/getTimeZone "UTC"))
      (.getTimeInMillis)))

(defn start-time []
  (- (current-utc-ms) 20000))

(defn now-minus-secs [secs]
  (- (current-utc-ms) (* secs 1000)))

(def rate-limiter (RateLimiter/create 1.0))

(defn as-events [x]
  {:token (.getNextToken x)
   :events (map #(.getMessage %) (.getEvents x))})

(defn as-log-events [x]
  {:token (.getNextForwardToken x)
   :events (map #(.getMessage %) (.getEvents x))})

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
  (-> (doto (DescribeLogStreamsRequest.)
        (.withOrderBy (OrderBy/valueOf "LastEventTime"))
        (.withDescending true)
        (.withLogGroupName log-group))
      (first)
      :name))

(defn log-seq
  "Returns a lazy sequence of log events from Cloudwatch"
  ([group stream]
   (let [{:keys [token events]}
         (get-log-events group stream (start-time))]
     (.acquire rate-limiter)
     (log-seq group stream events token)))
  ([group stream next-token]
   (let [{:keys [token events]}
         (get-log-events group stream nil next-token)]
     (.acquire rate-limiter)
     (log-seq group stream events token)))
  ([group stream events token]
   (lazy-seq (concat events (log-seq group stream token)))))

(defn search [log-group pattern duration]
  (let [start (now-minus-secs duration)]
    (loop [{:keys [token events]} (filter-log log-group pattern start nil)
           acc []]
      (if-not token
        (conj acc events)
        (recur (filter-log log-group pattern start token)
               (conj acc events))))))

(defn init! [config]
  (cred/init! config)
  (make-client (or (:region config) "us-east-1")))
