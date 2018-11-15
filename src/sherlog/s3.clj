(ns sherlog.s3
  (:require
   [sherlog.cred :as cred])
  (:import
   [com.amazonaws.services.s3
    AmazonS3 AmazonS3ClientBuilder]
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
    ListObjectsV2Request]
   [java.io
    File FileOutputStream
    InputStream OutputStream]
   [com.amazonaws.util IOUtils]))

(defonce client (atom nil))

(defn get-client []
  @client)

(defn make-client [region]
  (-> (AmazonS3ClientBuilder/standard)
      (.withCredentials (cred/cred-provider))
      (.withRegion region)
      .build))

(defn omethods [obj]
  (map #(.getName %) (-> obj class .getMethods)))

(defn make-json-input []
  (doto (JSONInput.)
    (.withType "LINES")))

(defn make-json-output []
  (doto (JSONOutput.)
    (.withRecordDelimiter "\n")))

(defn make-input-serialization []
  (doto (InputSerialization.)
    (.withJson (make-json-input))
    (.withCompressionType "NONE")))

(defn make-output-serialization []
  (doto (OutputSerialization.)
    (.withJson (make-json-output))))

(defn make-object-content-request [bucket key expression]
  (doto (SelectObjectContentRequest.)
    (.withBucketName bucket)
    (.withKey key)
    (.withExpression expression)
    (.withExpressionType "SQL")
    (.withInputSerialization (make-input-serialization))
    (.withOutputSerialization (make-output-serialization))))

(defn as-result [result]
  (->> (.getPayload result)
       (.getRecordsInputStream)))

(defn out-file [filename]
  (FileOutputStream. (File. filename)))

(defn map->where-clause [filters]
  (if (map? filters)
    (letfn [(as-form [[k v]]
              (format "s.%s = %s" (name k) (pr-str v)))]
      (->> (map as-form filters)
           (interpose " and ")
           (apply str)
           (format "%s")))
    filters))

(defn as-keys [xs]
  {:token (.getNextContinuationToken xs)
   :keys  (map #(.getKey %) (.getObjectSummaries xs))})

(defn list-keys* [bucket prefix token]
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

(defn make-expression [m]
  (if (empty? m)
    "select * from S3Object s"
    (->> (map->where-clause m)
         (str "select * from S3Object s where "))))

(defn query [bucket key filters]
  (->> (make-expression filters)
       (make-object-content-request bucket key)
       (.selectObjectContent (get-client))
       (as-result)))

(defn query-prefix [bucket prefix filters]
  (->> (list-keys bucket prefix)
       (map #(query bucket % filters))))

(defn write-file* [input-stream filename]
  (->> (FileOutputStream. (File. filename))
       (IOUtils/copy input-stream)))

(defn write-streams [input-streams filename]
  (let [output-stream (FileOutputStream. (File. filename))]
    (if (coll? input-streams)
      (doseq [in input-streams]
        (IOUtils/copy in output-stream))
      (write-file* input-streams output-stream))))

(defn init! [{:keys [region] :as auth}]
  (let [region (or region "us-east-1")]
    (cred/init! auth)
    (reset! client (make-client region))))
