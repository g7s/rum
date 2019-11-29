(ns user
  (:require
   [clojure.tools.namespace.repl :as repl]
   [shadow.cljs.devtools.server :as server]
   [shadow.cljs.devtools.api :as api]
   [rum.examples.pages.class :as class-page]
   [rum.examples.pages.fn :as fn-page]))


(repl/set-refresh-dirs "dev" "src" "examples")


(defn gen
  []
  (class-page/gen)
  (fn-page/gen))


(defn go
  []
  (gen)
  (server/start!)
  (api/watch :rum))


(defn halt!
  []
  (server/stop!))


(defn reset
  []
  (repl/refresh)
  (gen))


(defn cljs
  []
  (api/repl :rum))
