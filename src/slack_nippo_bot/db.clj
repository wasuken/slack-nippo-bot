(ns slack-nippo-bot.db
  (:require [slack-nippo-bot.util :refer :all]
            [clojure.java.jdbc :as j]))

(def db {:connection-uri (str "jdbc:sqlite:" (:db-path config))})

(defn link-sections
  ([sec-recs limit] (link-sections sec-recs limit 0))
  ([sec-recs limit n]
   (if (< n limit)
     (let [result (if (= n 0)
                    (sort (fn [x y] (or (compare (count (filter (fn [z] (= (:parent_id z) (:id x))) sec-recs))
                                                 (count (filter (fn [z] (= (:parent_id z) (:id y))) sec-recs)))
                                        (cond (= (:id x) (:parent_id)) -1
                                              (= (:parent_id x) (:id)) 1
                                              :else 0)))
                          sec-recs)
                    sec-recs)
           update-rec (merge-with concat
                                  (some #(if (= (:id %) (:parent_id (first result))) %) result)
                                  {:children [(first result)]})]
       (if-not (nil? (:id update-rec))
         (let [removed-result (remove #(= (:id %) (:id update-rec)) (drop 1 result))
               merge-result (concat removed-result [update-rec])]
           (link-sections merge-result limit (+ 1 n)))
         (link-sections (concat (drop 1 result) [(first result)]) limit (+ 1 n))))
     sec-recs)))

(defn insert->section-sentence
  ([sec-lst text user]
   (let [secs (j/query db ["select * from sections where user = ?" user])]
     (insert->section-sentence sec-lst text user
                               (link-sections secs (* (count secs) 2)))))
  ([sec-lst text user tree]
   (insert->section-sentence sec-lst text user tree nil))
  ([sec-lst text user tree parent-id]
   (cond (zero? (count sec-lst))
         (j/insert! db :section_sentences
                    {:section_id parent-id
                     :sentence_id (get (first (j/insert! db :sentences
                                                         {:value text}
                                                         {:return-keys ["id"]}))
                                       (keyword "last_insert_rowid()"))})
         (some #(= (:name %) (first sec-lst)) tree)
         (insert->section-sentence sec-lst text user (:children tree) (:parent_id tree))
         :else
         (let [before-parent-id (atom nil)]
           (doseq [sec sec-lst]
             (let [inserted (j/insert! db :sections
                                       {:name sec :parent_id @before-parent-id :user user})]
               (reset! before-parent-id (get (first inserted) (keyword "last_insert_rowid()")))))
           (j/insert! db :section_sentences
                      {:section_id @before-parent-id
                       :sentence_id (get (first (j/insert! db :sentences {:value text :user user} {:return-keys ["id"]}))
                                         (keyword "last_insert_rowid()"))})))))

(defn section-sentences->value->format [sec-id]
  (clojure.string/join "\n\n"
                       (map #(:value %)
                            (j/query db [(str "select * from sentences where id in "
                                              "(select sentence_id from section_sentences where section_id = ?)") sec-id]))))

(defn output-markdown
  ([user] (output-markdown user 1))
  ([user nest-level]
   (let [recs (j/query db [(str "select * from sections where user = ?") user])]
     (output-markdown user nest-level (link-sections recs (* (count recs) 2)))))
  ([user nest-level tree]
   (let [contents (clojure.string/join
                "\n\n" (map #(str (clojure.string/join "" (repeat nest-level "#")) " "
                                  (:name %) "\n\n" (section-sentences->value->format (:id %))) tree))]
     (cond (zero? (count contents))
           contents
           :else
           (str contents (clojure.string/join
                        "\n\n"
                        (map #(output-markdown user (+ 1 nest-level) (:children %)) tree)))))))
