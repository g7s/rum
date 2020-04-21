(defproject g7s/rum "0.11.7-SNAPSHOT"
  :description  "ClojureScript wrapper for React"
  :license      {:name "Eclipse"
                 :url  "http://www.eclipse.org/legal/epl-v10.html" }
  :url          "https://github.com/g7s/rum"

  :dependencies
  [[org.clojure/clojure "1.10.1" :scope "provided"]
   [org.clojure/clojurescript "1.10.597" :scope "provided"]
   [sablono "0.8.6"]]

  :source-paths
  ["src"]

  :profiles
  {:dev  {:source-paths ["examples"]
          :dependencies [[thheller/shadow-cljs "2.8.74"]
                         [com.google.javascript/closure-compiler-unshaded "v20190325"]
                         [org.clojure/google-closure-library "0.0-20190213-2033d5d9"]
                         [clj-diffmatchpatch "0.0.9.3" :exclusions [org.clojure/clojure]]] }
   :repl {:source-paths ["dev"]
          :dependencies [[org.clojure/tools.namespace "0.3.1"]]
          :repl-options {:init-ns user
                         :nrepl-middleware
                         [shadow.cljs.devtools.server.nrepl04/middleware]}}
   #_:perf #_{:source-paths ["perf"]
          :dependencies
          [[enlive    "1.1.6"]
           [criterium "0.4.4"]
           [hiccup    "1.0.5"]]}
   :test {:source-paths ["test"]}}

  :aliases {})
