(ns sherlog.log.client
  (:require
   [saw.core :as saw])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.logs AWSLogsClientBuilder]))

(defonce client (atom nil))

(defn- make-client [region]
  (-> (AWSLogsClientBuilder/standard)
      (.withCredentials (saw/creds))
      (.withRegion region)
      .build))

(defn get-client []
  @client)

(defn init! [config]
  (saw/login config)
  (reset! client (make-client (or (:region config) "us-east-1"))))
