(ns rum.examples.fn.local-state
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


;; Local component state


(rum/defnc local-state
  [title]
  (let [*count (rum/use-state 0)]
    [:div
     {:style {"-webkit-user-select" "none"
              "cursor" "pointer"}
      :on-click (fn [_] (swap! *count inc)) }
     title ": " @*count]))


(defn mount! [mount-el]
  (rum/hydrate (local-state "Clicks count") mount-el))
