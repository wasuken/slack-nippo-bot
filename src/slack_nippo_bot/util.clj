(ns slack-nippo-bot.util
  (:require [aero.core :refer [read-config]]
            [environ.core :refer [env]])
  (:gen-class))

(def config
  (if (.exists (clojure.java.io/file "config.edn"))
    (read-config (clojure.java.io/resource "config.edn"))))

(def connection {:api-url "https://slack.com/api" :token (env :token)})
