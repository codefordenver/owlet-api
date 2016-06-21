(ns owlet-api.test.db.core
  (:require [owlet-api.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [owlet-api.config :refer [env]]
            [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'owlet-api.config/env
      #'owlet-api.db.core/*db*)
    (migrations/migrate ["migrate"] (env :database-url))
    (f)))

(deftest test-users
  (jdbc/with-db-transaction
    [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-user!
               t-conn
               {:id       "1"
                :name     "Samuel"
                :nickname "samuel1"
                :email    "sam.paz@example.com"
                :picture  "picture-url"})))
    (is (= {:id       "1"
            :name     "Samuel"
            :nickname "samuel1"
            :email    "sam.paz@example.com"
            :picture  "picture-url"
            :admin    false}
           (db/get-user t-conn {:id "1"})))))
