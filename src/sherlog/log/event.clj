(ns sherlog.log.event
  (:require
   [sherlog.log.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.logs.model
    FilterLogEventsRequest
    DescribeLogStreamsRequest
    GetLogEventsRequest
    InputLogEvent
    PutLogEventsRequest]))

(defn- as-events [events]
  {:token (.getNextToken events)
   :events (map #(.getMessage %) (.getEvents events))})

(defn- as-log-events [events]
  {:token (.getNextForwardToken events)
   :events (map #(.getMessage %) (.getEvents events))})

(defn- get-log-events
  ([log-group log-stream start-time]
   (->> (doto (GetLogEventsRequest.)
         (.withLogStreamName log-stream)
         (.withLogGroupName log-group)
         (.withStartTime start-time)
         (.withStartFromHead false))
       (.getLogEvents (get-client))
       (as-log-events)))
  ([log-group log-stream start-time token]
   (->> (doto (GetLogEventsRequest.)
         (.withLogStreamName log-stream)
         (.withLogGroupName log-group)
         (.withNextToken token)
         (.withStartTime start-time)
         (.withStartFromHead false))
       (.getLogEvents (get-client))
       (as-log-events))))

(defn put! [log-group log-stream msg token]
  (let [event (doto (InputLogEvent.)
                (.withMessage   msg)
                (.withTimestamp (System/currentTimeMillis)))]
    (->> (doto (PutLogEventsRequest.)
           (.withLogGroupName log-group)
           (.withLogStreamName log-stream)
           (.withLogEvents [event])
           (.withSequenceToken token))
         (.putLogEvents (get-client)))))

(defn log-seq
  ([group stream]
   (let [{:keys [token events]}
         (get-log-events group stream (u/start-time))]
     (u/rate-limit!)
     (log-seq group stream events token)))
  ([group stream next-token]
   (let [{:keys [token events]}
         (get-log-events group stream nil next-token)]
     (u/rate-limit!)
     (log-seq group stream events token)))
  ([group stream events token]
   (lazy-seq (concat events (log-seq group stream token)))))

(defn- search*
  ([log-group pattern start-time token]
   (->> (doto (FilterLogEventsRequest.)
         (.withFilterPattern pattern)
         (.withLogGroupName log-group)
         (.withStartTime start-time)
         (.withNextToken token))
       (.filterLogEvents (get-client))
       (as-events)))
  ([log-group log-streams pattern start-time token]
   (->> (doto (FilterLogEventsRequest.)
          (.withFilterPattern pattern)
          (.withLogGroupName log-group)
          (.withLogStreamNames log-streams)
          (.withStartTime start-time)
          (.withNextToken token)
          (.withInterleaved true))
        (.filterLogEvents (get-client))
        (as-events))))

(defn search
  ([log-group pattern duration]
   (let [start (u/now-minus-secs duration)]
     (loop [{:keys [token events]}
            (search* log-group pattern start nil)
           acc []]
      (if-not token
        (conj acc events)
        (recur (search* log-group pattern start token)
               (conj acc events))))))
  ([log-group streams pattern duration]
   (let [start (u/now-minus-secs duration)]
     (loop [{:keys [token events]}
           (search* log-group streams pattern start nil)
            acc []]
      (if-not token
        (conj acc events)
        (recur (search* log-group streams pattern start token)
               (conj acc events)))))))
