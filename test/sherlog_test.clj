(ns sherlog-test
  (:require
   [clojure.test :refer :all]
   [sherlog.core :as s]
   [sherlog.util :as u]))

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

(deftest ^:integration log-filter-test
  (s/init! auth :log)
  (s/init! auth :metric))
