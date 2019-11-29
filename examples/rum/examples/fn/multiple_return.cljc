(ns rum.examples.fn.multiple-return
  (:require [rum.core :as rum]))


(rum/defnc multiple-return []
  (for [n (range 5)]
    [:li {:key n} (str "Item #" n)]))


(rum/defnc ulist [child]
  [:ul {}
   child])


(defn mount! [mount-el]
  (rum/hydrate (ulist (multiple-return)) mount-el))
