(ns owlet-api.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [owlet-api.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[owlet-api started successfully using the development profile]=-"))
   :middleware wrap-dev})
