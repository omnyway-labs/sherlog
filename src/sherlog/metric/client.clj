(ns sherlog.metric.client
  (:require
   [saw.core :as saw])
  (:import
   [com.amazonaws.regions Regions]
   [com.amazonaws.services.cloudwatch
    AmazonCloudWatchClientBuilder]))

(defonce client (atom nil))

(defn get-client []
  @client)

(defn make-client [region]
  (reset! client (-> (AmazonCloudWatchClientBuilder/standard)
                     (.withCredentials (saw/creds))
                     (.withRegion region)
                     .build)))

(defn init! [config]
  (saw/login config)
  (make-client (saw/region)))
