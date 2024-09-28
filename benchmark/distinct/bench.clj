(ns distinct.bench
  (:require [distinct.core :as d]
            [criterium.core :as c]))

(def distincts [[d/distinct-raw-transient "distinct-raw-transient"]
                [d/distinct-transient "distinct-transient"]
                [d/distinct-contain "distinct-contain"]
                [d/distinct-nolazy "distinct-nolazy"]])

(defn format-time [estimate]
  (let [mean (first estimate)
        [factor unit] (c/scale-time mean)]
    (c/format-value mean factor unit)))

(defn pcnt [r1 r2]
  (->> (/ (first (:mean r2))
          (first (:mean r1)))
       (* 100)
       (- 100)
       int))

(defmacro race [results-base base body]
 `(let [_#       (assert (= ~base ~body))
        results# (c/quick-benchmark ~body {})
        percent# (pcnt ~results-base results#)]
    (println ~(pr-str body) "\t"
             (format-time (:mean ~results-base)) "=>" (format-time (:mean results#))
             (str "(" (- percent#) "%)"))))


(defn run [& args]
  (doseq [size [10 100 1000 10000 1000000]
          :let [coll (vec (repeatedly size #(rand-int 1000)))]]
    (println (str "\nRunning benchmark for size " size "..."))
    (let [doall-base (c/quick-benchmark (doall (distinct coll)) {})
          into-base (c/quick-benchmark (into [] (distinct coll)) {})
          distinct-coll (distinct coll)]
      (println "Testing...")
      (race doall-base distinct-coll (doall (d/distinct-raw-transient coll)))
      (race into-base distinct-coll (into [] (d/distinct-raw-transient coll)))
      (println)
      (race doall-base distinct-coll (doall (d/distinct-transient coll)))
      (race into-base distinct-coll (into [] (d/distinct-transient coll)))
      (println)
      (race doall-base distinct-coll (doall (d/distinct-contain coll)))
      (race into-base distinct-coll (into [] (d/distinct-contain coll)))
      (println)
      (race doall-base distinct-coll (doall (d/distinct-nolazy coll)))
      (race into-base distinct-coll (into [] (d/distinct-nolazy coll))))))

