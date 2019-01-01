(ns sherlog.main
  (:require
   [clojure.pprint :as pprint]
   [sherlog.log :as log]
   [sherlog.s3 :as s3]
   [sherlog.metric :as metric]
   [sherlog.log-alarm :as la]))

(defn -main [& args]
  (let [[cmd log-group pattern duration & opts] args]
    (la/init! {:auth-type :profile
               :profile   (System/getenv "AWS_PROFILE")
               :region    "us-east-1"})
    (condp = (keyword cmd)
      :tail
      (doseq [line (log/tail log-group)]
        (println line))

      :search
      (doseq [line (->> (read-string duration)
                        (log/search log-group pattern)
                        (flatten))]
        (println line))

      :alarms
      (pprint/print-table
       (la/list log-group)))))
