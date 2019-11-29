(ns rum.examples.fn.self-reference
  (:require
    [rum.core :as rum]))


;; Self-referencing component


(rum/defnc self-reference <
  (rum/wrap-memo)
  (rum/wrap-key (fn [form depth]
                  (str form)))
  ([form] (self-reference form 0))
  ([form depth]
   (let [offset {:style {:margin-left (* 10 depth)}}]
     (if (sequential? form)
       [:.branch offset (map #(self-reference % (inc depth)) form)]
       [:.leaf   offset (str form)]))))


(defn mount! [mount-el]
  (rum/hydrate (self-reference [:a [:b [:c :d [:e] :g]]]) mount-el))
