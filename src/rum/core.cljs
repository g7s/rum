(ns rum.core
  (:refer-clojure :exclude [ref])
  (:require-macros rum.core)
  (:require
    [cljsjs.react]
    [cljsjs.react.dom]
    [goog.object :as gobj]
    [sablono.core]
    [rum.cursor :as cursor]
    [rum.util :as util :refer [collect collect* call-all]]
    [rum.derived-atom :as derived-atom]))


(defn state
  "Given React component, return Rum state associated with it."
  [comp]
  (gobj/get (.-state comp) ":rum/state"))


(defn react-component
  "Given rum `state` return the react component."
  [state]
  (:rum/react-component state))


(defn rum-args
  [state]
  (:rum/args state))


(defn- extend! [obj props]
  (doseq [[k v] props
          :when (some? v)]
    (gobj/set obj (name k) (clj->js v))))


(defn- build-class [render mixins display-name]
  (let [init           (collect   :init mixins)             ;; state props -> state
        derive-state   (collect   :derive-state mixins)     ;; state -> state
        render         render                               ;; state -> [dom state]
        wrap-render    (collect   :wrap-render mixins)      ;; render-fn -> render-fn
        wrapped-render (reduce #(%2 %1) render wrap-render)
        did-mount      (collect   :did-mount mixins)        ;; state -> state
        after-mount    (collect   :after-render mixins)     ;; state snapshot -> state
        should-update  (collect   :should-update mixins)    ;; old-state state -> boolean
        did-update     (collect* [:did-update               ;; state snapshot -> state
                                  :after-render] mixins)    ;; state snapshot -> state
        make-snapshot  (collect   :make-snapshot mixins)    ;; state -> snapshot
        did-catch      (collect   :did-catch mixins)        ;; state error info -> state
        will-unmount   (collect   :will-unmount mixins)     ;; state -> state
        class-props    (reduce merge (collect :class-properties mixins))  ;; custom prototype properties and methods
        static-props   (reduce merge (collect :static-properties mixins)) ;; custom static properties and methods

        ctor           (fn [props]
                         (this-as this
                           (gobj/set this "state"
                             #js {":rum/state"
                                  (-> (gobj/get props ":rum/initial-state")
                                      (assoc :rum/react-component this)
                                      (call-all init props)
                                      volatile!)})
                           (.call js/React.Component this props)))
        _              (goog/inherits ctor js/React.Component)
        prototype      (gobj/get ctor "prototype")]

    (extend! prototype class-props)
    (extend! ctor static-props)

    (gobj/set ctor "displayName" display-name)

    (gobj/set ctor "getDerivedStateFromProps"
      (fn [next-props state]
        (let [old-state  @(gobj/get state ":rum/state")
              state      (merge old-state (gobj/get next-props ":rum/initial-state"))
              next-state (call-all state derive-state)]
          ;; allocate new volatile
          ;; so that we can access both old and new states in shouldComponentUpdate
          #js {":rum/state" (volatile! next-state)})))

    (gobj/set prototype "render"
      (fn []
        (this-as this
          (let [state (state this)
                [dom next-state] (wrapped-render @state)]
            (vreset! state next-state)
            dom))))

    (gobj/set prototype "componentWillUnmount"
      (fn []
        (this-as this
          (when (seq will-unmount)
            (vswap! (state this) call-all will-unmount))
          (gobj/set this ":rum/unmounted?" true))))

    (when (or (seq did-mount) (seq after-mount))
      (gobj/set prototype "componentDidMount"
        (fn []
          (this-as this
            (let [next-state (-> @(state this)
                                 (call-all did-mount)
                                 (call-all after-mount nil))]
              (vreset! (state this) next-state))))))

    (when (seq should-update)
      (gobj/set prototype "shouldComponentUpdate"
        (fn [next-props next-state]
            (this-as this
              (let [old-state @(state this)
                    new-state @(gobj/get next-state ":rum/state")]
                (boolean (some #(% old-state new-state) should-update)))))))

    (when (seq make-snapshot)
      (gobj/set prototype "getSnapshotBeforeUpdate"
        (fn [props state]
          (let [old-state  @(gobj/get state ":rum/state")
                state      (merge old-state (gobj/get props ":rum/initial-state"))]
            (call-all state make-snapshot)))))

    (when (seq did-update)
      (gobj/set prototype "componentDidUpdate"
        (fn [_ _ snapshot]
          (this-as this
            (vswap! (state this) call-all did-update snapshot)))))

    (when (seq did-catch)
      (gobj/set prototype "componentDidCatch"
        (fn [error info]
          (this-as this
            (vswap! (state this) call-all did-catch error {:rum/component-stack (gobj/get info "componentStack")})
            (.forceUpdate this)))))

    ctor))


(defn- build-ctor
  [render mixins display-name]
  (let [class  (build-class render mixins display-name)
        key-fn (first (collect :key-fn mixins))
        ctor   (if (some? key-fn)
                 (fn [& args]
                   (let [props #js { ":rum/initial-state" { :rum/args args }
                                     "key" (apply key-fn args) }]
                     (js/React.createElement class props)))
                 (fn [& args]
                   (let [props #js { ":rum/initial-state" { :rum/args args }}]
                     (js/React.createElement class props))))]
    (with-meta ctor { :rum/class class })))


(defn ^:no-doc build-defnc
  [render-body wrappers display-name]
  (let [gwr  (group-by #(:wrap % :component) wrappers)
        cwr  (map (some-fn :fn identity) (:component gwr))
        pwr  (map :fn (:props gwr))
        fnc  (fn [props]
               (apply render-body (aget props ":rum/args")))
        fnc  (reduce #(%2 %1) fnc cwr)
        _    (aset fnc "displayName" display-name)
        ctor (if (seq pwr)
               (fn [& args]
                 (js/React.createElement fnc (-> { ":rum/args" args }
                                                 (call-all pwr)
                                                 util/map->obj)))
               (fn [& args]
                 (js/React.createElement fnc #js { ":rum/args" args })))]
    (with-meta ctor { :rum/class fnc })))


(defn ^:no-doc build-defc
  [render-body mixins display-name]
  (if (empty? mixins)
    (let [class (fn [props]
                  (apply render-body (aget props ":rum/args")))
          _     (aset class "displayName" display-name)
          ctor  (fn [& args]
                  (js/React.createElement class #js { ":rum/args" args }))]
      (with-meta ctor { :rum/class class }))
    (let [render (fn [state] [(apply render-body (:rum/args state)) state])]
      (build-ctor render mixins display-name))))


(defn ^:no-doc build-defcs
  [render-body mixins display-name]
  (let [render (fn [state] [(apply render-body state (rum-args state)) state])]
    (build-ctor render mixins display-name)))


(def ^:private schedule
  (or (and (exists? js/window)
           (or js/window.requestAnimationFrame
               js/window.webkitRequestAnimationFrame
               js/window.mozRequestAnimationFrame
               js/window.msRequestAnimationFrame))
      #(js/setTimeout % 16)))


(def ^:private batch
  (or (when (exists? js/ReactNative) js/ReactNative.unstable_batchedUpdates)
      (when (exists? js/ReactDOM) js/ReactDOM.unstable_batchedUpdates)
      (fn [f a] (f a))))


(def ^:private empty-queue [])
(def ^:private render-queue (volatile! empty-queue))


(defn- render-all [queue]
  (doseq [render-fn queue]
    (render-fn)))


(defn- render []
  (let [queue @render-queue]
    (vreset! render-queue empty-queue)
    (batch render-all queue)))


(defn schedule-render
  [render-fn]
  (when (empty? @render-queue)
    (schedule render))
  (vswap! render-queue conj render-fn))


(defn request-render
  "Schedules react component to be rendered on next animation frame."
  [state]
  (let [comp (react-component state)]
    (schedule-render (fn []
                       (when (and (some? comp)
                                  (not (gobj/get comp ":rum/unmounted?")))
                         (.forceUpdate comp))))))


(defn force-render
  [state]
  (.forceUpdate (react-component state)))


(defn mount
  "Add element to the DOM tree. Idempotent. Subsequent mounts will just update element."
  [element node]
  (js/ReactDOM.render element node)
  nil)


(defn unmount
  "Removes component from the DOM tree."
  [node]
  (js/ReactDOM.unmountComponentAtNode node))


(defn hydrate
  "Same as [[mount]] but must be called on DOM tree already rendered by a server via [[render-html]]."
  [element node]
  (js/ReactDOM.hydrate element node))


(defn portal
  "Render `element` in a DOM `node` that is ouside of current DOM hierarchy."
  [element node]
  (js/ReactDOM.createPortal element node))


(defn create-context
  "Create an instance of React Context."
  [value]
  (js/React.createContext value))


(defn provide-context
  "Provide a `value` to consumers in UI subtree via React’s Context API."
  [ctx value & children]
  (apply js/React.createElement (.-Provider ctx) #js {:value value} children))


(defn with-context
  "Subscribe UI subtree to context changes.
  Call `render-child` everytime a new value gets added into context via `Provider`."
  [ctx render-child]
  (js/React.createElement (.-Consumer ctx) nil #(render-child %)))


(defn dom-node
  "Given `state` return top-level DOM node of component."
  [state]
  (js/ReactDOM.findDOMNode (react-component state)))


(defn create-ref
  []
  (js/React.createRef))


(defn ref-val
  "Given a react `ref` return its current value."
  [ref]
  (unchecked-get ref "current"))


(defn set-ref-val!
  [ref val]
  (unchecked-set ref "current" val))


(defn with-key
  "Adds React key to element.

   ```
   (rum/defc label [text] [:div text])

   (-> (label)
       (rum/with-key \"abc\")
       (rum/mount js/document.body))
   ```"
  [element key]
  (js/React.cloneElement element #js { "key" key }))


(defn with-ref
  "Adds React ref (string or callback) to element.

   ```
   (rum/defc label [text] [:div text])

   (-> (label)
       (rum/with-ref \"abc\")
       (rum/mount js/document.body))
   ```"
  [element ref]
  (js/React.cloneElement element #js { "ref" ref }))


;; static mixin

(def static
  "Mixin. Will avoid re-render if none of component’s arguments have changed. Does equality check (`=`) on all arguments.

   ```
   (rum/defc label < rum/static
     [text]
     [:div text])

   (rum/mount (label \"abc\") js/document.body)

   ;; def != abc, will re-render
   (rum/mount (label \"def\") js/document.body)

   ;; def == def, won’t re-render
   (rum/mount (label \"def\") js/document.body)
   ```"
  {:should-update
   (fn [old-state new-state]
     (not= (rum-args old-state) (rum-args new-state)))})


;; local mixin

(defn local
  "Mixin constructor. Adds an atom to component’s state that can be used to keep stuff during component’s lifecycle. Component will be re-rendered if atom’s value changes. Atom is stored under user-provided key or under `:rum/local` by default.

   ```
   (rum/defcs counter < (rum/local 0 :cnt)
     [state label]
     (let [*cnt (:cnt state)]
       [:div {:on-click (fn [_] (swap! *cnt inc))}
         label @*cnt]))

   (rum/mount (counter \"Click count: \"))
   ```"
  ([]
   (local {}))
  ([initial]
   (local initial :rum/local))
  ([initial key]
   {:init
    (fn [state props]
      (let [local-state (atom initial)]
        (add-watch local-state key
                   (fn [_ _ _ _]
                     (request-render state)))
        (assoc state key local-state)))
    :will-unmount
    (fn [state]
      (remove-watch (get state key) key)
      (dissoc state key))}))


;; reactive mixin

(def ^:private ^:dynamic *reactions*)


(def reactive
  "Mixin. Works in conjunction with [[react]].

   ```
   (rum/defc comp < rum/reactive
     [*counter]
     [:div (rum/react counter)])

   (def *counter (atom 0))
   (rum/mount (comp *counter) js/document.body)
   (swap! *counter inc) ;; will force comp to re-render
   ```"
  {:init
   (fn [state props]
     (assoc state :rum.reactive/key (gensym "reactive")))
   :wrap-render
   (fn [render-fn]
     (fn [state]
       (binding [*reactions* (volatile! #{})]
         (let [old-reactions    (:rum.reactive/refs state #{})
               [dom next-state] (render-fn state)
               new-reactions    @*reactions*
               key              (:rum.reactive/key state)]
           (doseq [ref old-reactions]
             (when-not (contains? new-reactions ref)
               (remove-watch ref key)))
           (doseq [ref new-reactions]
             (when-not (contains? old-reactions ref)
               (add-watch ref key
                          (fn [_ _ _ _]
                            (request-render state)))))
           [dom (assoc next-state :rum.reactive/refs new-reactions)]))))
   :will-unmount
   (fn [state]
     (let [key (:rum.reactive/key state)]
       (doseq [ref (:rum.reactive/refs state)]
         (remove-watch ref key)))
     (dissoc state :rum.reactive/refs :rum.reactive/key))})


(defn react
  "Works in conjunction with [[reactive]] mixin. Use this function instead of `deref` inside render, and your component will subscribe to changes happening to the derefed atom."
  [ref]
  (assert *reactions* "rum.core/react is only supported in conjunction with rum.core/reactive")
  (vswap! *reactions* conj ref)
  @ref)


;; hooks

(def use-ref js/React.useRef)


(defn use-state
  [initial]
  (let [[state set-state!] (js/React.useState initial)]
    (reify
      cljs.core/IReset
      (-reset! [_ new-value]
        (set-state! new-value))

      cljs.core/ISwap
      (-swap! [_ f]
        (set-state! f))
      (-swap! [_ f x]
        (set-state! #(f % x)))
      (-swap! [_ f x y]
        (set-state! #(f % x y)))
      (-swap! [_ f x y more]
        (set-state! #(apply f % x y more)))

      cljs.core/IDeref
      (-deref [self] state))))


(defn use-var
  "A hook to define mutable variables that persist between renders (based on useRef hook)."
  [initial]
  (let [ref (js/React.useRef initial)]
    (reify
      cljs.core/IReset
      (-reset! [_ new-value]
        (set-ref-val! ref new-value))

      cljs.core/ISwap
      (-swap! [_ f]
        (set-ref-val! ref (f (ref-val ref))))
      (-swap! [_ f x]
        (set-ref-val! ref (f (ref-val ref) x)))
      (-swap! [_ f x y]
        (set-ref-val! ref (f (ref-val ref) x y)))
      (-swap! [_ f x y more]
        (set-ref-val! ref (apply f (ref-val ref) x y more)))

      cljs.core/IDeref
      (-deref [_] (ref-val ref)))))


(defn- make-effect-fn
  [f]
  (fn []
    (let [res (f)]
      (fn []
        (when (fn? res) (res))))))


(defn use-effect
  ([f]
   (js/React.useEffect (make-effect-fn f) nil))
  ([f deps]
   (js/React.useEffect (make-effect-fn f) (util/to-array-deps deps))))


(defn use-layout-effect
  ([f]
   (js/React.useLayoutEffect (make-effect-fn f) nil))
  ([f deps]
   (js/React.useLayoutEffect (make-effect-fn f) (util/to-array-deps deps))))


(def use-context js/React.useContext)


(defn use-memo
  [f deps]
  (js/React.useMemo f (util/to-array-deps deps)))


(defn use-callback
  [f deps]
  (js/React.useCallback f (util/to-array-deps deps)))


(defn use-render
  "Hook that returns a function that when called will schedule a re-render of the component."
  []
  (let [unmounted (js/React.useRef false)
        [_ set-s!] (js/React.useState false)]
    (js/React.useEffect #(fn [] (set-ref-val! unmounted true)) #js [])
    (fn []
      (schedule-render #(when-not (ref-val unmounted)
                          (set-s! not))))))


(defn use-react-when
  "Re-render component when `iref` changes but only when `c` is truthy.
  Same as (when c (use-react iref))."
  [c iref]
  (let [key     (ref-val (js/React.useRef (gensym "use-react")))
        render! (use-render)]
    (js/React.useEffect
     (fn []
       (when c
         (add-watch iref key render!))
       (fn []
         (when c
           (remove-watch iref key))))
     #js [c iref])
    (when c @iref)))


(defn use-react
  [iref]
  (use-react-when true iref))


(defn use-atom
  "Create an atom that persists between renders and remove any watches on unmount."
  [val]
  (let [patom (ref-val (use-ref (atom val)))]
    (use-effect
     #(fn []
        (doseq [[watch-key _] (.-watches patom)]
          (remove-watch patom watch-key)))
     [])
    patom))


(defn use-derived-atom
  "Create a derived-atom that persists between renders and remove the watch on unmount."
  {:style/indent 2}
  ([refs key f]
   (use-derived-atom refs key f []))
  ([refs key f deps]
   (use-derived-atom refs key f deps {:eq-fn =}))
  ([refs key f deps opts]
   (let [all-deps (into refs deps)
         calc     (use-memo
                   (fn []
                     (case (count refs)
                       1 (let [[a] refs] #(f @a))
                       2 (let [[a b] refs] #(f @a @b))
                       3 (let [[a b c] refs] #(f @a @b @c))
                       #(apply f (map deref refs))))
                   all-deps)
         sink     (use-memo
                   (fn []
                     (atom (calc)))
                   [calc])
         watch    (use-callback
                   (fn [_ _ _ _]
                     (let [new-val (calc)]
                       (when-not ((:eq-fn opts) @sink new-val)
                         (reset! sink new-val))))
                   [sink (:eq-fn opts)])]
     (use-effect
      (fn []
        (doseq [ref refs]
          (add-watch ref key watch))
        (fn []
          (doseq [ref refs]
            (remove-watch ref key))))
      [watch])
     sink)))


;; wrappers

;; A wrapper is a map with a :wrap key that specifies the type of wrap
;; (it can be either :component or :props default :component) and a
;; :fn key that holds the wrapping function.
;; Wrappers can be fed (<) into a defnc component.

(defn wrap-memo
  ([]
   (fn [component]
     (js/React.memo component #(= (gobj/get %1 ":rum/args")
                                  (gobj/get %2 ":rum/args")))))
  ([eq-fn]
   (fn [component]
     (js/React.memo component eq-fn))))


(defn wrap-key
  [key-fn]
  {:wrap :props
   :fn   (fn [props]
           (assoc props "key" (apply key-fn (get props ":rum/args"))))})


;; derived-atom

(def ^{:style/indent 2
       :arglists '([refs key f] [refs key f opts])
       :doc "Use this to create “chains” and acyclic graphs of dependent atoms.

             [[derived-atom]] will:

             - Take N “source” refs.
             - Set up a watch on each of them.
             - Create “sink” atom.
             - When any of source refs changes:
                - re-run function `f`, passing N dereferenced values of source refs.
                - `reset!` result of `f` to the sink atom.
             - Return sink atom.

             Example:

             ```
             (def *a (atom 0))
             (def *b (atom 1))
             (def *x (derived-atom [*a *b] ::key
                       (fn [a b]
                         (str a \":\" b))))

             (type *x)  ;; => clojure.lang.Atom
             (deref *x) ;; => \"0:1\"

             (swap! *a inc)
             (deref *x) ;; => \"1:1\"

             (reset! *b 7)
             (deref *x) ;; => \"1:7\"
             ```

             Arguments:

             - `refs` - sequence of source refs,
             - `key`  - unique key to register watcher, same as in `clojure.core/add-watch`,
             - `f`    - function that must accept N arguments (same as number of source refs) and return a value to be written to the sink ref. Note: `f` will be called with already dereferenced values,
             - `opts` - optional. Map of:
               - `:ref` - use this as sink ref. By default creates new atom,
               - `:eq-fn` - Defaults to `=`. An equality check function.  With the default when the result of recalculating `f` equals to the old value, `reset!` won’t be called. Set to `(constantly false)` if checking for equality can be expensive."}
  derived-atom derived-atom/derived-atom)


;; cursors

(defn cursor-in
  "Given atom with deep nested value and path inside it, creates an atom-like structure
   that can be used separately from main atom, but will sync changes both ways:

   ```
   (def db (atom { :users { \"Ivan\" { :age 30 }}}))

   (def ivan (rum/cursor db [:users \"Ivan\"]))
   (deref ivan) ;; => { :age 30 }

   (swap! ivan update :age inc) ;; => { :age 31 }
   (deref db) ;; => { :users { \"Ivan\" { :age 31 }}}

   (swap! db update-in [:users \"Ivan\" :age] inc)
   ;; => { :users { \"Ivan\" { :age 32 }}}

   (deref ivan) ;; => { :age 32 }
   ```

   Returned value supports `deref`, `swap!`, `reset!`, watches and metadata.

   The only supported option is `:meta`"
  [ref path & {:as options}]
  (if (instance? cursor/Cursor ref)
    (cursor/Cursor. (.-ref ref)
                    (into (vec (.-path ref)) path)
                    (merge (.-meta ref) (:meta options)))
    (cursor/Cursor. ref path (:meta options))))


(defn cursor
  "Same as [[cursor-in]] but accepts single key instead of path vector."
  [ref key & options]
  (apply cursor-in ref [key] options))
