(ns distinct.distinct-test
  (:require [clojure.test :refer [deftest is testing]]
            [distinct.core :as d]))

(def distincts [#?(:clj [d/distinct-raw-transient "distinct-raw-transient"])
                [d/distinct-transient "distinct-transient"]
                [d/distinct-contain "distinct-contain"]
                [d/distinct-nolazy "distinct-nolazy"]])

(defn compare-distinct [coll dd ddname]
  (is (= (distinct coll) (dd coll)) (str ddname " failed")))

(deftest basic-test
  (doseq [[f f-name] distincts]
    (is (= () (f [])) (str f-name " failed"))
    (is (= (range 10) (f (range 10))) (str f-name " failed"))
    (is (= (range 10) (f [0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9])) (str f-name " failed"))))

(deftest longer-test
  (doseq [size [10 100 1000 10000]
          :let [coll (vec (repeatedly size #(rand-int 1000)))]]
    (doseq [[f f-name] distincts]
      (compare-distinct coll f f-name))))

#?(:cljs (cljs.test/run-tests))
