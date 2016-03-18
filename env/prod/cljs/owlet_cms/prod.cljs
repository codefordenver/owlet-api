(ns owlet-cms.app
  (:require [owlet-cms.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
