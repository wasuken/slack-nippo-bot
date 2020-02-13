(ns slack-nippo-bot.util
  (:require [aero.core :refer [read-config]]))

(def config (read-config (clojure.java.io/resource "config.edn")))

(def connection {:api-url "https://slack.com/api" :token (:token config)})
