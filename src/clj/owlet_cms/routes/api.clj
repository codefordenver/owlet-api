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
    (if (and (:isSocial identity0) (:email_verified user))
      true
      false)))

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
      (let [n-accounts (vec (find-user-by-email (:email found?)))]
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
        (if-not (empty? entries)
          (let [entry_ids (mapv #(get % :entry_id) entries)
                join-entries-user (assoc found :entries
                                               (map #(:entry (get-entries-by-id %)) entry_ids))]
            (ok join-entries-user))
          (ok found)))
      (not-found "user not found"))))


(defn- check-for-duplicate-entries [sid]
  (if-let [found (find-user-by-social-id sid)]
    (if-let [entries (get-user-entries-by-id (:id found))]
      (if (seq entries)
        (let [entry_ids (mapv #(get % :entry_id) entries)]
          (mapv #(:entry (get-entries-by-id %)) entry_ids))
        []))))

(defn handle-update-user-content! [req]
  (let [entry-id (get-in req [:params :sys :id])
        social-id (get-in req [:params :fields :socialid :en-US])
        topic-header (get-in req [:headers "x-contentful-topic"])
        publish-header? (= topic-header "ContentManagement.Entry.publish")
        delete-header? (= topic-header "ContentManagement.Entry.delete")]
    (cond
      publish-header?
      (if (and entry-id social-id)
        (let [has-entry? (some? (some #(= entry-id %)
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
                      (internal-server-error insert-into-user_entries!)))
                  (internal-server-error insert-entry!))))
            (not-modified "Only updating for published content."))))
      delete-header?
      (let [{:keys [id]} (jdbc/with-db-transaction
                           [t-conn *db*]
                           (jdbc/db-set-rollback-only! t-conn)
                           (db/get-entry-id-from-entries {:entry entry-id}))
            delete-entry-from-user_entries!
            (try
              (jdbc/with-db-transaction
                [t-conn *db*]
                (jdbc/db-set-rollback-only! t-conn)
                (db/delete-entry-id-from-user-entries! {:entry_id id}))
              (catch Exception e (str "caught e:" (.getNextException e))))]
        (if delete-entry-from-user_entries!
          (let [delete-entry-from-entries!
                (try
                  (jdbc/with-db-transaction
                    [t-conn *db*]
                    (jdbc/db-set-rollback-only! t-conn)
                    (db/delete-entry-from-entries! {:entry entry-id}))
                  (catch Exception e (str "caught e:" (.getNextException e))))]
            (if delete-entry-from-entries!
              (ok delete-entry-from-user_entries!)
              (internal-server-error delete-entry-from-entries!)))
          (internal-server-error delete-entry-from-user_entries!)))
      :else
      (not-modified "Only updating for published content."))))

(defn handle-content-creation!
  "handle content creation from a UI
  - must pass revision number for auto-publish option to work"
  [req]
  (let [{:keys [content-type auto-publish? space-id fields]} (:params req)
        _space-id_ (or space-id (System/getenv "OWLET_CONTENTFUL_DEFAULT_SPACE_ID"))
        contentful-management-auth-token (System/getenv "OWLET_CONTENTFUL_MANAGEMENT_AUTH_TOKEN")
        opts {:headers {"X-Contentful-Content-Type" content-type
                        "Authorization"             (str "Bearer " contentful-management-auth-token)
                        "Content-Type"              "application/json"}
              :body    (json/encode {:fields fields})}
        {:keys [status body]}
        @(http/post
           (format "https://api.contentful.com/spaces/%s/entries" _space-id_) opts)]
    (if (and auto-publish? (= status 201))
      (let [entry-json (json/decode body true)
            entry-id (get-in entry-json [:sys :id])
            revision (get-in entry-json [:sys :version])
            publish-opts (assoc-in opts [:headers "X-Contentful-Version"] revision)
            {:keys [body]} @(http/put
                              (format "https://api.contentful.com/spaces/%1s/entries/%2s/published"
                                      _space-id_ entry-id)
                              publish-opts)]
        (ok body))
      (ok body))))

(defn handle-content-update!
  "handle content update from a UI
  - must pass entry-id and revision number for update"
  [req]
  (let [{:keys [content-type space-id entry-id fields]} (:params req)
        _space-id_ (or space-id (System/getenv "OWLET_CONTENTFUL_DEFAULT_SPACE_ID"))
        contentful-management-auth-token (System/getenv "OWLET_CONTENTFUL_MANAGEMENT_AUTH_TOKEN")
        opts {:headers {"X-Contentful-Content-Type" content-type
                        "Authorization"             (str "Bearer " contentful-management-auth-token)
                        "Content-Type"              "application/json"}}
        {:keys [status body]}
        @(http/get (format
                     "https://api.contentful.com/spaces/%1s/entries/%2s"
                     _space-id_ entry-id) opts)]
    ;; if retrieval from Content Management API is successful
    ;; perform the actual update..
    (if (= status 200)
      (let [revision (get-in (json/decode body true) [:sys :version])
            _opts (assoc-in opts [:headers "X-Contentful-Version"] revision)
            update-opts (assoc _opts :body (json/encode {:fields fields}))
            {:keys [status body]}
            @(http/put (format "https://api.contentful.com/spaces/%1s/entries/%2s" _space-id_ entry-id) update-opts)]
        (if (= status 200)
          (ok body)
          (internal-server-error body)))
      (internal-server-error body))))

(defn handle-get-all-entries-for-given-user-or-space
  "asynchronously GET all entries for given user or space
  optionally pass library-view=true param to get all entries for given space"
  [req]
  (let [{:keys [social-id space-id library-view]} (:params req)
        _space-id_ (or space-id (System/getenv "OWLET_CONTENTFUL_DEFAULT_SPACE_ID"))
        owlet-contentful-management-auth-token (System/getenv "OWLET_CONTENTFUL_MANAGEMENT_AUTH_TOKEN")
        owlet-activities-contentful-delivery-auth-token (System/getenv "OWLET_ACTIVITIES_CONTENTFUL_DELIVERY_AUTH_TOKEN")
        opts1 {:headers {"Authorization" (str "Bearer " owlet-contentful-management-auth-token)}}
        opts2 {:headers {"Authorization" (str "Bearer " owlet-activities-contentful-delivery-auth-token)}}
        contentful-cdn-responses (atom [])]
    (if (and social-id (not library-view))
      (if-let [found (find-user-by-social-id social-id)]
        (let [entries (get-user-entries-by-id (:id found))]
          (when (seq entries)
            (let [entry-ids (mapv #(get % :entry_id) entries)
                  contentful-entry-urls (->> entry-ids
                                             (map #(:entry (get-entries-by-id %)))
                                             (map #(format
                                                    "https://api.contentful.com/spaces/%1s/entries/%2s"
                                                    _space-id_ %)))
                  futures (doall (map #(http/get % opts1) contentful-entry-urls))]
              (doseq [resp futures]
                (when (= (:status @resp) 200)
                  (swap! contentful-cdn-responses conj (json/parse-string (:body @resp) true)))))
            (ok @contentful-cdn-responses)))
        (not-found (str "No entries found for s-id:" social-id)))
      (when (and library-view _space-id_)
        (let [{:keys [status body]}
              @(http/get (format
                           "https://cdn.contentful.com/spaces/%1s/entries?"
                           _space-id_) opts2)]
          (if (= status 200)
            (ok body)
            (not-found status)))))))

(defroutes api-routes
           (context "/api" []
                    (GET "/user/:id" [] handle-singler-user-lookup)
                    (GET "/users" [] handle-get-users)
                    (PUT "/users-district-id" {params :params} handle-update-users-district-id!)
                    (context "/content" []
                             (context "/get" []
                                      (GET "/entries"
                                           {params :params} handle-get-all-entries-for-given-user-or-space))
                             (context "/create" []
                                      (POST "/entry"
                                            {params :params} handle-content-creation!))
                             (context "/update" []
                                      (PUT "/entry"
                                           {params :params} handle-content-update!))))
           (context "/webhooks" []
                    (PUT "/auth0" {params :params} handle-user-insert-webhook!)
                    (POST "/contentful" {params :params} handle-update-user-content!)))


