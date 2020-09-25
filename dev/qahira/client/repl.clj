(ns qahira.client.repl
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as c]
   [com.stuartsierra.component.repl :as c.repl]
   [qahira.client.components.http-client :as qhr.clt.c.httpc]
   [qahira.client.edge.qahira-client :as qhr.clt.edge.qhrc]
   [qahira.systems :as qhr.sys]))

(defn make-system
  [profile]
  (let [source      (io/resource "qahira/client/config.edn")
        config      (aero/read-config source {:profile profile})
        system-kind (keyword "app" (name profile))]
    (-> (merge (qhr.sys/make-system config system-kind)
               (c/system-map
                 :qahira-client (qhr.clt.c.httpc/make-http-client
                                  (:qahira.client/qahira-client config))))
        (c/system-using
          {:qahira-client [:auth-token-encoder :api-token-encoder]}))))

(comment

  (c.repl/set-init (fn [_] (make-system :dev)))

  (c.repl/start)

  (c.repl/reset)

  (c.repl/stop)

  (qhr.clt.edge.qhrc/login-user
    (:qahira-client c.repl/system)
    "foobarquux"
    "foobarquux")

  (qhr.clt.edge.qhrc/request-token
    (:qahira-client c.repl/system)
    :reset
    "foobarquux")

  (let [client   (:qahira-client c.repl/system)
        response (qhr.clt.edge.qhrc/login-user client "foobarquux" "foobarquux")
        token    (-> response :body :token :auth)]
    (qhr.clt.edge.qhrc/read-auth-token client token))

  )
