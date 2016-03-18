(ns owlet-cms.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [owlet-cms.layout :refer [error-page]]
            [owlet-cms.routes.home :refer [home-routes]]
            [owlet-cms.routes.services :refer [service-routes]]
            [compojure.route :as route]
            [owlet-cms.middleware :as middleware]))

(def app-routes
  (routes
    #'service-routes
    (wrap-routes #'home-routes middleware/wrap-csrf)
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
