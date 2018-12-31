(ns sherlog.log.publisher
  (:require
   [clojure.string :as str]
   [sherlog.log.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.logs.model
    PutLogEventsRequest]))

(defn create-log-group [log-group-name]
  )

(defn put-event []
  )
