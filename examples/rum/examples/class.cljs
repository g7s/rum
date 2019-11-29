(ns rum.examples.class
  (:require
    [clojure.string :as str]
    [rum.core :as rum]
    [rum.examples.class.core :as core]
    [rum.examples.class.timer-static :as timer-static]
    [rum.examples.class.timer-reactive :as timer-reactive]
    [rum.examples.class.controls :as controls]
    [rum.examples.class.binary-clock :as binary-clock]
    [rum.examples.class.board-reactive :as board-reactive]
    [rum.examples.class.bmi-calculator :as bmi-calculator]
    [rum.examples.class.form-validation :as form-validation]
    [rum.examples.class.inputs :as inputs]
    [rum.examples.class.refs :as refs]
    [rum.examples.class.local-state :as local-state]
    [rum.examples.class.keys :as keys]
    [rum.examples.class.self-reference :as self-reference]
    [rum.examples.class.context :as context]
    [rum.examples.class.custom-props :as custom-props]
    [rum.examples.class.multiple-return :as multiple-return]
    [rum.examples.class.portals :as portals]
    [rum.examples.class.errors :as errors]))


(enable-console-print!)


;; Mount everything

(timer-static/mount!    (core/el "timer-static"))
(timer-reactive/mount!  (core/el "timer-reactive"))
(controls/mount!        (core/el "controls"))
(binary-clock/mount!    (core/el "binary-clock"))
(board-reactive/mount!  (core/el "board-reactive"))
(bmi-calculator/mount!  (core/el "bmi-calculator"))
(form-validation/mount! (core/el "form-validation"))
(inputs/mount!          (core/el "inputs"))
(refs/mount!            (core/el "refs"))
(local-state/mount!     (core/el "local-state"))
(keys/mount!            (core/el "keys"))
(self-reference/mount!  (core/el "self-reference"))
(context/mount!         (core/el "context"))
(custom-props/mount!    (core/el "custom-props"))
(multiple-return/mount! (core/el "multiple-return"))
(portals/mount!         (core/el "portal-root"))
(errors/mount!          (core/el "client-errors"))


;; Start clock ticking

(defn tick []
  (reset! core/*clock (.getTime (js/Date.)))
  (js/setTimeout tick @core/*speed))


(tick)
