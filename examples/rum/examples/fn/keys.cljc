(ns rum.examples.fn.keys
  (:refer-clojure :exclude [keys])
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


(rum/defnc keyed < (rum/wrap-key (fn [label number]
                                   (str label "-" number)))
  [label number]
  [:div (str label "-" number)])


(rum/defnc keys []
  [:div
    (list
      (keyed "a" 1)
      (keyed "a" 2)
      (keyed "b" 1)
      (rum/with-key (keyed "a" 1) "x"))])


(defn mount! [mount-el]
  (rum/hydrate (keys) mount-el))
