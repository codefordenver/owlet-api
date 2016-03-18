(ns owlet-cms.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[owlet-cms started successfully]=-"))
   :middleware identity})
