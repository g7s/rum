(ns ^:no-doc rum.derived-atom)


(defn derived-atom
  ([refs key f]
   (derived-atom refs key f {:eq-fn =}))
  ([refs key f opts]
   (let [calc  (case (count refs)
                 1 (let [[a] refs] #(f @a))
                 2 (let [[a b] refs] #(f @a @b))
                 3 (let [[a b c] refs] #(f @a @b @c))
                 #(apply f (map deref refs)))
         sink  (if (:ref opts)
                 (doto (:ref opts) (reset! (calc)))
                 (atom (calc)))
         watch (fn [_ _ _ _]
                 (let [new-val (calc)]
                   (when-not ((:eq-fn opts) @sink new-val)
                     (reset! sink new-val))))]
     (doseq [ref refs]
       (add-watch ref key watch))
     sink)))
