(ns owlet-api.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[owlet-api started successfully]=-"))
   :middleware identity})
