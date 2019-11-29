(ns rum.examples.fn.inputs
  (:require
    [clojure.string :as str]
    [rum.core :as rum]))


(def values (range 1 5))


(rum/defnc reactive-input
  [*ref]
  (let [value (rum/use-react *ref)]
    [:input { :type "text"
              :value (str value)
              :style { :width 170 }
              :on-change (fn [e] (reset! *ref (long (.. e -currentTarget -value)))) }]))


(rum/defnc checkboxes
  [*ref]
  (let [value (rum/use-react *ref)]
    [:div
      (for [v values]
        [:input { :key v
                  :type "checkbox"
                  :checked (= v value)
                  :value   v
                  :on-click (fn [_] (reset! *ref v)) }])]))


(rum/defnc radio
  [*ref]
  (let [value (rum/use-react *ref)]
    [:div
      (for [v values]
        [:input { :key v
                  :type "radio"
                  :name "inputs_radio"
                  :checked (= v value)
                  :value   v
                  :on-click (fn [_] (reset! *ref v)) }])]))


(rum/defnc select
  [*ref]
  (let [value (rum/use-react *ref)]
    [:select
      { :on-change (fn [e] (reset! *ref (long (.. e -target -value))))
        :value (str value) }
      (for [v values]
        [:option { :key v
                   :value (str v)}
         v])]))


(defn next-value [v]
  (loop [v' v]
    (if (= v v')
      (recur (rand-nth values))
      v')))


(rum/defnc shuffle-button
  [*ref]
  [:button
    { :on-click (fn [_]
                  (swap! *ref next-value)) }
    "Next value"])


(rum/defnc value
  [*ref]
  [:code (pr-str (rum/use-react *ref))])


(rum/defnc inputs []
  (let [*ref (atom 1)]
    [:dl
      [:dt "Input"]  [:dd (reactive-input *ref)]
      [:dt "Checks"] [:dd (checkboxes *ref)]
      [:dt "Radio"]  [:dd (radio *ref)]
      [:dt "Select"] [:dd (select *ref)]
      [:dt (value *ref)] [:dd (shuffle-button *ref)]]))


(defn mount! [mount-el]
  (rum/hydrate (inputs) mount-el))
