(ns rum.examples.refs
  (:require
    [rum.core :as rum]
    [rum.examples.core :as core]))


(def r (rum/create-ref))

(rum/defcs ta
  < {:after-render
      (fn [state]
        (let [ta (rum/ref-node r)]
          (set! (.-height (.-style ta)) "0")
          (set! (.-height (.-style ta)) (str (+ 2 (.-scrollHeight ta)) "px")))
        state)}
  [state]
  [:textarea
    { :ref r
      :style { :width   "100%"
               :padding "10px"
               :font    "inherit"
               :outline "none"
               :resize  "none"}
      :default-value "Auto-resizing\ntextarea"
      :placeholder "Auto-resizing textarea"
      :on-change (fn [_] (rum/request-render state)) }])


(rum/defc refs []
  [:div
    (ta)])


#?(:cljs
(defn mount! [mount-el]
     (rum/hydrate (refs) mount-el)))
