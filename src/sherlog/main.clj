(ns sherlog.main)

(defcommand
  ^{:alias "tail"
    :doc   "Tail Logs or service"
    :opts  [["-g"  "--log-group LogGroup"  "Cloudwatch LogGroup"]]}
  tail-command
  [{:keys [options]}]
  (let [{:keys [log-group]} options]
    (zam/init-cloud-service! :log)
    (zam/with-trace nil
      (doseq [e (zam-log/tail log-group)]
        (println e)))))

(defcommand
  ^{:alias "search"
    :doc   "Search logs"
    :opts [["-g" "--log-group LogGroup" "Log Group"]
           ["-d" "--duration Duration" "Duration in Secs"]
           ["-p" "--pattern PATTERN" "Filter Pattern"]
           ["-e" "--expression Expression" "S-expression or string"]]}
  search-command
  [{:keys [options]}]
  (let [{:keys [group pattern duration expression]} options
        duration (or (u/read-string-safely duration)
                     3600)
        pattern  (if expression
                   (format "\"%s\"" expression)
                   pattern)]
    (-> (zam-log/search group pattern duration)
        (flatten))))
