(ns rum.examples.fn.board-reactive
  (:require
    [rum.core :as rum]
    [rum.examples.fn.core :as core]))


;; Reactive drawing board


(def *board (atom (core/initial-board)))
(def *board-renders (atom 0))


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


(rum/defnc cell [x y]
  (rum/use-effect #(swap! *board-renders inc))
  (let [*cursor (rum/use-memo #(rum/cursor-in *board [y x]) [x y])]
    ;; each cell subscribes to its own cursor inside a board
    ;; note that subscription to color is conditional:
    ;; only if cell is on (@cursor == true),
    ;; this component will be notified on color changes
    [:div.art-cell {:style {:background-color (react-when (rum/use-react *cursor) core/*color)}
                    :on-mouse-over (fn [_] (swap! *cursor not) nil)}]))


(rum/defnc board-reactive []
  [:div.artboard
    (for [y (range 0 core/board-height)]
      [:div.art-row {:key y}
        (for [x (range 0 core/board-width)]
          ;; this is how one can specify React key for component
          (-> (cell x y)
              (rum/with-key [x y])))])
   (core/board-stats *board *board-renders)])


(defn mount! [mount-el]
   (rum/hydrate (board-reactive) mount-el))
