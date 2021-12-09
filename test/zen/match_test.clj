(ns zen.match-test
  (:require [clojure.test :refer [deftest] :as t]
            [zen.test-utils :refer [vmatch match valid valid-schema! invalid-schema]]
            [zen.match :as sut]
            [matcho.core :as matcho]
            [zen.core]))


(t/deftest test-match-internals
  (matcho/match
    (sut/match {:a 1 :b 2} {:a 2})
    [{:expected 2, :but 1, :path [:a]}])

  (matcho/match
    (sut/match {:a {:b {:c 1}} :b 1} {:a {:b {:c 2}}})
    [{:expected 2, :but 1, :path [:a :b :c]}])


  (matcho/match
    (sut/match
      {:name [{:use "some"} {:use "official"}]}
      {:name #{{:use "official"}}} )
    empty?)

  (matcho/match
    (sut/match
      {:name [{:use "some"} {:use "other"}]}
      {:name #{{:use "official"}}})
    [{:expected {:use "official"}
      :but [{:use "some"} {:use "other"}],
      :path [:name]}])

  (t/testing "Each element provided in #{} array must match at least one element from the instance array"
    (matcho/match
      (sut/match
        {:name [{:use "some"} {:use "other"} {:use "other2"}]}
        {:name #{{:use "some"} {:use "other"}}})
      empty?)

    (matcho/match
      (sut/match
        {:name [{:use "some"} {:use "other"} {:use "other2"}]}
        {:name #{{:use "some"} {:use "other3"}}})
      [{:expected {:use "other3"}
        :but [{:use "some"} {:use "other"} {:use "other2"}],
        :path [:name]}]))

  (matcho/match
    (sut/match 1 1)
    empty?)

  (matcho/match
    (sut/match nil nil)
    empty?)

  (matcho/match
    (sut/match "1" "1")
    empty?)

  (matcho/match
    (sut/match "1" 1)
    [{:expected 1 :but "1"}])

  (matcho/match
    (sut/match "1" nil)
    [{:expected nil :but "1"}])

  (matcho/match
    (sut/match nil "1")
    [{:expected "1" :but nil}]))


(deftest test-zen-match
  (def tctx (zen.core/new-context {:unsafe true}))

  (zen.core/load-ns!
    tctx {'ns 'test.match
          'map-match {:zen/tags #{'zen/schema}
                      :type 'zen/map
                      :keys {:kind {:type 'zen/string}
                             :nested {:type 'zen/map
                                      :keys {:prop {:type 'zen/string}}}}
                      :match {:kind "user"
                              :nested {:prop "prop"}}}})

  (valid tctx 'test.match/map-match {:kind "user" :nested {:prop "prop"}})

  (match tctx 'test.match/map-match {:kind "ups" :nested {:prop "ups"}}
         [{:message "Expected \"user\", got \"ups\"",
           :type "match",
           :path [:kind],
           :schema ['test.match/map-match]}
          {:message "Expected \"prop\", got \"ups\"",
           :type "match",
           :path [:nested :prop],
           :schema ['test.match/map-match]}]))

