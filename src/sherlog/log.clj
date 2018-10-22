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
    DescribeSubscriptionFiltersRequest
    GetLogEventsRequest
    OrderBy]))

(defonce client (atom nil))

(defn- make-client [region]
  (-> (AWSLogsClientBuilder/standard)
      (.withCredentials (cred/cred-provider))
      (.withRegion region)
      .build))

(defn- get-client []
  @client)

(defn- as-events [events]
  {:token (.getNextToken events)
   :events (map #(.getMessage %) (.getEvents events))})

(defn- as-log-events [events]
  {:token (.getNextForwardToken events)
   :events (map #(.getMessage %) (.getEvents events))})

(defn list-subscriptions [log-group]
  (->> (doto (DescribeSubscriptionFiltersRequest.)
         (.withLogGroupName log-group))
       (.describeSubscriptionFilters (get-client))))

(defn- get-log-events [log-group log-stream start-time token]
  (->> (doto (GetLogEventsRequest.)
          (.withLogStreamName log-stream)
          (.withLogGroupName log-group)
          (.withStartTime start-time)
          (.withNextToken token))
       (.getLogEvents (get-client))
       (as-log-events)))

(defn- filter-log [log-group pattern start-time token]
  (->> (doto (FilterLogEventsRequest.)
         (.withFilterPattern pattern)
         (.withLogGroupName log-group)
         (.withStartTime start-time)
         (.withNextToken token))
       (.filterLogEvents (get-client))
       (as-events)))

(defn- filter-log* [log-group log-streams pattern start-time token]
  (->> (doto (FilterLogEventsRequest.)
         (.withFilterPattern pattern)
         (.withLogGroupName log-group)
         (.withLogStreamNames log-streams)
         (.withStartTime start-time)
         (.withNextToken token)
         (.withInterleaved true))
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

(defn make-pattern [filters]
  (if (string? filters)
    filters
    (letfn [(as-form [[k v]]
              (format "($.%s = %s)" (name k) (pr-str v)))]
      (->> (map as-form filters)
           (interpose " && ")
           (apply str)
           (format "{%s}")))))

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

(defn search-group [log-group pattern duration]
  (let [start (u/now-minus-secs duration)
        pattern (make-pattern pattern)]
    (loop [{:keys [token events]} (filter-log log-group pattern start nil)
           acc []]
      (if-not token
        (conj acc events)
        (recur (filter-log log-group pattern start token)
               (conj acc events))))))

(defn search-streams [log-group streams pattern duration]
  (let [start (u/now-minus-secs duration)
        pattern (make-pattern pattern)]
     (loop [{:keys [token events]}
           (filter-log* log-group streams pattern start nil)
           acc []]
      (if-not token
        (conj acc events)
        (recur (filter-log* log-group streams pattern start token)
               (conj acc events))))))

(defn init! [config]
  (cred/init! config)
  (reset! client (make-client (or (:region config) "us-east-1"))))
