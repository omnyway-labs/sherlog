(ns sherlog.s3.client
  (:require
   [sherlog.cred :as cred])
  (:import
   [com.amazonaws.services.s3
    AmazonS3ClientBuilder]))

(defonce client (atom nil))

(defn get-client []
  @client)

(defn- make-client [region]
  (-> (AmazonS3ClientBuilder/standard)
      (.withCredentials (cred/cred-provider))
      (.withRegion region)
      .build))

(defn init! [{:keys [region] :as auth}]
  (let [region (or region "us-east-1")]
    (cred/init! auth)
    (reset! client (make-client region))))
