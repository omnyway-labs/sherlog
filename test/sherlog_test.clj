(ns sherlog-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [sherlog.util :as u]
   [sherlog.log :as log]
   [sherlog.metric :as metric]
   [sherlog.s3 :as s3]))

(def auth {:auth-type :profile :profile :prod-core})
(def test-bucket (System/getenv "S3_TEST_BUCKET"))

(deftest ^:integration select-test
  (s3/init! auth)
  (testing "Shallow query"
    (is (= {:id 1 :context {:request-id "Root-1=abc"}}
           (-> (s3/select test-bucket "test/" {:name "foo.bar"})
               (u/deserialize)
               (first)
               (select-keys [:id :context])))))

  (testing "Nested query"
    (is (= {:id 4 :context {:request-id "Root-1=xyz"}}
           (-> (s3/select test-bucket "test/"
                         {:context.request-id "Root-1=xyz"})
               (u/deserialize)
               (first)
               (select-keys [:id :context]))))))

(def log-group "sherlog-test")
(def log-stream "test-stream")

(defn log-fixture [f]
  (try
    (log/init! auth)
    (log/create-group log-group)
    (log/create-stream log-group log-stream)
    (f)
    (catch Exception e
      nil)
    (finally
      (log/delete-group log-group))))

(deftest ^:integration log-search-test
  (let [msg {:a    "foo"
             :b    "bar"
             :c    {:d "baz"}
             :rand (rand-int 10)}]
    (log-fixture
     (fn []
       (->> [(json/generate-string msg)]
            (log/put-events log-group log-stream))
       (Thread/sleep 4000)
       (is (= msg
              (-> (log/search log-group {:a "foo"} 5)
                  (flatten)
                  (u/deserialize)
                  (first))))))))

(deftest ^:integration log-filter-test
  (let [filter-name "sherlog-filter"]
    (log-fixture
     (fn []
       (log/create-filter :log-group log-group
                          :name      filter-name
                          :pattern   {:a "foo"}
                          :namespace "sherlog"
                          :value     "1")
       (let [filters (log/list-filters log-group)]
         (is (= 1 (count filters)))
         (is (= filter-name (-> filters first :name)))
         (log/delete-filter log-group filter-name)
         (is (= 0 (count (log/list-filters log-group)))))))))
