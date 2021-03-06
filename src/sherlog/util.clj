(ns sherlog.util
  (:require
   [clojure.string :as str]
   [cheshire.core :as json]
   [clj-time.core :as t]
   [clj-time.coerce :as c])
  (:import
   [java.util Calendar TimeZone Date]
   [com.google.common.util.concurrent
    RateLimiter]))

(defn current-utc-ms []
  (-> (Calendar/getInstance (TimeZone/getTimeZone "UTC"))
      (.getTimeInMillis)))

(defn now-minus-secs [secs]
  (- (current-utc-ms) (* secs 1000)))

(defn start-time []
  (- (current-utc-ms) 20000))

(def rate-limiter (RateLimiter/create 0.7))

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

(defn now []
  (Date.))

(defn estimate-period [duration]
  (let [start-time   (fn [duration-secs]
                       (Date. (- (.getTime (Date.))
                                 (* duration-secs 1000))))
        mapping {"1h" {:period 60 :start (start-time (* 60 60))}
                 "6h" {:period 120 :start (start-time (* 6 60 60))}
                 "1d" {:period 300 :start (start-time (* 24 60 60))}
                 "1w" {:period 600 :start (start-time (* 7 24 60 60))}
                 "2w" {:period 900 :start (start-time (* 2 7 24 60 60))}}]
    (get mapping (or duration "1d"))))

(defn ms->datetime [ms]
  (c/from-long ms))

(defn arn-name [arn]
  (->> (str/split arn #":")
       (last)
       (keyword)))

(defn deserialize [xs]
  (map #(json/parse-string % true) xs))

(defn is-lazy? [x]
  (instance? clojure.lang.LazySeq x))
