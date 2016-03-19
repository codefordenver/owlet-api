(ns owlet-cms.routes.api
  (:require [compojure.core :refer [defroutes PUT]]
            [ring.util.http-response :refer :all]
            [ring.handler.dump :refer [handle-dump]]
            [compojure.api.sweet :refer [context]]
            [clojure.java.jdbc :as jdbc]
            [owlet-cms.db.core :refer [*db*] :as db]))

(defn handle-user-upsert-webhook [res]
  (clojure.pprint/pprint (get-in res [:params]))
  (ok (get-in res [:params :json])))

(defroutes api-routes
           (context "/api" []
                    (PUT "/webhook" {params :params} handle-user-upsert-webhook)))


