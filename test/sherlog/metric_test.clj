(ns sherlog.metric-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [sherlog.util :as u]
   [sherlog.log :as log]
   [sherlog.metric :as metric]))

(def auth {:auth-type :profile
           :profile (System/getenv "AWS_PROFILE")})

(deftest basic-test
  (is (= 2 2)))
