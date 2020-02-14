(ns slack-nippo-bot.db-test
  (:require [slack-nippo-bot.db :as sut]
            [clojure.java.shell :refer [sh]]
            [clojure.test :as t]))

(def db-path "./test.sqlite")

(def test-db {:connection-uri (str "jdbc:sqlite:" db-path)})

(defn setup
  []
  (spit db-path "")
  (sh "sqlite3" (str db-path " < " "create.sql")))

(defn cleanup
  []
  (clojure.java.io/delete-file db-path))

(t/deftest base-test
  (setup)
  (let [secs [{:id 1  :parent_id nil}
              {:id 2  :parent_id 1}
              {:id 3  :parent_id 1}
              {:id 4  :parent_id 1}
              {:id 5  :parent_id 1}
              {:id 6  :parent_id 1}]
        expected [{:id 1
                   :parent_id nil
                   :children [{:id 2  :parent_id 1}
                              {:id 3  :parent_id 1}
                              {:id 4  :parent_id 1}
                              {:id 5  :parent_id 1}
                              {:id 6  :parent_id 1}]}]]
    (t/is (= expected
             (sut/link-sections secs))))
  (cleanup))
