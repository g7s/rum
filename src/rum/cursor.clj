(ns ^:no-doc rum.cursor)


(deftype Cursor [ref path ^:volatile-mutable meta watches]
  clojure.lang.IDeref
  (deref [_]
    ((::read meta get-in) (deref ref) path))

  clojure.lang.IRef
  (setValidator [_ _]
    (throw (UnsupportedOperationException. "rum.cursor.Cursor/setValidator")))
  (getValidator [_]
    (throw (UnsupportedOperationException. "rum.cursor.Cursor/getValidator")))
  (getWatches [_] @watches)
  (addWatch [this key callback]
    (vswap! watches assoc key callback)
    (add-watch ref (list this key)
      (fn [_ _ oldv newv]
        (let [old ((::read meta get-in) oldv path)
              new ((::read meta get-in) newv path)]
          (when (not= old new)
            (callback key this old new)))))
    this)
  (removeWatch [this key]
    (vswap! watches dissoc key)
    (remove-watch ref (list this key))
    this)

  clojure.lang.IAtom
  (swap [_ f]
    (-> (swap! ref (::write meta update-in) path f)
        ((::read meta get-in) path)))
  (swap [_ f a]
    (-> (swap! ref (::write meta update-in) path f a)
        ((::read meta get-in) path)))
  (swap [_ f a b]
    (-> (swap! ref (::write meta update-in) path f a b)
        ((::read meta get-in) path)))
  (swap [_ f a b rest]
    (-> (apply swap! ref (::write meta update-in) path f a b rest)
        ((::read meta get-in) path)))
  (compareAndSet [this oldv newv]
    (loop []
      (let [refv @ref]
        (if (not= oldv ((::read meta get-in) refv path))
          false
          (or (compare-and-set! ref refv ((::write meta update-in) refv path (constantly newv)))
              (recur))))))
  (reset [this newv]
    (swap! ref (::write meta update-in) path (constantly newv))
    newv)

  clojure.lang.IObj
  (meta [_] meta)
  (withMeta [_ m]
    (Cursor. ref path m (volatile! {})))

  clojure.lang.IReference
  (alterMeta [this f args]
    (.resetMeta this (apply f meta args)))
  (resetMeta [_ m]
    (set! meta m)
    m))
