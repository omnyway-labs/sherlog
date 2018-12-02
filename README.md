# sherlog

[![CircleCI](https://circleci.com/gh/omnyway-labs/sherlog.svg?style=svg)](https://circleci.com/gh/omnyway-labs/sherlog)

A Library to query logs in Cloudwatch and S3

Sherlog abstracts querying logs and creating realtime metrics from the
log streams

Sherlog provides APIs to

-   Query and Cloudwatch Log Streams
-   Filter and Fetch logs in S3 using AWS S3-select
-   Create Metric Filters on Cloudwatch Logs

Usage
=====

Initialize sherlog
------------------

``` {.clojure}
(require '[sherlog.core :as log])
(log/init! {:auth-type :profile
            :profile   (System/getenv "AWS_PROFILE")
            :region    "us-east-1"})
```

Querying Cloudwatch Logs
------------------------

``` {.clojure}
(log/search log-group PATTERN DURATION-IN-SECS)
```

Where PATTERN is a clojure map or a jq-like Pattern supported by
Cloudwatch

Examples:

``` {.clojure}
(log/search log-group "ERROR" 3000)
(log/search log-group "{ $.id = \"id123\" }" 3000)
;; search takes a map
(log/search log-group {:id "id123" :log-type "event"} 3000)

(log/tail log-group)
;; returns a log-seq
```

Grepping Logs in S3
-------------------

Sherlog uses s3-select to **grep** logs in S3. Supports logs in JSON
encoding.

``` {.clojure}
(log/grep s3-bucket prefix filters)
;; example
(log/grep "my-s3-bucket" "2018/11" {:log-type "event"})
```

To output the stream to a file

``` {.clojure}
(log/grep "my-bucket" "2018/11" {:token "some-token"} "/tmp/data.json")
```

Cloudwatch Metric and Subscription Filters
------------------------------------------

Sherlog supports both Metric and Subscription Filters. To list the
filters:

``` {.clojure}
(log/list-filters log-group type)
```

where type is **:metric** or **:subscription**

To create a Metric Filter, you could

``` {.clojure}
(log/create-filter log-group name pattern namespace initial-value)
;; example:
(log/create-filter :foo "my-metric" {:log-type "error"} "errors" 1)
;; this creates a realtime counter of errors in logs

(log/delete-filter log-group name)
```

``` {.clojure}
(log/find-metrics metric-namespace)
(log/show-metric "AWS/Billing" "EstimatedCharges" :currency "1d")
```

License - Apache 2.0
====================

Copyright 2018 Omnyway Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License. You may obtain
a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
