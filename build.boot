(set-env!
 :dependencies
 '[[org.clojure/clojure "1.8.0" :scope "provided"]
   [org.clojure/clojurescript "1.9.89" :scope "provided"]

   [binaryage/devtools "0.7.2"]
   [com.cognitect/transit-clj "0.8.285"]
   [cljs-http "0.1.41"]
   [environ "1.0.3"]
   [boot-environ "1.0.3" :scope "test"]

   [ajchemist/boot-figwheel "0.5.4-6" :scope "test"] ;; latest release
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [figwheel-sidecar "0.5.4-7" :scope "test"]])

(require
 'boot-figwheel
 '[environ.boot :refer [environ]])

(refer 'boot-figwheel :rename '{cljs-repl fw-cljs-repl})

(task-options!
 pom {:project 'binaryage/devtools-sample
      :version "0.1.0-SNAPSHOT"
      :description "An example integration of cljs-devtools"})

(def all-builds
  [{:id "demo"
    :source-paths ["src/demo"]
    :compiler {:output-to     "_compiled/demo/devtools_sample.js"
               :output-dir    ""
               :main          'devtools-sample.boot
               :preloads      ['devtools.preload]
               :optimizations :none
               :source-map    true}
    :figwheel true}])

(def http-server-root "resources/public")

(require
 '[ring.middleware file]
 '[ring.util.response :as response]
 '[ring.util.mime-type :as mime])

(def ring-handler
  (-> (fn [{uri :uri}]
        (ring.util.response/not-found
         (format "<div><h1>Figwheel Server: {%s} not found</h1></div>" uri)))
    (ring.middleware.file/wrap-file http-server-root {:allow-symlinks? true})
    ((fn [handler]
       (fn [{uri :uri :as req}]
         (let [resp (handler req)]
           (if-let [mime-type (mime/ext-mime-type uri)]
             (response/content-type resp mime-type)
             resp)))))
    #_(ring.middleware.content-type/wrap-content-type)
    #_(ring.middleware.not-modified/wrap-not-modified)))

(def fw-options
  {:server-port 7000
   :server-logfile ".figwheel_server.log"
   :ring-handler 'boot.user/ring-handler})

(deftask demo-figwheel []
  (merge-env! :source-paths #{"src/demo"})
  (figwheel
   :build-ids ["demo"]
   :all-builds all-builds
   :figwheel-options fw-options
   :target-path http-server-root))

(deftask demo []
  (comp
   #_(target :dir #{"resources/public/_compiled/demo"} :no-clean true)
   (repl :server true)
   (demo-figwheel)
   (wait)))
