(ns owlet-cms.routes.api
  (:require [owlet-cms.db.core :refer [*db*] :as db])
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer [context]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn is-not-social-login-but-verified? [user]
  (let [identity0 (first (:identities user))]
    (if (:isSocial identity0)
      true
      (if (:email_verified user)
        true
        false))))

(defn find-user-by-social-id [sid]
  (try
    (jdbc/with-db-transaction
      [t-conn *db*]
      (jdbc/db-set-rollback-only! t-conn)
      (db/get-user {:sid sid}))
    (catch Exception e (str "caught e:" (.getNextException e)))))

(defn get-user-entries-by-id [id]
  (try
    (jdbc/with-db-transaction
      [t-conn *db*]
      (jdbc/db-set-rollback-only! t-conn)
      (db/get-user-entries-by-id {:id id}))
    (catch Exception e (str "caught e:" (.getNextException e)))))

(defn find-user-by-email [email]
  (try
    (jdbc/with-db-transaction
      [t-conn *db*]
      (jdbc/db-set-rollback-only! t-conn)
      (db/get-user-by-email {:email email}))
    (catch Exception e (str "caught e:" (.getNextException e)))))

(defn handle-user-insert-webhook! [res]
  (let [user (get-in res [:params :user])
        found? (find-user-by-social-id (:user_id user))]
    (if-not found?
      (let [transact! (try
                        (if (is-not-social-login-but-verified? user)
                          (jdbc/with-db-transaction
                            [t-conn *db*]
                            (jdbc/db-set-rollback-only! t-conn)
                            (db/create-user!
                              {:sid      (:user_id user)
                               :name     (:name user)
                               :nickname (:nickname user)
                               :email    (:email user)
                               :picture  (:picture user)})))
                        (catch Exception e (str "caught e:" (.getNextException e))))]
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

(defn handle-update-users-district-id! [req]
  (let [ok-response (ok "updated user's district id")
        user_id (get-in req [:params :user-id])
        district_id (get-in req [:params :district-id])
        found? (find-user-by-social-id user_id)
        update-district-id! (fn [user_id district-id]
                              (jdbc/with-db-transaction
                                [t-conn *db*]
                                (jdbc/db-set-rollback-only! t-conn)
                                (db/update-user-district-id! {:district_id district-id
                                                              :sid         user_id})))]
    (if found?
      ;; check for multiple accounts with the same email
      (let [n-accounts (into [] (find-user-by-email (:email found?)))]
        (if (> (count n-accounts) 1)
          ;; update all accounts with same :email
          (let [_ (doseq [user n-accounts]
                    (update-district-id! (:sid user) district_id))]
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

(defn- get-entries-by-id
  "get entries ids from contentful"
  [id]
  (try
    (jdbc/with-db-transaction
      [t-conn *db*]
      (jdbc/db-set-rollback-only! t-conn)
      (db/get-entry-by-id {:id id}))
    (catch Exception e (str "caught e:" (.getNextException e)))))

(defn handle-singler-user-lookup [req]
  (let [user-id (get-in req [:route-params :id])]
    (if-let [found (find-user-by-social-id user-id)]
      (if-let [entries (get-user-entries-by-id (:id found))]
        (if (not (empty? entries))
          (let [entry_ids (mapv #(get % :entry_id) entries)
                join-entries-user (assoc found
                                    :entries (->> entry_ids
                                                  (map #(:entry (first (get-entries-by-id %))))))]
            (ok join-entries-user))
          (ok found)))
      (not-found "user not found"))))


(defn- check-for-duplicate-entries [sid]
  (if-let [found (find-user-by-social-id sid)]
    (if-let [entries (get-user-entries-by-id (:id found))]
      (if (not (empty? entries))
        (let [entry_ids (mapv #(get % :entry_id) entries)]
          (->> entry_ids
               (mapv #(:entry (first (get-entries-by-id %))))))
        []))))

(defn handle-update-user-content! [req]
  (let [entry-id (get-in req [:params :sys :id])
        social-id (get-in req [:params :fields :socialid :en-US])
        publish-header? (= (get-in req [:headers "x-contentful-topic"])
                           "ContentManagement.Entry.publish")
        delete-header? (= (get-in req [:headers "x-contentful-topic"])
                          "ContentManagement.Entry.delete")]
    (if publish-header?
      (if (and entry-id social-id)
        (let [has-entry? (some? (some (fn [e] (= entry-id e))
                                      (check-for-duplicate-entries social-id)))]
          (if-not has-entry?
            (if-let [user-found (find-user-by-social-id social-id)]
              (let [insert-entry! (try
                                    (jdbc/with-db-transaction
                                      [t-conn *db*]
                                      (jdbc/db-set-rollback-only! t-conn)
                                      (db/insert-user-entry! {:entry entry-id}))
                                    (catch Exception e (str "caught e:" (.getNextException e))))]
                (if insert-entry!
                  (let [insert-into-user_entries! (try
                                                    (jdbc/with-db-transaction
                                                      [t-conn *db*]
                                                      (jdbc/db-set-rollback-only! t-conn)
                                                      (db/insert-into-user_entries! {:user_id  (:id user-found)
                                                                                     :entry_id (:id (first insert-entry!))}))
                                                    (catch Exception e (str "caught e:" (.getNextException e))))]
                    (if insert-into-user_entries!
                      (ok insert-into-user_entries!)
                      (internal-server-error "opps")))
                  (internal-server-error "opps"))))
            (not-modified "Only updating for published content."))))
      (not-modified "Only updating for published content."))
    #_(if delete-header?
      (if (and entry-id social-id)
        (if-let [found (find-user-by-social-id social-id)]
          (let [id (:id found)
                ])))
      (not-modified "Only updating for published content."))
    ))

;; TODO: generalize this function so that its able to post any content-type passed in
(defn handle-content-creation [req]
  (let [{:keys [url social-id content-type auto-publish?]} (:params req)
        space-id (System/getenv "SPACE_ID")
        contentful-auth-token (System/getenv "CONTENTFUL_AUTH_TOKEN")
        opts {:headers {"X-Contentful-Content-Type" content-type
                        "Authorization"             (str "Bearer " contentful-auth-token)
                        "Content-Type"              "application/json"}
              :body    (json/encode {:fields {:url      {"en-US" url}
                                              :socialid {"en-US" social-id}}})}
        {:keys [status body]} @(http/post (format "https://api.contentful.com/spaces/%s/entries" space-id) opts)]
    (if (and auto-publish? (= status 201))
      (let [entry-json (json/decode body true)
            entry-id (get-in entry-json [:sys :id])
            revision (get-in entry-json [:sys :version])
            {:keys [body]} @(http/put (format "https://api.contentful.com/spaces/%1s/entries/%2s/published" space-id entry-id)
                                      (assoc-in opts [:headers "X-Contentful-Version"] revision))]
        (println "publish request")
        (ok body))
      (do
        (println "create request")
        (ok body)))))

(defroutes api-routes
           (context "/api" []
                    (GET "/user/:id" [] handle-singler-user-lookup)
                    (GET "/users" [] handle-get-users)
                    (PUT "/users-district-id" {params :params} handle-update-users-district-id!)
                    (context "/content" []
                             (context "/create" []
                                      (POST "/entry"
                                            {params :params} handle-content-creation))))
           (context "/webhooks" []
                    (PUT "/auth0" {params :params} handle-user-insert-webhook!)
                    (POST "/contentful" {params :params} handle-update-user-content!)))


