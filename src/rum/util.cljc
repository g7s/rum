(ns ^:no-doc rum.util)


(defn collect [key mixins]
  (into []
        (keep (fn [m] (get m key)))
        mixins))


(defn collect* [keys mixins]
  (into []
        (mapcat (fn [m] (keep (fn [k] (get m k)) keys)))
        mixins))


(defn call-all [state fns & args]
  (reduce
    (fn [state fn]
      (apply fn state args))
    state
    fns))


#?(:cljs
   (do
     (defprotocol ToArrayDeps
       (to-array-deps [this]))


     (extend-protocol ToArrayDeps
       nil
       (to-array-deps [_] #js [])
       array
       (to-array-deps [this] this)
       boolean
       (to-array-deps [this] (if (true? this) nil #js []))
       cljs.core/ISequential
       (to-array-deps [this] (into-array this))
       default
       (to-array-deps [this] #js [this]))


     (defn create-hook
       [react-hook hook-impl-fn]
       (fn hook
         ([f]
          (react-hook (hook-impl-fn f) nil))
         ([f deps]
          (react-hook (hook-impl-fn f) (to-array-deps deps)))))


     (defn eq-props?
       [eq-fn old-props new-props]
       (let [old-keys (.keys js/Object old-props)
             new-keys (.keys js/Object new-props)]
         (and (identical? (alength old-keys) (alength new-keys))
              (reduce (fn [r k]
                        (and r (eq-fn (unchecked-get new-props k) (unchecked-get old-props k))))
                      true
                      new-keys))))))
