(ns qahira.client.components.jwt-encoder
  (:require
   [orchid.components.jwt-encoder :as orc.c.jwt-enc]))

(defn make-sha-signer
  [config]
  (orc.c.jwt-enc/make-sha-signer config))

(defn make-asymmetric-signer
  [config]
  (orc.c.jwt-enc/make-asymmetric-signer config))

(defn make-jwt-encoder
  [config]
  (orc.c.jwt-enc/make-jwt-encoder config))
