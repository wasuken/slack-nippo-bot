(ns slack-nippo-bot.db-test
  (:require [slack-nippo-bot.db :as sut]
            [clojure.java.shell :refer [sh]]
            [clojure.java.jdbc :as j]
            [clojure.test :as t]))

(def db-path "./test.sqlite")

(def test-db {:connection-uri (str "jdbc:sqlite:" db-path)})

(defn setup
  []
  (when (.exists (clojure.java.io/file db-path))
    (clojure.java.io/delete-file db-path))
  (spit db-path "")
  (doseq [create (clojure.string/split (slurp "./create.sql") #";")]
    (when-not (= "" (clojure.string/trim create))
      (j/db-do-commands test-db create))))

(defn cleanup
  []
  (clojure.java.io/delete-file db-path)
  )

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

(defn hashmap-left-keys-eq [exp act]
  (reduce (fn [res key] (if (= (get exp key)
                               (get act key))
                          true
                          (reduced false)))
          true
          (keys exp)))

(defn lst-hashmap-left-keys-eq [exp-lst act-lst]
  (and
   (= (count exp-lst) (count act-lst))
   (hashmap-left-keys-eq (first exp-lst) (first act-lst))
   (if-not (zero? (count (drop 1 exp-lst)))
     (lst-hashmap-left-keys-eq (drop 1 exp-lst) (drop 1 act-lst))
     true)))

(t/deftest input-test
  (setup)
  (let [test-case "これはテストです $hoge==fuga"
        sentences-exp [{:id 1 :value "これはテストです"}]
        section-exp [{:id 1 :name "hoge"}
                     {:id 2 :name "fuga"}]
        section-sentences-exp [{:section_id 2 :sentence_id 1}]]
    (sut/insert->section-sentence ["hoge" "fuga"] "これはテストです" "fooo")
    (t/is (lst-hashmap-left-keys-eq sentences-exp (j/query test-db ["select * from sentences"])))
    (t/is (lst-hashmap-left-keys-eq section-exp (j/query test-db ["select * from sections"])))
    (t/is (lst-hashmap-left-keys-eq section-sentences-exp
                                    (j/query test-db ["select * from section_sentences"])))
    )
  (cleanup))

(t/deftest output-test
  (setup)
  (let []
    (sut/insert->section-sentence ["hoge" "fuga"] "これはテストです" "fooo")
    (t/is (= (clojure.string/replace (sut/output-markdown ["hoge" "fuga"] "fooo")
                                     #"\n"
                                     "")
             "# hoge## fugaこれはテストです")))
  (cleanup))

(t/deftest remove-test
  (setup)
  (t/is (= (sut/sec-tree->remove-target #(= (:name %) "foo")
                                        {:name "hoge" :children [{:name "foo" :children nil}]})
         {:name "foo" :children nil}))
  (cleanup))
