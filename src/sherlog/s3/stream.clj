(ns sherlog.s3.stream
  (:refer-clojure :exclude [read write])
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import
   [java.io
    File FileOutputStream
    InputStream OutputStream
    ByteArrayOutputStream]
   [com.amazonaws.util IOUtils]))

(defn- write-file* [input-stream filename]
  (->> (FileOutputStream. (File. filename))
       (IOUtils/copy input-stream)))

(defn write-seq [input-streams filename]
  (let [output-stream (FileOutputStream. (File. filename))]
    (if (coll? input-streams)
      (doseq [in input-streams]
        (IOUtils/copy in output-stream))
      (write-file* input-streams output-stream))))

(defn- read-stream* [input-stream]
  (with-open [in input-stream
              out (ByteArrayOutputStream.)]
    (IOUtils/copy in out)
    (-> (.toByteArray out)
        (String.)
        (str/trim)
        (str/split #"\n"))))

(defn read-seq [input-streams]
  (->> (map read-stream* input-streams)
       (flatten)
       (remove empty?)))
