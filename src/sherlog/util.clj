(ns sherlog.util
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as c])
  (:import
   [java.util Calendar TimeZone]
   [com.google.common.util.concurrent
    RateLimiter]))

(defn current-utc-ms []
  (-> (Calendar/getInstance (TimeZone/getTimeZone "UTC"))
      (.getTimeInMillis)))

(defn now-minus-secs [secs]
  (- (current-utc-ms) (* secs 1000)))

(defn start-time []
  (- (current-utc-ms) 20000))

(def rate-limiter (RateLimiter/create 1.0))

(defn rate-limit! []
  (.acquire rate-limiter))

(defn time-boundary [duration]
  (let [now (t/now)]
    {:end   (c/to-date now)
     :start (c/to-date (t/minus now (t/minutes duration)))}))

(defn omethods [obj]
  (map #(.getName %) (-> obj class .getMethods)))

(defmacro ignore-errors
  "Evaluate body and return `nil` if any exception occurs."
  [& body]
  `(try
     ~@body
     (catch Throwable e# nil)))
