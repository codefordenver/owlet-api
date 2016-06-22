(ns owlet-api.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [owlet-api.layout :refer [error-page]]
            [owlet-api.routes.home :refer [home-routes]]
            [owlet-api.routes.api :refer [api-routes]]
            [owlet-api.routes.services :refer [service-routes]]
            [compojure.route :as route]
            [owlet-api.middleware :as middleware]))

(def app-routes
  (routes
    #'api-routes
    #'service-routes
    (wrap-routes #'home-routes middleware/wrap-csrf)
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
