(ns rum.examples.fn.controls
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Control panel


;; generic “atom editor” component
(rum/defnc input [ref]
  [:input {:type "text"
           :value (rum/use-react ref)
           :style {:width 100}
           :on-change #(reset! ref (.. % -target -value))}])


;; Raw top-level component, everything interesting is happening inside
(rum/defnc controls []
  [:dl
    [:dt "Color: "]
    [:dd (input core/*color)]
    ;; Binding another component to the same atom will keep 2 input boxes in sync
    [:dt "Clone: "]
    [:dd (input core/*color)]
    [:dt "Color: "]
    [:dd (core/watches-count core/*color) " watches"]

    [:dt "Tick: "]
    [:dd (input core/*speed) " ms"]
    [:dt "Time:"]
    [:dd (core/watches-count core/*clock) " watches"]
])


(defn mount! [mount-el]
   (rum/hydrate (controls) mount-el))
