(ns owlet-cms.routes.home
  (:require [owlet-cms.layout :as layout]
            [compojure.core :refer [defroutes GET PUT]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
           (GET "/" [] (home-page))
           (GET "/docs" [] (response/ok (-> "docs/docs.md" io/resource slurp))))

