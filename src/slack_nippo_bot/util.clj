(ns slack-nippo-bot.util
  (:require [aero.core :refer [read-config]])
  (:gen-class))

(def config
  (if (.exist (clojure.java.io/file "config.edn"))
    (read-config (clojure.java.io/resource "config.edn"))))

(def connection {:api-url "https://slack.com/api" :token (:token config)})
