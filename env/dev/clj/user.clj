(ns user
  (:require [mount.core :as mount]
            owlet-cms.core))

(defn start []
  (mount/start-without #'owlet-cms.core/repl-server))

(defn stop []
  (mount/stop-except #'owlet-cms.core/repl-server))

(defn restart []
  (stop)
  (start))


