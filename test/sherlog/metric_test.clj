(ns sherlog.metric-test
  (:require
   [clojure.test :refer :all]
   [cheshire.core :as json]
   [sherlog.util :as u]
   [sherlog.log :as log]
   [sherlog.metric :as metric]))

(deftest basic-test
  (is (= 2 2)))
