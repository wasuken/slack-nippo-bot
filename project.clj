(defproject slack-nippo-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aero "1.1.5"]
                 [org.julienxx/clj-slack "0.6.3"]
                 [org.clojure/data.json "0.2.7"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.xerial/sqlite-jdbc "3.30.1"]
                 [stylefruits/gniazdo "1.1.2"]]
  :repl-options {:init-ns slack-nippo-bot.core}
  :main slack-nippo-bot.core)
