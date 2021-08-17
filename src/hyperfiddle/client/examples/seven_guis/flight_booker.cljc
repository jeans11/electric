(ns hyperfiddle.client.examples.seven-guis.flight-booker
  (:require [hfdl.lang :as photon]
            [hyperfiddle.photon-dom :as dom]
            [devcards.core :as dc :include-macros true]
            [missionary.core :as m]
            [hyperfiddle.client.examples.card :refer [dom-node]]
            [clojure.string :as str])
  #?(:cljs (:require-macros [hyperfiddle.client.examples.seven-guis.flight-booker :refer [ComboBox DateInput BookButton FlightBooker]])))

(def date-regexp #"^[0-9]{1,2}\.[0-9]{1,2}\.[0-9]{4}$")

(defn is-date? [x] (re-find date-regexp x))

(defn valid-date? [date] (and (= 3 (count date))
                              (nat-int? (nth date 0))
                              (nat-int? (nth date 1))
                              (nat-int? (nth date 2))))

;; (valid-date? (parse-date "11.22.3333a"))

(defn read-int [x] #?(:clj (Integer/parseInt x)
                      :cljs (js/parseInt x)))

(defn parse-date [x]
  (map read-int (some-> (re-find date-regexp x)
                        (str/split #"\."))))

(defn serialize-date [[m d y]]
  (str m "." d "." y))

(defn after-or-equal? [[m1 d1 y1] [m2 d2 y2]]
  (and (>= y2 y1)
       (>= m2 m1)
       (>= d2 d1)))


(photon/defn DateInput [disabled? default-value]
  (let [!value (atom default-value)]
    (dom/input
     (dom/attribute "value" (serialize-date default-value)) ;; uncontrolled, just init to a defalut value
     (dom/style {"background-color" (if (valid-date? ~(m/watch !value))
                                      "inherit"
                                      "red")})
     (dom/property "disabled" disabled?)
     (reset! !value
             ~(->> (dom/events dom/parent "keyup")
                   (m/eduction (map dom/event-target)
                               (map dom/get-value)
                               (map parse-date))
                   (m/reductions {} default-value)
                   (m/relieve {}))))))

(photon/defn ComboBox [default-value]
  (dom/select
   (dom/option (dom/attribute "value" "one-way") (dom/text "one-way flight"))
   (dom/option (dom/attribute "value" "return") (dom/text "return flight"))
   ~(->> (dom/events dom/parent "change")
         (m/eduction (map dom/event-target)
                     (map dom/get-value))
         (m/reductions {} default-value)
         (m/relieve {}))))

(defn log [m x] (prn m x) x)

(def default-date (parse-date "27.03.2014"))

(defn alert [x] #?(:cljs (js/alert x)))

(defn confirmation! [!flight-type !d1 !d2 click-event]
  (when (some? click-event)
    (alert
     (case @!flight-type
       "one-way" (str "You have booked a one-way flight on " (serialize-date @!d1))
       "return"  (str "You have booked a flight on " (serialize-date @!d1) ", returning on " (serialize-date @!d2))))))



(photon/defn BookButton [disabled?]
  (dom/button
   (dom/text "Book")
   (dom/property "disabled" disabled?)
   (log "init in " ~(->> (dom/events dom/parent "click")
                        (m/eduction (map (partial log "in 2")))
                        (m/reductions {} nil)
                        (m/relieve {})))))

(photon/defn FlightBooker []
  (let [!flight-type (atom nil)
        !d1          (atom nil)
        !d2          (atom nil)]
    (dom/div (dom/style {"display"        "grid"
                         "width"          "fit-content"
                         "grid-auto-flow" "columns"
                         "grid-gap"       "0.5rem"})
             (reset! !flight-type (photon/$ ComboBox "one-way"))
             (reset! !d1 (photon/$ DateInput false default-date))
             (reset! !d2 (photon/$ DateInput (not= "return" ~(m/watch !flight-type)) default-date))
             ;; FIXME BookButton is continuous, so changing any value after a
             ;; first click will display an alert, since the last click event is
             ;; re-sampled. It is probably related to how `do` works, since the
             ;; `disabled?` property might trigger a change in the
             ;; `dom/button`’s implicit `do`.
             (confirmation! !flight-type !d1 !d2
                           (photon/$ BookButton (or (not (after-or-equal? ~(m/watch !d1) ~(m/watch !d2)))
                                                    (not (valid-date? ~(m/watch !d1)))
                                                    (not (valid-date? ~(m/watch !d2)))))))))

(dc/defcard booker
  "# 3 — Flight Booker

   Challenge: Constraints.

   ![](https://eugenkiss.github.io/7guis/static/bookflight.a5434663.png)


   The focus of Flight Booker lies on modelling constraints between widgets on
   the one hand and modelling constraints within a widget on the other hand.
   Such constraints are very common in everyday interactions with GUI
   applications. A good solution for Flight Booker will make the constraints
   clear, succinct and explicit in the source code and not hidden behind a lot
   of scaffolding.

   The task is:

   - [x] to build a frame containing a combobox C with the two options “one-way
   flight” and “return flight”,
   - [x] two textfields T1 and T2 representing the start and return date,
   respectively,
   - [x] and a button B for submitting the selected flight.
   - [x] T2 is enabled iff C’s value is “return flight”.
   - [x] When C has the value “return flight” and T2’s date is strictly before
   T1’s then B is disabled.
   - [x] When a non-disabled textfield T has an ill-formatted date then T is
   colored red and B is disabled.
   - [x] When clicking B a message is displayed informing the user of his
   selection (e.g. “You have booked a one-way flight on 04.04.2014.”).
   - [x] Initially, C has the value “one-way flight”
   - [x] and T1 as well as T2 have the same (arbitrary) date
     - [x] (it is implied that T2 is disabled).
"
  (dom-node
   (fn [_ node]
     (photon/run
       (photon/binding [dom/parent node]
         (photon/$ FlightBooker))))))