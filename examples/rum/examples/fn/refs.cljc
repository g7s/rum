(ns rum.examples.fn.refs
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


(def r (rum/create-ref))


(rum/defnc ta
  []
  (let [render! (rum/use-render)]
    (rum/use-effect
     (fn []
       (let [ta (rum/ref-val r)]
         (set! (.-height (.-style ta)) "0")
         (set! (.-height (.-style ta)) (str (+ 2 (.-scrollHeight ta)) "px")))))
    [:textarea
     { :ref          r
      :style         { :width  "100%"
                      :padding "10px"
                      :font    "inherit"
                      :outline "none"
                      :resize  "none"}
      :default-value "Auto-resizing\ntextarea"
      :placeholder   "Auto-resizing textarea"
      :on-change     (fn [_] (render!)) }]))


(rum/defc refs []
  [:div
    (ta)])


(defn mount! [mount-el]
  (rum/hydrate (refs) mount-el))
