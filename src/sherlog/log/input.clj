(ns sherlog.log.input
  (:require
   [clojure.string :as str]
   [sherlog.log.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.logs.model
    CreateLogGroupRequest
    CreateLogStreamRequest
    DeleteLogGroupRequest
    PutLogEventsRequest
    InputLogEvent
    ResourceAlreadyExistsException]))

(defn create-log-group [log-group]
  (try
    (->> (doto (CreateLogGroupRequest.)
           (.withLogGroupName log-group))
         (.createLogGroup (get-client)))
    (catch ResourceAlreadyExistsException e
      nil)))

(defn delete-log-group [log-group]
  (->> (doto (DeleteLogGroupRequest.)
         (.withLogGroupName log-group))
       (.deleteLogGroup (get-client))))

(defn create-log-stream [log-group log-stream]
  (->> (doto (CreateLogStreamRequest.)
         (.withLogGroupName log-group)
         (.withLogStreamName log-stream))
       (.createLogStream (get-client))))

(defn put-event [log-group log-stream msg]
  (let [event (doto (InputLogEvent.)
                (.withMessage   msg)
                (.withTimestamp (System/currentTimeMillis)))]
    (->> (doto (PutLogEventsRequest.)
           (.withLogGroupName log-group)
           (.withLogStreamName log-stream)
           (.withLogEvents [event]))
         (.putLogEvents (get-client)))))
