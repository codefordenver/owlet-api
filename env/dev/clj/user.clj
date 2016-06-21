(ns user
  (:require [mount.core :as mount]
            owlet-api.core))

(defn start []
  (mount/start-without #'owlet-api.core/repl-server))

(defn stop []
  (mount/stop-except #'owlet-api.core/repl-server))

(defn restart []
  (stop)
  (start))


