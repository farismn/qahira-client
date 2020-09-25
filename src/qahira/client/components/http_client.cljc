(ns qahira.client.components.http-client
  (:require
   [orchid.components.http-client :as orc.c.httpc]))

(defn make-http-client
  [config]
  (orc.c.httpc/make-http-client config))
