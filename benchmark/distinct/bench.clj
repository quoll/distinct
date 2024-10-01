(ns distinct.bench
  (:require [distinct.core :as d]
            [criterium.core :as c]))

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
  `(let [_#      (assert (= ~base ~body))
        results# (c/quick-benchmark ~body {})
        percent# (pcnt ~results-base results#)
        body-str# ~(pr-str body)
        len# (count body-str#)]
    (println (str
              body-str#
              (cond (>= len# 56) " "
                    (>= len# 48) "\t  "
                    (>= len# 40) "\t\t  "
                    :default "\t\t\t  ")
              (format-time (:mean ~results-base)) "=>" (format-time (:mean results#))
              "(" (- percent#) "%)"))))


(defn run [& args]
  (doseq [size [10 100 1000 10000 1000000]
          :let [coll (vec (repeatedly size #(rand-int 1000)))]]
    (println (str "\nRunning benchmark for size " size "..."))
    (let [doall-lazy-base (c/quick-benchmark (doall (distinct coll)) {})
          doall-base (c/quick-benchmark (doall (sequence (distinct) coll)) {})
          into-base (c/quick-benchmark (into [] (distinct) coll) {})
          distinct-coll (distinct coll)]
      (println "Baseline for (doall (distinct coll))\t\t" (format-time (:mean doall-lazy-base)))
      (println "Baseline for (doall (sequence (distinct) coll))\t" (format-time (:mean doall-base)))
      (println "Baseline for (into [] (distinct) coll)\t\t" (format-time (:mean into-base)))
      (println "Testing...")
      (println "\nRemoving laziness comparison:")
      (race doall-lazy-base distinct-coll (doall (d/distinct-nolazy coll)))

      (println "\nTransducer comparisons:")
      (race doall-base distinct-coll (doall (sequence (d/distinct-nocontains) coll)))
      (race into-base distinct-coll (into [] (d/distinct-nocontains) coll))
      (println)
      (race doall-base distinct-coll (doall (sequence (d/distinct-transient) coll)))
      (race into-base distinct-coll (into [] (d/distinct-transient) coll))
      (println)
      (race doall-base distinct-coll (doall (sequence (d/distinct-transient-nocontains) coll)))
      (race into-base distinct-coll (into [] (d/distinct-transient-nocontains) coll))
      (println)
      (race doall-base distinct-coll (doall (sequence (d/distinct-raw-transient) coll)))
      (race into-base distinct-coll (into [] (d/distinct-raw-transient) coll)))))

