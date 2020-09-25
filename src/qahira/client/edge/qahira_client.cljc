(ns qahira.client.edge.qahira-client
  (:require
   [orchid.components.http-client :as orc.c.httpc]
   #?(:clj [qahira.client.edge.token-encoder :as qhr.c.edge.token-enc])
   [qahira.client.url :as qhr.c.url]
   [taoensso.encore :as e]))

(defprotocol QahiraClient
  (request-condition [qahira-client]))

(defprotocol UserClient
  (register-user [user-client new-user])
  (login-user [user-client username password])
  (update-user-password [user-client username auth-token new-password old-password])
  (reset-user-password [user-client username reset-token new-password])
  (deactivate-user [user-client username auth-token])
  (restore-user [user-client username restore-token]))

#?(:clj (defprotocol AppClient
          (request-token [app-client kind username])
          (read-auth-token [app-client auth-token])))

(defn- wrap-basic-auth
  [request]
  #?(:clj (let [extract (juxt :username :password)]
            (e/update-in request [:basic-auth] {} (fn [auth]
                                                    (if (empty? auth)
                                                      :swap/dissoc
                                                      (extract auth)))))
     :cljs request))

(defn- assoc-authz-token
  [request scheme token]
  (assoc-in request [:headers "authorization"] (str scheme " " token)))

(defn- wrap-qahira-token-auth
  [request]
  (if-let [token (:qahira-token-auth request)]
    (-> request
        (dissoc :qahira-token-auth)
        (assoc-authz-token "QahiraToken" token))
    request))

(defn- wrap-params
  [request config]
  #?(:clj (let [params       (:params request)
                content-type (:content-type config)]
            (-> request
                (dissoc :params)
                (e/assoc-when :accept content-type
                              :as content-type
                              :form-params (when (some? params)
                                             params)
                              :content-type (when (some? params)
                                              content-type))))
     :cljs (let [params    (:params request)
                 target-kw (case (:content-type config)
                             :transit+json :transit-params
                             :json         :json-params
                             nil)]
             (-> request
                 (dissoc :params)
                 (e/assoc-when target-kw (when (some? target-kw)
                                           params))))))

(defn- run-request
  [client http-method uri request]
  (orc.c.httpc/request client http-method uri (-> request
                                                  (wrap-params (:config client))
                                                  (wrap-basic-auth)
                                                  (wrap-qahira-token-auth))))

(extend-protocol UserClient
  orchid.components.http_client.HttpClient
  (register-user [user-client new-user]
    (let [uri     (qhr.c.url/anon-user)
          request {:params {:user new-user}}]
      (run-request user-client :post uri request)))
  (login-user [user-client username password]
    (let [uri     (qhr.c.url/target-user username)
          request {:basic-auth {:username username
                                :password password}}]
      (run-request user-client :post uri request)))
  (update-user-password [user-client username auth-token new-password old-password]
    (let [uri     (qhr.c.url/target-user username)
          request {:qahira-token-auth auth-token
                   :params            {:user {:new-password new-password
                                              :old-password old-password}}}]
      (run-request user-client :put uri request)))
  (reset-user-password [user-client username reset-token new-password]
    (let [uri     (qhr.c.url/reset-user username)
          request {:qahira-token-auth reset-token
                   :params            {:user {:new-password new-password}}}]
      (run-request user-client :put uri request)))
  (deactivate-user [user-client username auth-token]
    (let [uri     (qhr.c.url/target-user username)
          request {:qahira-token-auth auth-token}]
      (run-request user-client :delete uri request)))
  (restore-user [user-client username restore-token]
    (let [uri     (qhr.c.url/reset-user username)
          request {:qahira-token-auth restore-token}]
      (run-request user-client :post uri request))))

#?(:clj (extend-protocol AppClient
          orchid.components.http_client.HttpClient
          (request-token [app-client kind username]
            (let [uri           (qhr.c.url/target-token kind username)
                  token-encoder (:api-token-encoder app-client)
                  claims        {:username username}
                  token         (qhr.c.edge.token-enc/make-token token-encoder claims :api)
                  request       {:qahira-token-auth token}]
              (run-request app-client :get uri request)))
          (read-auth-token [app-client auth-token]
            (let [token-encoder (:auth-token-encoder app-client)]
              (qhr.c.edge.token-enc/read-token token-encoder auth-token :auth)))))

(extend-protocol QahiraClient
  orchid.components.http_client.HttpClient
  (request-condition [qahira-client]
    (let [uri (qhr.c.url/anon-meta)]
      (run-request qahira-client :get uri {}))))
