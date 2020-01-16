(ns rum.examples.fn.form-validation
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


(defn- react-when
  [c ref]
  (let [key     (rum/use-var (gensym "react-when"))
        render! (rum/use-render)]
    (rum/use-effect (fn []
                      (if c
                        (add-watch ref key render!)
                        (remove-watch ref key))
                      (fn []
                        (remove-watch ref key))))
    (when c @ref)))


(rum/defnc validating-input [ref f]
  (let [val (rum/use-react ref)]
    [:input {:type      "text"
             :style     {:width            170
                         :background-color (react-when (not (f val)) core/*color)}
             :value     val
             :on-change #(reset! ref (.. % -target -value))}]))


(rum/defnc restricting-input [ref f]
  (let [render! (rum/use-render)]
    [:input {:type      "text"
             :style     {:width 170}
             :value     (rum/use-react ref)
             :on-change #(let [new-val (.. % -target -value)]
                           (if (f new-val)
                             (reset! ref new-val)
                             ;; request-render is mandatory because sablono :input
                             ;; keeps current value in input’s state and always applies changes to it
                             (render!)))}]))


(rum/defnc restricting-input-native [ref f]
  (let [render! (rum/use-render)]
    #?(:cljs
       (js/React.createElement "input"
                               #js {:type     "text"
                                    :style    #js {:width 170}
                                    :value    (rum/use-react ref)
                                    :onChange #(let [new-val (.. % -target -value)]
                                                 (when (f new-val)
                                                   (reset! ref new-val))
                                                 ;; need forceUpdate here because otherwise rendering will be delayed until requestAnimationFrame
                                                 ;; and that breaks cursor position inside input
                                                 (render!))}))))


(rum/defnc form-validation []
  (let [state (atom {:email "a@b.c"
                     :phone "+7913 000 0000"
                     :age   "22"})]
    [:dl
     [:dt "E-mail:"]
     [:dd (validating-input  (rum/cursor state :email) #(re-matches #"[^@]+@[^@.]+\..+" %))]
     [:dt "Phone:"]
     [:dd (restricting-input (rum/cursor state :phone) #(re-matches #"[0-9\- +()]*" %))]
     [:dt "Age:"]
     [:dd (restricting-input-native (rum/cursor state :age) #(re-matches #"([1-9][0-9]*)?" %))]]))


(defn mount! [mount-el]
  (rum/hydrate (form-validation) mount-el))
