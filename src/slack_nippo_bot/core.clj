(ns slack-nippo-bot.core
  (:require [clj-slack.rtm :as rtm]
            [slack-nippo-bot.util :refer :all]
            [slack-nippo-bot.msg :refer :all]
            [gniazdo.core :as ws])
  (:gen-class))

(defn -main
  []
  (ws/connect
      (:url (rtm/connect connection))
    :on-receive #(parse-text %)))
