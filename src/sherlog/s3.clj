(ns sherlog.s3
  (:refer-clojure :exclude [select])
  (:require
   [sherlog.s3.client :as client]
   [sherlog.s3.select :refer [query]]
   [sherlog.s3.stream :as stream]))

(defn select
  ([s3-bucket prefix filters]
   (-> (query s3-bucket prefix filters)
       (stream/read-seq)))
  ([s3-bucket prefix filters out-file]
   (-> (query s3-bucket prefix filters)
       (stream/write-seq out-file))))

(defn init! [config]
  (client/init! config))
