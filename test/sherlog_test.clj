(ns sherlog-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [sherlog.core :as s]
   [sherlog.util :as u]
   [sherlog.log.input :as in]))

(def auth {:auth-type :profile :profile :prod-core})
(def test-bucket (System/getenv "S3_TEST_BUCKET"))

(deftest ^:integration select-test
  (s/init! auth :s3)
  (testing "Shallow query"
    (is (= {:id 1 :context {:request-id "Root-1=abc"}}
           (-> (s/select test-bucket "test/" {:name "foo.bar"})
               (u/deserialize)
               (first)
               (select-keys [:id :context])))))

  (testing "Nested query"
    (is (= {:id 4 :context {:request-id "Root-1=xyz"}}
           (-> (s/select test-bucket "test/"
                         {:context.request-id "Root-1=xyz"})
               (u/deserialize)
               (first)
               (select-keys [:id :context]))))))

(deftest ^:integration log-search-test
  (s/init! auth :log)
  (let [log-group "sherlog-test"
        log-stream "test-stream"
        msg        {:a "foo"
                    :b "bar"
                    :c {:d "baz"}
                    :rand (rand-int 10)}]
    (in/create-log-group log-group)
    (in/create-log-stream log-group log-stream)
    (->> (json/generate-string msg)
         (in/put-event log-group log-stream))
    (Thread/sleep 4000)
    (is (= msg
           (-> (s/search log-group {:a "foo"} 5)
               (flatten)
               (u/deserialize)
               (first))))
    (in/delete-log-group log-group)))
