(ns sherlog.s3.select
  (:require
   [clojure.string :as str]
   [sherlog.s3.client :refer [get-client]]
   [sherlog.util :as u])
  (:import
   [com.amazonaws.services.s3.model
    JSONInput
    JSONOutput
    CompressionType
    ExpressionType
    InputSerialization
    OutputSerialization
    SelectObjectContentEvent
    SelectObjectContentEventVisitor
    SelectObjectContentRequest
    SelectObjectContentResult
    ListObjectsV2Request]))

(defn- make-json-input []
  (doto (JSONInput.)
    (.withType "LINES")))

(defn- make-json-output []
  (doto (JSONOutput.)
    (.withRecordDelimiter "\n")))

(defn- make-input-serialization []
  (doto (InputSerialization.)
    (.withJson (make-json-input))
    (.withCompressionType "GZIP")))

(defn- make-output-serialization []
  (doto (OutputSerialization.)
    (.withJson (make-json-output))))

(defn- make-object-content-request [bucket key expression]
  (doto (SelectObjectContentRequest.)
    (.withBucketName bucket)
    (.withKey key)
    (.withExpression expression)
    (.withExpressionType "SQL")
    (.withInputSerialization (make-input-serialization))
    (.withOutputSerialization (make-output-serialization))))

(defn- as-result [result]
  (->> (.getPayload result)
       (.getRecordsInputStream)))

(defn- as-key-path [path]
  (->> (str/split (name path) #"\.")
       (map pr-str)
       (str/join #".")))

(defn- map->where-clause [filters]
  (if (map? filters)
    (letfn [(as-form [[k v]]
              (format "s.%s = '%s'" (as-key-path k) v))]
      (->> (map as-form filters)
           (interpose " and ")
           (apply str)
           (format "%s")))
    filters))

(defn make-expression [m]
  (if (empty? m)
    "select * from S3Object s"
    (->> (map->where-clause m)
         (str "select * from S3Object s where "))))

(defn- as-keys [xs]
  {:token (.getNextContinuationToken xs)
   :keys  (map #(.getKey %) (.getObjectSummaries xs))})

(defn- list-keys* [bucket prefix token]
  (->> (doto (ListObjectsV2Request.)
         (.withBucketName bucket)
         (.withPrefix prefix)
         (.withContinuationToken token))
       (.listObjectsV2 (get-client))
       (as-keys)))

(defn list-keys [bucket prefix]
  (loop [{:keys [token keys]} (list-keys* bucket prefix nil)
         acc  []]
    (if-not token
      (flatten (conj acc keys))
      (recur (list-keys* bucket prefix token)
             (conj acc keys)))))

(defn- query-key* [bucket key filters]
  (->> (make-expression filters)
       (make-object-content-request bucket key)
       (.selectObjectContent (get-client))
       (as-result)))

(defn query [bucket prefix filters]
  (->> (list-keys bucket prefix)
       (map #(query-key* bucket % filters))))
