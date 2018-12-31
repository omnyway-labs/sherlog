(ns sherlog.log.client
  (:require
   [sherlog.cred :as cred])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.logs AWSLogsClientBuilder]))

(defonce client (atom nil))

(defn- make-client [region]
  (-> (AWSLogsClientBuilder/standard)
      (.withCredentials (cred/cred-provider))
      (.withRegion region)
      .build))

(defn get-client []
  @client)

(defn init! [config]
  (cred/init! config)
  (reset! client (make-client (or (:region config) "us-east-1"))))
