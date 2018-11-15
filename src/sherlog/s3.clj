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
    SelectObjectContentResult]
   [java.io
    File FileOutputStream
    InputStream OutputStream]
   [com.amazonaws.util IOUtils]))

(defonce client (atom nil))

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

(defn make-expression [m]
  (->> (map->where-clause m)
       (str "select * from S3Object s where ")))

(defn query [bucket key filters]
  (->> (make-expression filters)
       (make-object-content-request bucket key)
       (.selectObjectContent @client)
       (as-result)))

(defn query-and-write [bucket key filters filename]
  (-> (query bucket key filters)
      (IOUtils/copy (out-file filename))))

(defn init! [{:keys [region] :as auth}]
  (let [region (or region "us-east-1")]
    (cred/init! auth)
    (reset! client (make-client region))))
