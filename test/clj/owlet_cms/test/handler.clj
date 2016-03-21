(ns owlet-cms.test.handler
  (:require [clojure.test :refer :all])
  (:require [ring.mock.request :refer :all]
            [owlet-cms.handler :refer :all]
            [cheshire.core :as json]))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "api/users"
    (let [response (app (request :get "/api/users"))]
      (is (= (json/generate-string {:data []})
             (slurp (:body response)))))))
