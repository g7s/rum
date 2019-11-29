(ns rum.examples.fn.core
  (:require
    [rum.core :as rum]))


(def *clock (atom 0))
(def *color (atom "#FA8D97"))
(def *speed (atom 167))


#?(:clj
    (def formatter
      (doto (java.text.SimpleDateFormat. "HH:mm:ss.SSS")
        (.setTimeZone (java.util.TimeZone/getTimeZone "UTC")))))


(defn format-time [ts]
  #?(:cljs (-> ts (js/Date.) (.toISOString) (subs 11 23))
     :clj  (.format formatter (java.util.Date. ts))))


#?(:cljs
    (defn el [id]
      (js/document.getElementById id)))


(defn use-periodic-refresh [period]
  #?(:cljs
     (let [s (rum/use-state false)]
       (rum/use-effect
        (fn []
          (let [interval (js/setInterval #(swap! s not) period)]
            (fn []
              (js/clearInterval interval))))))))


;; Using custom mixin
(rum/defnc watches-count [ref]
  (use-periodic-refresh 1000)
  [:span (count #?(:cljs (.-watches ref)
                   :clj  (.getWatches ^clojure.lang.IRef ref))) ])


;; Generic board utils


(def ^:const board-width 19)
(def ^:const board-height 10)


(defn prime? [i]
  (and (>= i 2)
       (empty? (filter #(= 0 (mod i %)) (range 2 i)))))


(defn initial-board []
  (->> (map prime? (range 0 (* board-width board-height)))
       (partition board-width)
       (mapv vec)))


(rum/defnc board-stats [*board *renders]
  [:div.stats
    "Renders: "       (rum/use-react *renders)
    [:br]
    "Board watches: " (watches-count *board)
    [:br]
    "Color watches: " (watches-count *color) ])
