(ns rum.examples.fn
  (:require
    [clojure.string :as str]
    [rum.core :as rum]
    [rum.examples.fn.core :as core]
    [rum.examples.fn.bmi-calculator :as bmi-calculator]
    [rum.examples.fn.binary-clock :as binary-clock]
    [rum.examples.fn.timer-static :as timer-static]
    [rum.examples.fn.timer-reactive :as timer-reactive]
    [rum.examples.fn.controls :as controls]
    [rum.examples.fn.board-reactive :as board-reactive]
    [rum.examples.fn.form-validation :as form-validation]
    [rum.examples.fn.inputs :as inputs]
    [rum.examples.fn.refs :as refs]
    [rum.examples.fn.local-state :as local-state]
    [rum.examples.fn.keys :as keys]
    [rum.examples.fn.self-reference :as self-reference]
    [rum.examples.fn.context :as context]
    [rum.examples.fn.custom-props :as custom-props]
    [rum.examples.fn.multiple-return :as multiple-return]
    [rum.examples.fn.portals :as portals]
    [rum.examples.fn.errors :as errors]
    ))


(enable-console-print!)


;; Mount everything
(bmi-calculator/mount!  (core/el "bmi-calculator"))
(binary-clock/mount!    (core/el "binary-clock"))
(board-reactive/mount!  (core/el "board-reactive"))

(timer-static/mount!    (core/el "timer-static"))
(timer-reactive/mount!  (core/el "timer-reactive"))
(controls/mount!        (core/el "controls"))
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
