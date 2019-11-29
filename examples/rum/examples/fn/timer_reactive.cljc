(ns rum.examples.fn.timer-reactive
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reactive components (reagent-style)


;; regular static top-down component with immutable args
(rum/defnc colored-clock < (rum/wrap-memo)
  [time color]
  [:span {:style {:color color}} (core/format-time time)])


(rum/defnc timer-reactive []
  [:div "Reactive: "
    ;; Subscribing to atom changes with rum/react
    ;; Then pass _dereferenced values_ to static component
    (colored-clock (rum/use-react core/*clock) (rum/use-react core/*color))])


;; After initial mount, all changes will be re-rendered automatically
(defn mount! [mount-el]
  (rum/hydrate (timer-reactive) mount-el))
