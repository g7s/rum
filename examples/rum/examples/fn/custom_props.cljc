(ns rum.examples.fn.custom-props
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


(rum/defnc custom-props []
  [:div "N/A"])


(defn mount! [mount-el]
  (rum/hydrate (custom-props) mount-el))
