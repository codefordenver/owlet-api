(ns owlet-cms.routes.api
  (:require [owlet-cms.db.core :refer [*db*] :as db])
  (:require [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :refer :all]
            [ring.handler.dump :refer [handle-dump]]
            [compojure.api.sweet :refer [context]]
            [clojure.java.jdbc :as jdbc]))

(defn is-not-social-login-but-verified? [user]
  (let [identity0 (first (:identities user))]
    (if (:isSocial identity0)
      true
      (if (:email_verified user)
        true
        false))))

(defn handle-user-upsert-webhook [res]
  (let [user (get-in res [:params :user])
        ;; context (get-in res [:params :context])
        ;; _ (clojure.pprint/pprint user)
        transact! (try
                    (if (is-not-social-login-but-verified? user)
                      (jdbc/with-db-transaction
                        [t-conn *db*]
                        (jdbc/db-set-rollback-only! t-conn)
                        (db/create-user!
                          {:id       (:_id user)
                           :name     (:name user)
                           :nickname (:nickname user)
                           :email    (:email user)
                           :picture  (:picture user)})))
                    (catch Exception e (str "caught e:" (.getNextException e))))]
    ;; returns 1 if inserted
    (println transact!)
    (when transact!
      (ok user))))

(defn handle-get-users [_]
  (let [users (jdbc/with-db-transaction
                [t-conn *db*]
                (jdbc/db-set-rollback-only! t-conn)
                (db/get-users))]
    (when users
      (ok {:data users}))))

(defroutes api-routes
           (context "/api" []
                    (GET "/users" [] handle-get-users)
                    (PUT "/webhook" {params :params} handle-user-upsert-webhook)))


