(ns owlet-cms.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [owlet-cms.core-test]))

(doo-tests 'owlet-cms.core-test)

