(ns owlet-cms.routes.api
  (:require [owlet-cms.db.core :refer [*db*] :as db])
  (:require [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :refer :all]
            ;; [ring.handler.dump :refer [handle-dump]]
            [compojure.api.sweet :refer [context]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]))

(defn is-not-social-login-but-verified? [user]
  (let [identity0 (first (:identities user))]
    (if (:isSocial identity0)
      true
      (if (:email_verified user)
        true
        false))))

(defn find-user-by-id [id]
  (try
    (jdbc/with-db-transaction
      [t-conn *db*]
      (jdbc/db-set-rollback-only! t-conn)
      (db/get-user {:id id}))
    (catch Exception e (str "caught e:" (.getNextException e)))))

(defn find-user-by-email [email]
  (try
    (jdbc/with-db-transaction
      [t-conn *db*]
      (jdbc/db-set-rollback-only! t-conn)
      (db/get-user-by-email {:email email}))
    (catch Exception e (str "caught e:" (.getNextException e)))))

(defn handle-user-insert-webhook [res]
  (let [user (get-in res [:params :user])
        ;; context (get-in res [:params :context])
        found? (find-user-by-id (:user_id user))]
    (pprint user)
    (if-not found?
      (let [transact! (try
                        (if (is-not-social-login-but-verified? user)
                          (jdbc/with-db-transaction
                            [t-conn *db*]
                            (jdbc/db-set-rollback-only! t-conn)
                            (db/create-user!
                              {:id       (:user_id user)
                               :name     (:name user)
                               :nickname (:nickname user)
                               :email    (:email user)
                               :picture  (:picture user)})))
                        (catch Exception e (str "caught e:" (.getNextException e))))]
        ;; returns 1 if inserted
        (if transact!
          (ok user)
          (internal-server-error transact!)))
      (ok "user exists"))))

(defn handle-get-users [_]
  (let [users (jdbc/with-db-transaction
                [t-conn *db*]
                (jdbc/db-set-rollback-only! t-conn)
                (db/get-users))]
    (when users
      (ok {:data users}))))

(defn handle-update-users-district-id! [res]
  (let [ok-response (ok "updated user's district id")
        user_id (get-in res [:params :user-id])
        district_id (get-in res [:params :district-id])
        found? (find-user-by-id user_id)
        update-district-id! (fn [user_id district-id]
                              (jdbc/with-db-transaction
                                [t-conn *db*]
                                (jdbc/db-set-rollback-only! t-conn)
                                (db/update-user-district-id! {:district_id district-id
                                                              :id          user_id})))]
    (if found?
      ;; check for multiple accounts with the same email
      (let [n-accounts (into [] (find-user-by-email (:email found?)))]
        (if (> (count n-accounts) 1)
          ;; update all accounts with same :email
          (let [_ (doseq [user n-accounts]
                     (update-district-id! (:id user) district_id))]
            ok-response)
          ;; update existing single user
          (update-district-id! user_id district_id)))
      ;; first time user, or single time user
      (let [transaction! (update-district-id! user_id district_id)]
        (if (= 1 transaction!)
          ok-response
          (do
            (println transaction!)
            (internal-server-error transaction!)))))))

(defn handle-singler-user-lookup [req]
  (let [user-id (get-in req [:route-params :id])
        found (find-user-by-id user-id)]
    (if found
      (ok found)
      (not-found "user not found"))))

(defroutes api-routes
           (context "/api" []
                    (GET "/user/:id" [] handle-singler-user-lookup)
                    (GET "/users" [] handle-get-users)
                    (PUT "/users-district-id" {params :params} handle-update-users-district-id!)
                    (PUT "/webhook" {params :params} handle-user-insert-webhook)))


