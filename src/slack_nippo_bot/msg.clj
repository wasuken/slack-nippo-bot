(ns slack-nippo-bot.msg
  (:require [clojure.data.json :as json]
            [slack-nippo-bot.util :refer :all]
            [slack-nippo-bot.db :refer :all]
            [clj-slack.chat :as chat]
            [gniazdo.core :as ws]))

(defn post-message [channel text]
  (prn 'chat (chat/post-message connection channel text)))

(defn parse-nippo-text [text channel user]
  (try
    (cond (= text "!!output!!")
          ;; ここで死んでるが、特にエラーはなし。
          (post-message channel (output-markdown user))
          (= text "!!help!!")
          (post-message channel "作成中")
          :else
          (let [split-texts (clojure.string/split text #" ")
                vanilla-text (clojure.string/join " "
                                                  (remove #(re-matches #"^\$\$.*" %) split-texts))
                pos-text (clojure.string/replace (or (first (filter #(re-matches #"^\$\$.*" %) split-texts))
                                                     "")
                                                 #"\$"
                                                 "")]
            (insert->section-sentence (clojure.string/split pos-text #"==") vanilla-text user)))
    (catch Exception e (prn 'err e))))

(defn parse-text [text]
  (let [json-msg (json/read-str text)
        main-type (get json-msg "type")
        sub-type (get json-msg "subtype")]
    (prn 'json json-msg)
    (when-not (= sub-type "bot_message")
      (cond (= main-type "message")
            (parse-nippo-text (get json-msg "text")
                              (get json-msg "channel")
                              (get json-msg "user"))))))
