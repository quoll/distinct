# distinct
Various reimplementations of clojure.core/distinct.

Benchmarking is based on the example from @tonsky in [CLJ-2090](https://clojure.atlassian.net/browse/CLJ-2090).

# Issues
The single-arity version of the function `clojure.core/distinct` uses a `lazy-seq` which is not as efficient as a transducer based approach can be. By looking at this, it also seems possible to improve the performanc of the transducer code by using a transient value.

This repository contains various reimplementations of `clojure.core/distinct` and benchmarks them.

## Approaches
### `distinct-nolazy`
The first approach is to leave the transducer alone, and only replace the single-arity version. This uses a `sequence` call, so that the result still appears to be lazy.

### `distinct-contain`
The second approach does not move to a transient, but instead tries to reduce work done with the set of "seen" values. The original code checks if the value is in the set, and if not then it gets added. However, a `conj` operation will need to do the same check again internally. This approach adds the value to the set, and then uses `identical?` to check if the set changed.

Unfortunately, this does not work the same way in ClojureScript, so in that case it falls back to checking the count of the set.

### `distinct-transient`
The third approach is to use a transient set. This should be faster than a non-transient version since transient sets often do not need to allocate new memory and perform copies. This was the original approach taken by @tonsky way back in 2016. However, at the time transient sets did not respond to the `contains?` function, and the proposed change was pushed back until this had been addressed.

This approach cannot easily avoid the double-checking that `distinct-contain` can, since transient objects are typically mutated in place, meaning that object reference comparisons will not usually see a change.

### `distinct-raw-transient`
The fourth approach uses a transient set the same as `distinct-transient`, but rather than using the newly supported `contains?` method, it calls the native `.contains` method on the `ITransientSet` interface. This skips a couple of internal checks on the datatype that occur in `clojure.lang.RT/contains(Object,Object)`. This would not typically be recommended, but if it were hidden in the core libraries then perhaps it might be acceptable.

## Benchmarks
Using the same benchmarking approach adopted by @tonsky in [CLJ-2090](https://clojure.atlassian.net/browse/CLJ-2090), the existing `clojure.core/distinct` function is compared to each of the new implementations, using pregressively larger sequences of random numbers.

The benchmarks can be run using:
```bash
clj -T:bench
```

### Results
```
Running benchmark for size 10...
Testing...
(doall (d/distinct-raw-transient coll)) 	 1.359962 µs => 655.074957 ns (-51%)
(into [] (d/distinct-raw-transient coll)) 	 1.427701 µs => 636.514539 ns (-55%)

(doall (d/distinct-transient coll)) 	 1.359962 µs => 733.525693 ns (-46%)
(into [] (d/distinct-transient coll)) 	 1.427701 µs => 709.567913 ns (-50%)

(doall (d/distinct-contain coll)) 	 1.359962 µs => 741.093792 ns (-45%)
(into [] (d/distinct-contain coll)) 	 1.427701 µs => 669.958125 ns (-53%)

(doall (d/distinct-nolazy coll)) 	 1.359962 µs => 990.944933 ns (-27%)
(into [] (d/distinct-nolazy coll)) 	 1.427701 µs => 906.783336 ns (-36%)

Running benchmark for size 100...
Testing...
(doall (d/distinct-raw-transient coll)) 	 13.249275 µs => 7.009062 µs (-47%)
(into [] (d/distinct-raw-transient coll)) 	 14.196480 µs => 6.685684 µs (-52%)

(doall (d/distinct-transient coll)) 	 13.249275 µs => 7.969190 µs (-39%)
(into [] (d/distinct-transient coll)) 	 14.196480 µs => 8.020656 µs (-43%)

(doall (d/distinct-contain coll)) 	 13.249275 µs => 7.335320 µs (-44%)
(into [] (d/distinct-contain coll)) 	 14.196480 µs => 7.596712 µs (-46%)

(doall (d/distinct-nolazy coll)) 	 13.249275 µs => 9.669858 µs (-27%)
(into [] (d/distinct-nolazy coll)) 	 14.196480 µs => 9.831380 µs (-30%)

Running benchmark for size 1000...
Testing...
(doall (d/distinct-raw-transient coll)) 	 131.251459 µs => 61.831191 µs (-52%)
(into [] (d/distinct-raw-transient coll)) 	 136.831951 µs => 61.610665 µs (-54%)

(doall (d/distinct-transient coll)) 	 131.251459 µs => 76.296806 µs (-41%)
(into [] (d/distinct-transient coll)) 	 136.831951 µs => 75.848436 µs (-44%)

(doall (d/distinct-contain coll)) 	 131.251459 µs => 74.115683 µs (-43%)
(into [] (d/distinct-contain coll)) 	 136.831951 µs => 74.460207 µs (-45%)

(doall (d/distinct-nolazy coll)) 	 131.251459 µs => 93.357999 µs (-28%)
(into [] (d/distinct-nolazy coll)) 	 136.831951 µs => 92.817790 µs (-32%)

Running benchmark for size 10000...
Testing...
(doall (d/distinct-raw-transient coll)) 	 785.329010 µs => 342.665183 µs (-56%)
(into [] (d/distinct-raw-transient coll)) 	 777.811676 µs => 334.067827 µs (-57%)

(doall (d/distinct-transient coll)) 	 785.329010 µs => 460.312765 µs (-41%)
(into [] (d/distinct-transient coll)) 	 777.811676 µs => 457.581314 µs (-41%)

(doall (d/distinct-contain coll)) 	 785.329010 µs => 368.617139 µs (-53%)
(into [] (d/distinct-contain coll)) 	 777.811676 µs => 364.723872 µs (-53%)

(doall (d/distinct-nolazy coll)) 	 785.329010 µs => 421.054514 µs (-46%)
(into [] (d/distinct-nolazy coll)) 	 777.811676 µs => 420.043298 µs (-45%)

Running benchmark for size 1000000...
Testing...
(doall (d/distinct-raw-transient coll)) 	 66.279006 ms => 26.137720 ms (-60%)
(into [] (d/distinct-raw-transient coll)) 	 65.804700 ms => 26.108728 ms (-60%)

(doall (d/distinct-transient coll)) 	 66.279006 ms => 39.824934 ms (-39%)
(into [] (d/distinct-transient coll)) 	 65.804700 ms => 39.697564 ms (-39%)

(doall (d/distinct-contain coll)) 	 66.279006 ms => 28.282756 ms (-57%)
(into [] (d/distinct-contain coll)) 	 65.804700 ms => 28.410563 ms (-56%)

(doall (d/distinct-nolazy coll)) 	 66.279006 ms => 31.526072 ms (-52%)
(into [] (d/distinct-nolazy coll)) 	 65.804700 ms => 31.305035 ms (-52%)
```

## Discussion
The `distinct-nolazy` code (shown last in each iteration) is consistently faster at every size of input. Small input sets show the smallest benefit, but this is still approximately 25% faster than the original code. As the input size increases, the benefit of avoiding the lazy-seq becomes more pronounced. Based on this, each of the other implementations also adopt the transducer approach for the single-arity version.

Using `distinct-contain` actually does a little better than using a transient set, at every size, though the difference is minor. The default is to think of transients being faster, but avoiding the double-checking apparently provides a similar benefit. It also feels slightly "cleaner" and more idiomatic to use a persistent set rather than a transient one, particularly as the transitient collection is never made persistent again.

The `distinct-raw-transient` code is the fastest, but it is also the least idiomatic. It relies on an internal implementation detail of the `ITransientSet` interface, and does not use the `contains?` function that was explicitly referenced in earlier discussions, and the reason for this code being delayed. If the Clojure maintainers are OK with it, then this is the fastest option.

While all of the functions, except `distinct-raw-transient`, have been tested on ClojureScript, performance here has not been considered.

## Conclusion
Using `(sequence (distinct) coll)` in the single-arity code is both idiomatic to Clojure and provides a clear benefit in performance.

Updating the transducer code to use a transient does not make sense unless casting to the `ITransientSet` interface is considered acceptable. Otherwise, using a persistent set and avoiding double-checking provides slightly better performance, though the code does need to look slightly different in ClojureScript.


