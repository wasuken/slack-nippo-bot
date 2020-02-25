(ns slack-nippo-bot.db
  (:require [slack-nippo-bot.util :refer :all]
            [clojure.java.jdbc :as j]
            [environ.core :refer [env]])
  (:gen-class))

(def db {:connection-uri (str "jdbc:sqlite:" (env :dbpath))})

(defn link-sections
  ([sec-recs] (link-sections sec-recs (* (count sec-recs) 2)))
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
         (insert->section-sentence (drop 1 sec-lst) text user (:children tree) (:parent_id tree))
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

(defn tree->delete-db [tree user]
  (when-not (nil? tree)
    (j/delete! db :sections ["id = ? and user = ?" (:id tree) user])
    (doseq [child (:children tree)]
      (tree->delete-db child user))))

(defn section-sentences->value->format [user sec-id]
  (clojure.string/join "\n\n"
                       (map #(:value %)
                            (j/query db [(str "select * from sections as sec "
                                              "join section_sentences as ss "
                                              "on ss.section_id = sec.id "
                                              "join sentences as st "
                                              "on ss.sentence_id = st.id "
                                              "where sec.user = ? and sec.id = ?")
                                         user
                                         sec-id]))))

(defn sec-tree->remove-target [func top-tree]
  (let [result (atom nil)]
    (letfn [(search [f tree]
              (cond (or (f tree) (nil? tree) (not (nil? @result)))
                    (reset! result tree)
                    :else
                    (doseq [child (:children tree)]
                        (search f child))))]
      (search func top-tree))
    @result))

;; (defn output-md
;;   ([user]
;;    (output-md user nil))
;;   ([user parent-id]
;;    (output-md user parent-id (j/query db [(str "select * from sections where user = ?") user])))
;;   ([user parent-id link-secs]
;;    ))

(defn output-markdown
  ([user sec-lst] (output-markdown user sec-lst 1))
  ([user sec-lst nest-level]
   (let [recs (j/query db [(str "select * from sections where user = ?") user])]
     (output-markdown user sec-lst nest-level (link-sections recs (* (count recs) 2)))))
  ([user sec-lst nest-level tree]
   (cond (nil? (first sec-lst))
         ""
         :else
         (let [contents (clojure.string/join
                         "\n\n" (map #(str (clojure.string/join "" (repeat nest-level "#")) " "
                                           (:name %) "\n\n"
                                           (section-sentences->value->format user (:id %)))
                                     tree))]
           (str contents (clojure.string/join
                                "\n\n"
                                (map #(output-markdown user
                                                       (drop 1 sec-lst)
                                                       (+ 1 nest-level)
                                                       (:children %))
                                     tree)))))))
