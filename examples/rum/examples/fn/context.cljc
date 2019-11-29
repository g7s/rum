(ns rum.examples.fn.context
  (:require
   [rum.core :as rum]
   [rum.examples.class.core :as core]))


;; Components with context that all descendants have access to implicitly.

;; https://reactjs.org/docs/context.html
;; Context is designed to share data that can be considered “global”
;; for a tree of React components,
;; such as the current authenticated user, theme, or preferred language.

(def color-theme-ctx
  (rum/create-context @core/*color))


(rum/defnc rum-comp []
  (let [color (rum/use-context color-theme-ctx)]
    [:span
     { :style { :color color }}
     "Child component uses context to set font color."]))


(rum/defnc context []
  [:div
   [:div "Root component implicitly passes data to descendants."]
   (rum/provide-context color-theme-ctx
                        (rum/use-react core/*color)
                        (rum-comp))])


(defn mount! [mount-el]
  (rum/hydrate (context) mount-el))
