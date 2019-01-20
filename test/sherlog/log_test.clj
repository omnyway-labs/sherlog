(ns sherlog.log-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [saw.core :as saw]
   [sherlog.util :as u]
   [sherlog.log :as log]))

(def auth {:auth-type :profile
           :profile (System/getenv "AWS_PROFILE")})

(def log-group "sherlog-test")
(def log-stream "test-stream")

(defn log-fixture [f]
  (try
    (log/init! (saw/session))
    (log/create-group log-group)
    (log/create-stream log-group log-stream)
    (f)
    (catch Exception e
      (println e))))

(use-fixtures :once log-fixture)

(deftest ^:integration log-search-test
  (let [msg {:a    "foo"
             :b    "bar"
             :c    {:d "baz"}
             :rand (rand-int 10)}]
    (->> [(json/generate-string msg)]
         (log/put-events log-group log-stream))
    (Thread/sleep 4000)
    (is (= msg
           (-> (log/search log-group {:a "foo"} 5)
               (flatten)
               (u/deserialize)
               (first))))))

(deftest ^:integration log-filter-test
  (let [filter-name "sherlog-filter"]
    (log/create-filter :log-group log-group
                       :name      filter-name
                       :pattern   {:a "foo"}
                       :namespace "sherlog"
                       :value     "1")
    (let [filters (log/list-filters log-group)]
         (is (= 1 (count filters)))
         (is (= filter-name (-> filters first :name)))
         (log/delete-filter log-group filter-name)
         (is (= 0 (count (log/list-filters log-group)))))))


(defn make-msg [a]
  (json/generate-string {:a    a
                         :b    "bar"
                         :c    {:d "baz"}
                         :rand (rand-int 10)}))

(deftest ^:integration pattern-test
  (is (= 0
         (->> (map make-msg ["bar"])
              (log/test-filter {:a "foo"}))))
  (is (= 1
         (->> (map make-msg ["foo" "bar"])
              (log/test-filter {:a "foo"}))))
  (is (= 2
         (->> (map make-msg ["foo" "foo"])
              (log/test-filter {:a "foo"})))))
