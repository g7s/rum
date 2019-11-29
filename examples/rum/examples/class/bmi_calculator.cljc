(ns rum.examples.class.bmi-calculator
  (:require
    [rum.core :as rum]
    [rum.examples.class.core :as core]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reagent stype BMI calculator


(defn calc-bmi [{:keys [height weight bmi] :as data}]
  (let [h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ weight (* h h)))
      (assoc data :weight (* bmi h h)))))


(def *bmi-data (atom (calc-bmi {:height 180
                                :weight 80})))


(defn slider [param value min max]
  (let [reset (case param
                :bmi :weight
                :bmi)]
    [:input {:type  "range"
             :value (int value)
             :min   min
             :max   max
             :style {:width "100%"}
             :on-change #(swap! *bmi-data assoc
                                param (-> % .-target .-value)
                                reset nil)}]))


(rum/defc bmi-calculator < rum/reactive []
  (let [{:keys [weight height bmi] :as data} (calc-bmi (rum/react *bmi-data))
        [color diagnose] (cond
                          (< bmi 18.5) ["orange" "underweight"]
                          (< bmi 25)   ["inherit" "normal"]
                          (< bmi 30)   ["orange" "overweight"]
                          :else        ["red" "obese"])]
    [:div.bmi
      [:div
        "Height: " (int height) "cm"
        (slider :height height 100 220)]
      [:div
        "Weight: " (int weight) "kg"
        (slider :weight weight 30 150)]
      [:div
        "BMI: " (int bmi) " "
        [:span {:style {:color color}} diagnose]
        (slider :bmi bmi 10 50)]]))


;; After initial mount, all changes will be re-rendered automatically
(defn mount! [mount-el]
  (rum/hydrate (bmi-calculator) mount-el))
