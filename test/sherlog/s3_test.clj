(ns sherlog.s3-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [sherlog.util :as u]
   [sherlog.s3 :as s3]))

(def auth {:auth-type :profile
           :profile (System/getenv "AWS_PROFILE")})
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
