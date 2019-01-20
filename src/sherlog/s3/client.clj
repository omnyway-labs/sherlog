(ns sherlog.s3.client
  (:require
   [saw.core :as saw])
  (:import
   [com.amazonaws.services.s3
    AmazonS3ClientBuilder]))

(defonce client (atom nil))

(defn get-client []
  @client)

(defn- make-client [region]
  (-> (AmazonS3ClientBuilder/standard)
      (.withCredentials (saw/creds))
      (.withRegion region)
      .build))

(defn init! [{:keys [region] :as auth}]
  (let [region (or region "us-east-1")]
    (saw/login auth)
    (reset! client (make-client region))))
