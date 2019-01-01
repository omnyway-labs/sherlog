(ns sherlog.log.stream
  (:refer-clojure :exclude [list])
  (:require
   [clojure.string :as str]
   [sherlog.log.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.logs.model
    CreateLogGroupRequest
    DescribeLogGroupsRequest
    CreateLogStreamRequest
    DeleteLogGroupRequest
    DescribeLogStreamsRequest
    ResourceAlreadyExistsException
    OrderBy]))

(defn put-retention-policy []
  )

(defn as-stream [s]
  {:name                 (.getLogStreamName s)
   :arn                  (.getArn s)
   :last-event-timestamp (.getLastEventTimestamp s)})

(defn create-group [log-group]
  (try
    (->> (doto (CreateLogGroupRequest.)
           (.withLogGroupName log-group))
         (.createLogGroup (get-client)))
    (catch ResourceAlreadyExistsException e
      nil)))

(defn delete-group [log-group]
  (->> (doto (DeleteLogGroupRequest.)
         (.withLogGroupName log-group))
       (.deleteLogGroup (get-client))))

(defn create [log-group log-stream]
  (->> (doto (CreateLogStreamRequest.)
         (.withLogGroupName log-group)
         (.withLogStreamName log-stream))
       (.createLogStream (get-client))))

(defn latest [log-group]
  (->> (doto (DescribeLogStreamsRequest.)
        (.withOrderBy (OrderBy/valueOf "LastEventTime"))
        (.withDescending true)
        (.withLogGroupName log-group))
       (.describeLogStreams (get-client))
       (.getLogStreams)
       (first)
       (.getLogStreamName)))

(defn get-sequence-token [log-group log-stream]
  (->> (doto (DescribeLogStreamsRequest.)
         (.withLogGroupName log-group)
         (.withLogStreamNamePrefix log-stream))
       (.describeLogStreams (get-client))
       (.getLogStreams)
       (first)
       (.getUploadSequenceToken)))

(defn list-groups []
  (->> (DescribeLogGroupsRequest.)
       (.describeLogGroups (get-client))
       (.getLogGroups)
       (map #(.getLogGroupName %))))

(defn list [log-group]
  (->> (doto (DescribeLogStreamsRequest.)
         (.withLogGroupName log-group)
         (.withOrderBy "LastEventTime")
         (.withDescending true))
       (.describeLogStreams (get-client))
       (map as-stream)))
