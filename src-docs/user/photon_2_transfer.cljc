(ns user.photon-2-transfer
  "Photon with client/server transfer at the REPL"
  (:require [hyperfiddle.photon :as p]
            [hyperfiddle.rcf :refer [tests ! % with]]
            #?(:clj shadow.cljs.devtools.server)
            #?(:clj shadow.cljs.devtools.api)))


(p/defn App [x]
  (if (even? x)
    (pr-str (type 1))
    ~@(pr-str (type 1))))                   ; client/server transfer

#?(:cljs
   (tests
     "client/server transfer, pure functional!"
     (def !x (atom 0))
     (def dispose ((p/client (p/main (! (App. (p/watch !x)))))
                   js/console.log js/console.error))
     % := "#object[Number]"
     (swap! !x inc)
     % := "java.lang.Long"                  ; holy cow
     (dispose)))


; How to run:
; 1. Jack into JVM REPL
; 2. Start shadow stuff
; 3. Open a separate nrepl connected to shadow server and start shadow/repl

(hyperfiddle.rcf/enable!)
#?(:cljs (defn ^:dev/before-load stop [] (hyperfiddle.rcf/enable! false)))
#?(:cljs (defn ^:dev/after-load start [] (hyperfiddle.rcf/enable!)))

#?(:clj
   (do
     (shadow.cljs.devtools.server/start!)
     (shadow.cljs.devtools.api/watch
       {:build-id      :app
        :target        :browser
        :devtools      {:watch-dir "resources/public"}
        :build-options {:cache-level :jars}
        :output-dir    "resources/public/js"
        :asset-path    "/js"
        :modules       {:main {:entries ['user.photon-2-transfer]}}})
     (p/start-websocket-server! {:host "localhost" :port 8081})
     (println (str "\n" "http://localhost:8080"))))

(comment
  ; connect a new NREPL do not use existing JVM repl !!!
  (shadow.cljs.devtools.api/repl :app)  ; do not eval in your existing JVM repl it wont work
  ; Connect browser session - http://localhost:8080
  ; Browser console: shadow-cljs: #3 ready!
  (type 1)
  (println 1)  ; see browser console
  (tests (pr-str (type 1)) := "#object[Number]")  ; see ✅ in browser console
  )