(ns sherlog.metric.client
  (:require
   [sherlog.cred :as cred])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.cloudwatch
    AmazonCloudWatchClientBuilder]))

(defonce client (atom nil))

(defn get-client []
  @client)

(defn make-client [region]
  (reset! client (-> (AmazonCloudWatchClientBuilder/standard)
                     (.withCredentials (cred/cred-provider))
                     (.withRegion region)
                     .build)))

(defn init! [config]
  (cred/init! config)
  (make-client (or (:region config) "us-east-1")))
