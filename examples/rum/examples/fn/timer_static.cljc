(ns rum.examples.fn.timer-static
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Static component (quiescent-style)


(rum/defnc timer-static < (rum/wrap-memo)
  [label ts]
  [:div label ": "
    [:span {:style {:color @core/*color}} (core/format-time ts)]])


(defn mount! [mount-el]
  (rum/hydrate (timer-static "Static" @core/*clock) mount-el)
  ;; Setting up watch manually,
  ;; force top-down re-render via mount
  (add-watch core/*clock :timer-static
             (fn [_ _ _ new-val]
               (rum/hydrate (timer-static "Static" new-val) mount-el))))
