(ns slack-nippo-bot.msg
  (:require [clojure.data.json :as json]
            [slack-nippo-bot.util :refer :all]
            [slack-nippo-bot.db :refer :all]
            [clj-slack.chat :as chat]
            [gniazdo.core :as ws]
            [clj-http.client :as client]
            [environ.core :refer [env]])
  (:gen-class))

(defn post-message [channel text]
  (prn 'chat (chat/post-message connection channel text)))

(defn post-my-blog [sec-lst user]
  (let [o-md (output-markdown sec-lst user)
        title (subs (first (clojure.string/split o-md #"\n")) 2)]
    (client/post (env :posturl) {:form-params {:title title
                                               :body o-md
                                               :token (env :posttoken)
                                               :tags ["日報"]
                                               :type "md"}
                                 :form-param-encoding "UTF-8"})))

(defn parse-nippo-text [text channel user]
  (try
    (let [split-texts (clojure.string/split text #" ")
          vanilla-text (clojure.string/join " "
                                            (remove #(re-matches #"^\$\$.*" %) split-texts))
          pos-text (clojure.string/replace (or (first (filter #(re-matches #"^\$\$.*" %) split-texts))
                                               "")
                                           #"\$"
                                           "")
          pos-split (remove #(zero? (count %)) (clojure.string/split pos-text #"=="))]
      (cond (= vanilla-text "!!output!!")
            (post-message channel (all-output-markdown user))
            (= vanilla-text "!!help!!")
            (post-message channel "作成中")
            (= vanilla-text "!!delete!!")
            (tree->delete-db pos-split user)
            (= vanilla-text "!!post!!")
            (post-my-blog pos-split user)
            :else
            (insert->section-sentence pos-split vanilla-text user)))

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
