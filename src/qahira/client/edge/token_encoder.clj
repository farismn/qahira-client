(ns qahira.client.edge.token-encoder
  (:refer-clojure :exclude [when-let])
  (:require
   [clj-time.core :as time]
   [orchid.components.jwt-encoder :as orc.c.jwt-enc]
   [taoensso.encore :as e :refer [when-let]]))

(defprotocol TokenEncoder
  (-encode [token-encoder claims])
  (-decode [token-encoder token]))

(defn make-token
  [token-encoder claims kind]
  (let [new-claims (-> claims (dissoc :password) (assoc :kind kind))]
    (-encode token-encoder new-claims)))

(defn read-token
  [token-encoder token kind]
  (when-let [payload (update (-decode token-encoder token) :kind e/as-?kw)
             _       (e/some= (:kind payload) (e/as-?kw kind))]
    payload))

(defn- assoc-time
  [claims duration]
  (let [iat (time/now)
        exp (when (some? duration)
              (time/plus iat (time/seconds duration)))]
    (e/assoc-when claims :iat iat :exp exp)))

(extend-protocol TokenEncoder
  orchid.components.jwt_encoder.SHASigner
  (-encode [token-encoder claims]
    (let [duration   (-> token-encoder :config :duration)
          new-claims (assoc-time claims duration)]
      (orc.c.jwt-enc/encode token-encoder new-claims)))
  (-decode [token-encoder token]
    (orc.c.jwt-enc/decode token-encoder token))

  orchid.components.jwt_encoder.AsymmetricSigner
  (-encode [token-encoder claims]
    (let [duration   (-> token-encoder :config :duration)
          new-claims (assoc-time claims duration)]
      (orc.c.jwt-enc/encode token-encoder new-claims)))
  (-decode [token-encoder token]
    (orc.c.jwt-enc/decode token-encoder token)))
