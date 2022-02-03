(ns user.persons
  (:require [clojure.spec.alpha :as s]
            [hyperfiddle.api :as hf]
            [hyperfiddle.photon :as p]
            [hyperfiddle.rcf :refer [! % tests]])
  #?(:cljs (:require-macros [user.persons :refer [genders gender shirt-sizes persons submission emails sub-profile]])))


(s/fdef genders :args (s/cat) :ret (s/coll-of number?))
(p/defn genders []
  (into [] ~ (hf/q '[:find [?e ...] :where [_ :dustingetz/gender ?e]] (:db hf/db))))

(tests
  (def dispose (p/run (! (p/$ genders))))
  % := [:dustingetz/male :dustingetz/female]
  (dispose))

(p/defn gender []
  (first (p/$ genders)))


(tests
  (def dispose (p/run (! (p/$ gender))))
  % := :dustingetz/male
  (dispose))

(s/fdef shirt-sizes :args (s/cat :gender keyword?
                                 :needle string?)
        :ret (s/coll-of number?))

(p/defn shirt-sizes [gender needle]
  (when gender
    (sort
      ~(hf/q '[:in $ % ?gender ?needle
               :find [?e ...]
               :where
               [?e :dustingetz/type :dustingetz/shirt-size]
               [?e :dustingetz/gender ?gender]
               [?e :db/ident ?ident]
               (hyperfiddle.api/needle-match ?ident ?needle)]
             (:db hf/db)
             hf/rules gender (or needle "")))))

(p/defn emails [needle]
  ~(hf/q '[:in $ % ?needle
           :find [?e ...]
           :where
           [?e :dustingetz/email ?email]
           (hyperfiddle.api/needle-match ?email ?needle)]
         (:db hf/db)
         hf/rules (or needle "")))

(s/fdef emails :args (s/cat :needle string?)
        :ret (s/coll-of string?))

(p/defn persons [needle]
  (sort
    ~(hf/q '[:find [?e ...]
             :in $ % ?needle
             :where
             [?e :dustingetz/email ?email]
             (hyperfiddle.api/needle-match ?email ?needle)]
           (:db hf/db)
           hf/rules (or needle ""))))

(s/fdef persons :args (s/cat :needle string?)
        :ret (s/coll-of (s/keys :req [:dustingetz/email
                                      :dustingetz/email1
                                      :dustingetz/gender
                                      :dustingetz/shirt-size])))


(s/fdef submission :args (s/cat :needle string?) :ret number?)
(p/defn submission [needle] (first (p/$ persons needle)))


(s/fdef sub-profile :args (s/cat :sub any?) :ret any?)
(p/defn sub-profile [sub] ~(hf/nav (:db hf/db) sub :db/id))

(tests
  (hyperfiddle.photon/run (! (p/$ user.gender-shirt-size/submission "")))
  %)

(tests
  (def dispose
    (p/run
      (! (p/$ persons ""))
      (! (p/$ persons "example"))
      (! (p/$ persons "b"))))
  % := [9 10 11]
  % := [9 10 11]
  % := [10]
  (dispose))