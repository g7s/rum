(ns rum.examples.class.custom-props
  (:require
    [goog.object :as gobj]
    [rum.core :as rum]
    [rum.examples.class.core :as core]))


;; Custom methods and data on the underlying React components.

(defn rand-color []
  (str "#" (-> (rand)
               (* 0xffffff)
               (js/Math.floor)
               (.toString 16))))


(def props
  {:msgData "Components can store custom data on the underlying React component."
   :msgMethod #(this-as this
                 [:div {:style {:cursor "pointer"}
                        :on-mouse-move
                        (fn [_]
                          (reset! core/*color (rand-color))
                          (gobj/set this "msgData"
                            [:div {:style {:color @core/*color}}
                             (:msgData props)])
                          (rum/request-render @(rum/state this)))}
                  "Custom methods too. Hover over me!"])})


(rum/defcs custom-props < {:class-properties props}
  [state]
  (let [comp (rum/react-component state)]
    [:div
     ;; using aget to avoid writing externs
     [:div (gobj/get comp "msgData")]
     [:div (.call (gobj/get comp "msgMethod") comp)]]))


(defn mount! [mount-el]
  (rum/mount (custom-props) mount-el))
