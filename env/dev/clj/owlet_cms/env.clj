(ns owlet-cms.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [owlet-cms.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[owlet-cms started successfully using the development profile]=-"))
   :middleware wrap-dev})
