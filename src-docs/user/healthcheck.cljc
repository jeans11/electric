(ns user.healthcheck
  (:require [hyperfiddle.photon :as p]
            [hyperfiddle.photon-dom :as dom]
            [hyperfiddle.zero :as z])
  (:import (hyperfiddle.photon Pending))
  #?(:cljs (:require-macros user.healthcheck)))      ; forces shadow hot reload to also reload JVM at the same time

(p/defn App []
  (dom/div
    (dom/h1 (dom/text "Healthcheck"))
    (dom/dl
      (dom/dt (dom/text "time"))   (dom/dd (dom/text z/time))
      (dom/dt (dom/text "client")) (dom/dd (dom/text (p/client (pr-str (type 1)))))
      (dom/dt (dom/text "server")) (dom/dd (dom/text (p/server (pr-str (type 1))))))))

(def main
  #?(:cljs (p/boot
             (try
               (binding [dom/node (dom/by-id "root")]
                 (App.))
               (catch Pending _)))))

(comment
  #?(:clj (user/browser-main! `main))
  )