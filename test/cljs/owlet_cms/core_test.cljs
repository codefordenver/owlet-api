(ns owlet-cms.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [reagent.core :as reagent :refer [atom]]
            [owlet-cms.core :as rc]))

(deftest test-home
  (is (= true true)))

