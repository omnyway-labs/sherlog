(ns sherlog.util
  (:require
   [clojure.string :as str]
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

(defmacro ->!
  "A variation on the threading macro which applies the same arg to a
  sequence of functions, e.g.,

  (->! 1
       prn                              ; prints 1
       #(prn (inc %1))                  ; prints 2
       (fn [x] (prn (inc x))))          ; also prints 2
  => 1"
  [x & forms]
  `(do
     ~@(map (fn [form]
              (if (and (seq? form)
                       (not (#{'fn* 'fn} (first form))))
                `(~(first form) ~x ~@(next form))
                `(~form ~x)))
            forms)
     ~x))

(defn arn-name [arn]
  (->> (str/split arn #":")
       (last)
       (keyword)))
