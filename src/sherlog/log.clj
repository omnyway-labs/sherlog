(ns sherlog.log
  (:require
   [clojure.string :as str]
   [sherlog.util :as u]
   [sherlog.log.client :as client]
   [sherlog.log.filter :as filter]
   [sherlog.log.event :as event]
   [sherlog.log.stream :as stream]))

(defn make-pattern [filters]
  (if (map? filters)
    (letfn [(as-form [[k v]]
              (format "($.%s = %s)" (name k) (pr-str v)))]
      (->> (map as-form filters)
           (interpose " && ")
           (apply str)
           (format "{%s}")))
    filters))

(defn list-filters [log-group]
  (filter/list-all log-group))

(defn tail
  ([log-group]
   (let [stream (stream/latest log-group)]
     (event/log-seq log-group stream)))
  ([log-group log-stream]
   (event/log-seq log-group log-stream)))

(defn search
  ([log-group pattern duration]
   (let [pattern (make-pattern pattern)]
     (event/search log-group pattern duration)))
  ([log-group streams pattern duration]
   (let [pattern (make-pattern pattern)]
     (event/search log-group streams pattern duration))))

(defn put-event
  ([log-group log-stream msg]
   (->> (stream/get-sequence-token log-group log-stream)
        (event/put! log-group log-stream msg)))
  ([log-group log-stream msg token]
   (event/put! log-group log-stream msg token)))

(defn list-filters [log-group]
  (filter/list-all log-group))

(defn delete-filter [log-group filter-name]
  (filter/delete log-group filter-name))

(defn create-filter [& {:keys [log-group name pattern namespace value]}]
  (filter/create :log-group log-group
                 :name      name
                 :pattern   pattern
                 :namespace namespace
                 :value     value))

(defn create-group [log-group-name]
  (stream/create-group log-group-name))

(defn delete-group [log-group]
  (stream/delete-group log-group))

(defn list-groups []
  (stream/list-groups))

(defn create-stream [log-group log-stream]
  (stream/create log-group log-stream))

(defn list-streams [log-group]
  (stream/list log-group))

(defn init! [config]
  (client/init! config))
