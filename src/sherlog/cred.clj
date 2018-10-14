(ns sherlog.cred
  (:import
   [com.amazonaws.auth
    AWSCredentials
    BasicAWSCredentials
    BasicSessionCredentials
    AWSSessionCredentials
    AWSStaticCredentialsProvider
    InstanceProfileCredentialsProvider
    EnvironmentVariableCredentialsProvider
    DefaultAWSCredentialsProviderChain]
   [com.amazonaws.auth.profile ProfileCredentialsProvider]))

(def aws-creds (atom nil))

(defn cred-provider []
  @aws-creds)

(defn ^AWSCredentials
  static-credentials
  [{:keys [access-key secret-key session-token] :as creds}]
  (cond (and access-key secret-key session-token)
        (BasicSessionCredentials. access-key secret-key session-token)
        (and access-key secret-key)
        (BasicAWSCredentials. access-key secret-key)))

(defn get-creds []
  (let [creds    (.getCredentials @aws-creds)]
    {:access-key (.getAWSAccessKeyId creds)
     :secret-key (.getAWSSecretKey creds)
     :token      (if (instance? AWSSessionCredentials creds)
                   (.getSessionToken creds))}))

(def ^:private providers
  {:instance InstanceProfileCredentialsProvider
   :profile  ProfileCredentialsProvider
   :env      EnvironmentVariableCredentialsProvider
   :default   DefaultAWSCredentialsProviderChain})

(defn lookup-provider [auth-type]
  (->> (get providers auth-type)
       (.getName)))

(defn resolve-creds [{:keys [auth-type profile] :as config}]
  (condp = auth-type
    :instance (InstanceProfileCredentialsProvider. false)
    :profile  (ProfileCredentialsProvider. (name profile))
    :env      (EnvironmentVariableCredentialsProvider.)
    :static   (AWSStaticCredentialsProvider. (static-credentials config))
    :default  (DefaultAWSCredentialsProviderChain.)))

(defn init! [config]
  (reset! aws-creds (resolve-creds config)))
