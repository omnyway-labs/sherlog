(ns sherlog.main
  (:require
   [sherlog.core :as core]))

(defn -main [& args]
  (let [[cmd log-group pattern duration & opts] args]
    (core/init! {:auth-type :profile
                 :profile   (System/getenv "AWS_PROFILE")
                 :region    "us-east-1"})
    (condp = (keyword cmd)
      :tail
      (doseq [line (core/tail log-group)]
        (println line))

      :search
      (doseq [line (->> (read-string duration)
                        (core/search log-group pattern)
                        (flatten))]
        (println line)))))
