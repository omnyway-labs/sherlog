(ns sherlog.log-alarm-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [saw.core :as saw]
   [sherlog.util :as u]
   [sherlog.log :as log]
   [sherlog.log-alarm :as la]))

(def log-group "sherlog-test")
(def log-stream "test-stream")
(def metric-name "sherlog.a.count")
(def pagerduty (System/getenv "PAGERDUTY_ARN"))

(defn log-fixture [f]
  (try
    (la/init! (saw/session))
    (log/create-group log-group)
    (log/create-stream log-group log-stream)
    (f)
    (catch Exception e
      (println e))))

(use-fixtures :once log-fixture)

(deftest ^:integration create-log-alarm-test
  (println "running integration")
  (la/create {:log-group   log-group
              :alarm-name  metric-name
              :metric-name metric-name
              :filter      {:a "foo"}
              :trigger     '(>= avg 3.0)
              :actions     [pagerduty]})
  (is (= [metric-name]
         (->> (la/list log-group)
              (map :name))))
  (la/delete log-group))
