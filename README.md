# distinct
Various reimplementations of clojure.core/distinct.

Benchmarking is based on the example from @tonsky in [CLJ-2090](https://clojure.atlassian.net/browse/CLJ-2090).

# Issues
The single-arity version of the function `clojure.core/distinct` uses a `lazy-seq` which is not as efficient as a transducer based approach can be. By looking at this, it also seems possible to improve the performanc of the transducer code by using a transient value.

This repository contains various reimplementations of `clojure.core/distinct` and benchmarks them.

## Approaches
### `distinct-nolazy`
The first approach is to leave the transducer alone, and only replace the single-arity version. This uses a `sequence` call, so that the result still appears to be lazy.

### `distinct-nocontains`
The second approach does not move to a transient, but instead tries to reduce work done with the set of "seen" values. The original code checks if the value is in the set, and if not then it gets added. However, a `conj` operation will need to do the same check again internally. This approach adds the value to the set, and then uses `identical?` to check if the set changed.

Unfortunately, this relies on behavior of `conj` for sets that is not documented, since a set implementation may decide to return a new value when it is unchanged. This is the case in ClojureScript, since executing `conj` on a set always returns a new object. In that case it falls back to checking the count of the set.

### `distinct-transient`
The third approach is to use a transient set. This should be faster than a non-transient version since transient sets often do not need to allocate new memory and perform copies. This was the original approach taken by @tonsky way back in 2016. However, at the time transient sets did not respond to the `contains?` function, and the proposed change was pushed back until this had been addressed.

### `distinct-transient-nocontains`
The fourth approach tries to use a transient set similarly to `distinct-nocontains`. However, since transients often mutated in place, `count` has to be used for the comparison.

### `distinct-raw-transient`
The fifth approach uses a transient set the same as `distinct-transient`, but rather than using the newly supported `contains?` method, it calls the native `.contains` method on the `ITransientSet` interface. This skips a couple of internal checks on the datatype that occur in `clojure.lang.RT/contains(Object,Object)`. This would not typically be recommended, but if it were hidden in the core libraries then perhaps it might be acceptable.

## Benchmarks
Using the same benchmarking approach adopted by @tonsky in [CLJ-2090](https://clojure.atlassian.net/browse/CLJ-2090), the existing `clojure.core/distinct` function is compared to each of the new implementations, using pregressively larger sequences of random numbers.

The benchmarks can be run using:
```bash
clj -T:bench
```

### Results
Negative numbers indicate an improvement from the baseline, with larger magnitudes being a larger improvement. Positive numbers indicate slower code.
```
Running benchmark for size 10...
Baseline for (doall (distinct coll))		 1.275606 µs
Baseline for (doall (sequence (distinct) coll))	 932.728909 ns
Baseline for (into [] (distinct) coll)		 691.892505 ns
Testing...

Removing laziness comparison:
(doall (d/distinct-nolazy coll))			  1.275606 µs=>1.048474 µs(-17%)

Transducer comparisons:
(doall (sequence (d/distinct-nocontains) coll))		  932.728909 ns=>718.309603 ns(-22%)
(into [] (d/distinct-nocontains) coll)			  691.892505 ns=>496.720107 ns(-28%)

(doall (sequence (d/distinct-transient) coll))		  932.728909 ns=>652.291847 ns(-30%)
(into [] (d/distinct-transient) coll)			  691.892505 ns=>516.240887 ns(-25%)

(doall (sequence (d/distinct-transient-nocontains) coll)) 932.728909 ns=>562.438874 ns(-39%)
(into [] (d/distinct-transient-nocontains) coll)	  691.892505 ns=>448.767418 ns(-35%)

(doall (sequence (d/distinct-raw-transient) coll))	  932.728909 ns=>565.827714 ns(-39%)
(into [] (d/distinct-raw-transient) coll)		  691.892505 ns=>448.127584 ns(-35%)

Running benchmark for size 100...
Baseline for (doall (distinct coll))		 12.470999 µs
Baseline for (doall (sequence (distinct) coll))	 8.668717 µs
Baseline for (into [] (distinct) coll)		 7.683792 µs
Testing...

Removing laziness comparison:
(doall (d/distinct-nolazy coll))			  12.470999 µs=>8.785739 µs(-29%)

Transducer comparisons:
(doall (sequence (d/distinct-nocontains) coll))		  8.668717 µs=>6.890167 µs(-20%)
(into [] (d/distinct-nocontains) coll)			  7.683792 µs=>5.766337 µs(-24%)

(doall (sequence (d/distinct-transient) coll))		  8.668717 µs=>7.441956 µs(-14%)
(into [] (d/distinct-transient) coll)			  7.683792 µs=>6.233142 µs(-18%)

(doall (sequence (d/distinct-transient-nocontains) coll)) 8.668717 µs=>6.469240 µs(-25%)
(into [] (d/distinct-transient-nocontains) coll)	  7.683792 µs=>5.243282 µs(-31%)

(doall (sequence (d/distinct-raw-transient) coll))	  8.668717 µs=>6.070465 µs(-29%)
(into [] (d/distinct-raw-transient) coll)		  7.683792 µs=>4.595752 µs(-40%)

Running benchmark for size 1000...
Baseline for (doall (distinct coll))		 132.355828 µs
Baseline for (doall (sequence (distinct) coll))	 91.329531 µs
Baseline for (into [] (distinct) coll)		 83.965102 µs
Testing...

Removing laziness comparison:
(doall (d/distinct-nolazy coll))			  132.355828 µs=>92.775722 µs(-29%)

Transducer comparisons:
(doall (sequence (d/distinct-nocontains) coll))		  91.329531 µs=>73.107234 µs(-19%)
(into [] (d/distinct-nocontains) coll)			  83.965102 µs=>62.585374 µs(-25%)

(doall (sequence (d/distinct-transient) coll))		  91.329531 µs=>72.886548 µs(-20%)
(into [] (d/distinct-transient) coll)			  83.965102 µs=>65.489080 µs(-22%)

(doall (sequence (d/distinct-transient-nocontains) coll)) 91.329531 µs=>64.595585 µs(-29%)
(into [] (d/distinct-transient-nocontains) coll)	  83.965102 µs=>58.128802 µs(-30%)

(doall (sequence (d/distinct-raw-transient) coll))	  91.329531 µs=>62.453381 µs(-31%)
(into [] (d/distinct-raw-transient) coll)		  83.965102 µs=>51.001812 µs(-39%)

Running benchmark for size 10000...
Baseline for (doall (distinct coll))		 826.316599 µs
Baseline for (doall (sequence (distinct) coll))	 425.762721 µs
Baseline for (into [] (distinct) coll)		 401.456068 µs
Testing...

Removing laziness comparison:
(doall (d/distinct-nolazy coll))			  826.316599 µs=>442.612112 µs(-46%)

Transducer comparisons:
(doall (sequence (d/distinct-nocontains) coll))		  425.762721 µs=>368.402848 µs(-13%)
(into [] (d/distinct-nocontains) coll)			  401.456068 µs=>330.489260 µs(-17%)

(doall (sequence (d/distinct-transient) coll))		  425.762721 µs=>444.146335 µs(4%)
(into [] (d/distinct-transient) coll)			  401.456068 µs=>396.619772 µs(-1%)

(doall (sequence (d/distinct-transient-nocontains) coll)) 425.762721 µs=>475.110424 µs(11%)
(into [] (d/distinct-transient-nocontains) coll)	  401.456068 µs=>461.110783 µs(14%)

(doall (sequence (d/distinct-raw-transient) coll))	  425.762721 µs=>324.163292 µs(-23%)
(into [] (d/distinct-raw-transient) coll)		  401.456068 µs=>279.295872 µs(-30%)

Running benchmark for size 1000000...
Baseline for (doall (distinct coll))		 65.214443 ms
Baseline for (doall (sequence (distinct) coll))	 31.084661 ms
Baseline for (into [] (distinct) coll)		 31.327697 ms
Testing...

Removing laziness comparison:
(doall (d/distinct-nolazy coll))			  65.214443 ms=>32.314287 ms(-50%)

Transducer comparisons:
(doall (sequence (d/distinct-nocontains) coll))		  31.084661 ms=>28.375289 ms(-8%)
(into [] (d/distinct-nocontains) coll)			  31.327697 ms=>27.292067 ms(-12%)

(doall (sequence (d/distinct-transient) coll))		  31.084661 ms=>37.918721 ms(21%)
(into [] (d/distinct-transient) coll)			  31.327697 ms=>34.144261 ms(8%)

(doall (sequence (d/distinct-transient-nocontains) coll)) 31.084661 ms=>42.331066 ms(36%)
(into [] (d/distinct-transient-nocontains) coll)	  31.327697 ms=>41.973133 ms(33%)

(doall (sequence (d/distinct-raw-transient) coll))	  31.084661 ms=>25.404296 ms(-18%)
(into [] (d/distinct-raw-transient) coll)		  31.327697 ms=>23.351939 ms(-25%)
```

## Discussion
Removing laziness, as per `distinct-nolazy`, is consistently faster at every size of input. Small input sets show the smallest benefit, but this is still approximately 20% faster than the original code. As the input size increases, the benefit of avoiding the lazy-seq becomes more pronounced. Based on this, each of the other implementations also adopt the transducer approach for the single-arity version.

Using `distinct-nocontains` has a performance improvement at every size, with improvements greatest for small sequences.. The default is to think of transients being faster, but avoiding the double-checking apparently provides a similar benefit. It also feels slightly "cleaner" and more idiomatic to use a persistent set rather than a transient one, particularly as the transitient collection is never made persistent again. Unfortunately, it depends on sets not changing when an existing entry is added. Clojure depends on this internally, but it is not part of the published interface.

Both `distinct-transient` and `distinct-transient-nocontains` look very good over small sequences, with the "no contains" approach performing much better than the basic transient set approach of checking for containership first. However, for sequences of size 10K and above the improvements start to disappear, and eventually these functions take longer than the existing approach. I can't say exactly what is happenening here without a full profile, but I can speculate that perhaps the internal embedded calls to `ATransientMap.ensureEditable()` are adding up. Meanwhile, the persistent set fills up rapidly, and most of the sequence is processed without needing to update the structure. The code path here is very short, as it returns as soon as it discovers the entry already exists.

The `distinct-raw-transient` code is the fastest, but it is also the least idiomatic. It relies on an internal implementation detail of the `ITransientSet` interface, and does not use the `contains?` function that was explicitly referenced in earlier discussions, and the reason for this code being delayed. Aside from skipping some of the code path used by the `contains?` function, I am surprised that this function outperforms everything else. If the Clojure maintainers are OK with it, then this is the fastest option.

While all of the functions, except `distinct-raw-transient`, have been tested on ClojureScript, performance for that platform has not been considered.

## Conclusion
Using `(sequence (distinct) coll)` in the single-arity code is both idiomatic to Clojure and provides a clear benefit in performance, regardless of the implementation of the transducer. This appears to be a clear win.

Getting a clear comparison of the remaining functions therefore requires that only the transduced versions be compared, which the tests show.

Inserting into a persistent set without checking if an element exists first provides from 8%-28% speed improvement compared to the baseline depending on the sequence size. While not a documented feature of `PersistentHashSet`s, this may be considered acceptable for internal code.

Using a transient set via the public `contains?` function turns out to be slower for large sequences when the set usually returns `true` to this function. However, casting to the `ITransientSet` interface provides speed improvement across the board. There is precedent for casting like this in `clojure.core`. However, the conversation some years ago suggested that the core team may prefer to rely on the `clojure.core/contains?` function.


