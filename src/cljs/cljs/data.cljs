(ns cljs.data)

(defprotocol IEncodeJS
  (-clj->js [x] "Recursively transforms clj values to JavaScript")
  (-key->js [x] "Transforms map keys to valid JavaScript keys. Arbitrary keys are
  encoded to their string representation via (pr-str x)"))

(extend-protocol IEncodeJS
  default
  (-key->js [k]
    (if (or (string? k)
            (number? k)
            (keyword? k)
            (symbol? k))
      (-clj->js k)
      (pr-str k)))

  (-clj->js [x]
    (cond
      (keyword? x) (name x)
      (symbol? x) (str x)
      (map? x) (let [m (js-obj)]
                 (doseq [[k v] x]
                   (aset m (-key->js k) (-clj->js v)))
                 m)
      (coll? x) (apply array (map -clj->js x))
      :else x))

  nil
  (-clj->js [x] nil))

(defn clj->js
   "Recursively transforms ClojureScript values to JavaScript.
sets/vectors/lists become Arrays, Keywords and Symbol become Strings,
Maps become Objects. Arbitrary keys are encoded to by key->js."
   [x]
   (-clj->js x))

(defprotocol IEncodeClojure
  (-js->clj [x] [x options] "Transforms JavaScript values to Clojure"))

(extend-protocol IEncodeClojure
  default
  (-js->clj
    ([x options]
       (let [{:keys [keywordize-keys]} options
             keyfn (if keywordize-keys keyword str)
             f (fn thisfn [x]
                 (cond
                   (seq? x) (doall (map thisfn x))
                   (coll? x) (into (empty x) (map thisfn x))
                   (goog.isArray x) (vec (map thisfn x))
                   (identical? (type x) js/Object) (into {} (for [k (js-keys x)]
                                                              [(keyfn k)
                                                               (thisfn (aget x k))]))
                   :else x))]
         (f x)))
    ([x] (-js->clj x {:keywordize-keys false}))))

(defn js->clj
  "Recursively transforms JavaScript arrays into ClojureScript
  vectors, and JavaScript objects into ClojureScript maps.  With
  option ':keywordize-keys true' will convert object fields from
  strings to keywords."
  [x & opts]
  (-js->clj x (apply array-map opts)))
