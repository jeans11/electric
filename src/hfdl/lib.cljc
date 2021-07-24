(ns hfdl.lib
  (:require [hfdl.lang :refer [#?(:clj dataflow)]]
            [hfdl.impl.rfor :refer [rfor]]
            [missionary.core :as m])
  #?(:clj (:import (clojure.lang Box)))
  #?(:cljs (:require-macros [hfdl.lang :refer []])))

(defn place! "
Defines a new identity representing a variable initialized with given value and usable both :
* as a continuous flow producing successive states of the variable.
* as a one-argument function assigning a new state to the variable."
  [init]
  (let [!a (atom init)
        >a (m/watch !a)]
    (fn
      ([x] (reset! !a x))
      ([n t] (>a n t)))))

#?(:clj
   (defn diff [z -]
     (fn [rf]
       (let [p (Box. z)]
         (fn
           ([] (rf))
           ([r] (rf r))
           ([r x]
            (let [r (rf r (- x (.-val p)))]
              (set! (.-val p) x) r)))))))

(comment
  (def >input (atom (vec (range 5))))
  (def ats (vec (repeatedly 10 #(atom 0))))

  ;; (A -> Flow[B]) -> Flow[Traversable[A]] -> Flow[Traversable[B]]
  (def it ((reactive-for (fn [id] (prn :branch id) (m/watch (nth ats id))) (m/watch >input))
           #(prn :ready) #(prn :done)))

  (swap! >input conj 5)
  (swap! >input pop)

  (swap! (nth ats 1) inc)
  (swap! (nth ats 2) + 2)

  (reset! >input [0 2 1 3 4 5])

  @it

  )

(comment
  (def !db (atom [1]))
  (def >db (m/watch !db))

  (defn q [query >db]
    >db)

  (defn entity-ks [>db x]
    (m/ap [:a :b :c]))

  (defn entity-get [>db x k]
    (m/ap (case k
            :a "a"
            :b "b"
            :c "c")))

  (defn get-column [x k]
    (dataflow (pr-str @(entity-get >db x k))))

  (defn get-row [x]
    (dataflow
      (pr-str x)
      (let [ks @(entity-ks >db x)]
        [:pre (pr-str @(entity-get >db x :person/name))]
        @(reactive-for (partial get-column x) ~ks))))

  (def query '[:select])

  (def app
    (dataflow
      (let [xs @(q query >db)]
        [:div
         [:pre (pr-str xs)]
         @(reactive-for get-row ~xs)])))

  (require '[hfdl.lang :refer [debug!]])
  (def p (debug! app))

  (swap! !db conj 4)
  (swap! !db disj 3)
  @p

  )

(comment
  (require '[geoffrey.diff :refer [*$*]])
  (require '[datascript.core :as d])
  (geoffrey.diff/init-datascript)

  ;(defn q [>db]
  ;  (m/latest #(datomic.api/q '[:find ?e :where [?e :dustingetz/email]] %) >db))

  (def query-users '[:find [?e ...] :where [?e :dustingetz/email]])
  (defn q [>db]
    (dataflow (take 2 (sort (d/q query-users @>db)))))

  (defn entity-ks [>db x]
    (dataflow (keys (d/touch (d/entity @>db x)))))

  (defn entity-get [>db x kf]
    (dataflow (kf (d/entity @>db x))))

  (defn get-column [>db x k]
    (dataflow [:pre (pr-str @(entity-get >db x k))]))

  (defn get-row [>db x]
    (dataflow
      [:pre (pr-str @(entity-get >db x :dustingetz/email))]
      (let [#_#_db2 (:db-after (d/with @>db [{:dustingetz/email "dan@example.com"}]))
            ks @(entity-ks >db x)]
        @(reactive-for (partial get-column >db x) ~ks))))

  (def !db (atom *$*))

  (def app
    (dataflow
      (let [db @(m/watch !db)
            xs @(q ~db)]
        [:div
         [:pre (pr-str xs)]
         @(reactive-for (partial get-row ~db) ~xs)])))

  (require '[hfdl.lang :refer [debug!]])
  (def p (debug! app))

  (swap! !db #(:db-after (d/with % [{:dustingetz/email "dan@example.com"}])))

  #_(swap! !db #(:db-after
                  (d/with %
                    [[:db/add 12 :dustingetz/email "dan2@example.com"]
                     [:db/retractEntity 11]])))

  @p
  ;; (hfdl.sourcemap/humanize app (hfdl.lang/heap-dump @p))

  )