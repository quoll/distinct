(ns distinct.core)

;; Uses the normal core function for transducing, but without the lazy-seq in the standard distinct
(defn distinct-nolazy
  "Returns a lazy sequence of the elements of coll with duplicates removed.
  Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (if (contains? @seen input)
            result
            (do (vswap! seen conj input)
                (rf result input))))))))
  ([coll]
   (sequence (distinct-nolazy) coll)))


;; Uses a set with single access to track seen
(defn distinct-nocontains
  "Returns a lazy sequence of the elements of coll with duplicates removed.
  Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [s @seen
                n (conj s input)]
            (if #?(:clj (identical? s n)
                   :cljs (= (-count s) (-count n)))
              result
              (do (vreset! seen n)
                  (rf result input)))))))))
  ([coll]
   (sequence (distinct-nocontains) coll)))

;; Uses a volatile transient with standard API access to track seen
(defn distinct-transient
  "Returns a lazy sequence of the elements of coll with duplicates removed.
  Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [seen (volatile! (transient #{}))]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (if (contains? @seen input)
            result
            (do (vswap! seen conj! input)
                (rf result input))))))))
  ([coll]
   (sequence (distinct-transient) coll)))

;; Uses a volatile transient with standard API access to track seen
(defn distinct-transient-nocontains
  "Returns a lazy sequence of the elements of coll with duplicates removed.
  Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [seen (volatile! (transient #{}))]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [s @seen
                sc #?(:clj (count s) :cljs (-count s))
                n (conj! s input)]
            (if (= sc #?(:clj (count n) :cljs (-count n)))
              result
              (do (vreset! seen n)
                  (rf result input)))))))))
  ([coll]
   (sequence (distinct-transient-nocontains) coll)))

;; Uses a volatile transient with raw access to track seen
#?(:clj 
   (defn distinct-raw-transient
     "Returns a lazy sequence of the elements of coll with duplicates removed.
     Returns a stateful transducer when no collection is provided."
     ([]
      (fn [rf]
        (let [seen (volatile! (transient #{}))]
          (fn
            ([] (rf))
            ([result] (rf result))
            ([result input]
             (if (.contains ^clojure.lang.ITransientSet @seen input)
               result
               (do (vswap! seen conj! input)
                   (rf result input))))))))
     ([coll]
      (sequence (distinct-raw-transient) coll))))

