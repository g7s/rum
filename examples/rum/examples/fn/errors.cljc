(ns rum.examples.fn.errors
  (:require
    [rum.core :as rum]))


(rum/defnc faulty-render [msg]
  (throw (ex-info msg {})))


(rum/defnc faulty-effect [msg]
  (rum/use-effect (fn [] (throw (ex-info msg {}))))
  "Some test you’ll never see")


(rum/defcs child-error < { :did-catch
                          (fn [state error info]
                            (assoc state ::error error)) }
  [{error ::error, c :rum/react-component} comp msg]
  (if (some? error)
    [:span "CAUGHT: " (str error)]
    [:span "No error: " (comp msg)]))


(rum/defnc errors []
  [:span
    (child-error faulty-render "render error")
    (child-error faulty-effect "effect error")])


(defn mount! [mount-el]
  (rum/mount (errors) mount-el))
