(ns rum.examples.fn.portals
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


(rum/defnc portal [*clicks]
  [:div
    { :on-click (fn [_] (swap! *clicks inc))
      :style { :user-select "none", :cursor "pointer" }}
    "[ PORTAL Clicks: " @*clicks " ]"])


(rum/defnc root
  []
  (let [*clicks (rum/use-state 0)]
    [:div
     { :on-click (fn [_] (swap! *clicks inc))
      :style     { :user-select "none", :cursor "pointer" } }
     "[ ROOT Clicks: " @*clicks " ]"
     (rum/portal (portal *clicks) #?(:cljs (core/el "portal-off-root")
                                     :clj nil))]))


(defn mount! [mount-el]
  (rum/hydrate (root) mount-el))
