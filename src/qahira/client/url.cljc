(ns qahira.client.url
  (:require
   [taoensso.encore :as e]))

(defn anon-user
  []
  "/api/user")

(defn target-user
  [username]
  (str "/api/user/" username))

(defn reset-user
  [username]
  (str "/api/user/" username "/reset"))

(defn restore-user
  [username]
  (str "/api/user/" username "/restore"))

(defn target-token
  [kind username]
  (str "/api/token/" (e/as-name kind) "/" username))
