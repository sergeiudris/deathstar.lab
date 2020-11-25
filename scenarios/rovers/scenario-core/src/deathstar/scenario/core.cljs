(ns deathstar.scenario.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]

   [reagent.core :as r]
   [reagent.dom :as rdom]

   ["@flatten-js/core" :default fjs :rename {BooleanOperations fjsbool
                                             Relations fjsrel}]))


; https://github.com/sergeiudris/starnet/tree/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game2.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game3/data.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game1.cljc

(s/def ::hovered-entity any?)

(defn filter-entities-in-range
  [entites rover]
  (let [fjs-rover-range (.circle fjs
                                 (.point fjs (::x rover)  (::y rover))
                                 (::rover-vision-range rover))
        entities-in-range (->> entites
                               (filter (fn [[k entity]]
                                         (and
                                          (not (= (::entity-type entity) ::sands))
                                          (not (empty?
                                                (.intersect
                                                 fjs-rover-range
                                                 (.point fjs (::x entity) (::y entity))))))
                                         #_(.intersect fjsrel
                                                       fjs-rover-range
                                                       (.point fjs (::x v) (::y v)))))
                               (into {}))]
    entities-in-range))

(defn move-rover
  [state value]
  (let []
    (swap! state update ::rover merge (select-keys value [::x ::y]))))

(defn create-watchers
  [state]
  (let [#_rover* #_(r/cursor state [::rover])]
    (add-watch state ::watch-state
               (fn [key atom-ref old-state new-state]
                 (when (and
                        (::entities new-state) (::rover new-state)
                        (or
                         (not (identical? (::entities old-state) (::entities new-state)))
                         (not (identical? (::rover old-state) (::rover new-state)))))
                   (let [entites (::entities new-state)
                         rover (::rover new-state)
                         entities-in-range (filter-entities-in-range entites rover)]
                     (swap! state assoc ::entities-in-range entities-in-range)))))
    #_(add-watch rover* ::watch-rover
                 (fn [key atom-ref old-state new-state]
                   (println ::watch-rover)))))


(defn spec-number-in-range
  [spec_ min_ max_]
  (s/with-gen
    spec_
    #(gen/large-integer* {:min min_ :max max_})))


(s/def ::id uuid?)
(s/def ::entity-type keyword?)
(s/def ::x (s/with-gen
             int?
             #(gen/large-integer* {:min 0 :max 63})))
(s/def ::y (s/with-gen
             int?
             #(gen/large-integer* {:min 0 :max 63})))


(s/def ::energy-level number?)
(s/def ::rover-vision-range int?)
(s/def ::rover-scan-range int?)
(s/def ::rover-travel-range int?)

(s/def ::rover (s/merge
                (s/keys :req [::id])
                (s/with-gen
                  (s/keys :req [::x
                                ::y
                                ::entity-type
                                ::energy-level
                                ::rover-vision-range
                                ::rover-scan-range
                                ::rover-travel-range])
                  #(gen/hash-map
                    ::entity-type (gen/return ::rover)
                    ::x (gen/large-integer* {:min 26 :max 34})
                    ::y (gen/large-integer* {:min 12 :max 16})
                    ::rover-travel-range gen/small-integer
                    ::energy-level (gen/return 100)
                    ::rover-vision-range (gen/return 4)
                    ::rover-scan-range (gen/return 8)))))


(s/def ::location (s/merge
                   (s/keys :req [::id])
                   (s/with-gen
                     (s/keys :req [::x
                                   ::y
                                   ::entity-type])
                     #(gen/hash-map
                       ::entity-type (gen/return ::location)
                       ::x (gen/large-integer* {:min 0 :max 63})
                       ::y (gen/large-integer* {:min 0 :max 63})))))
(derive ::location ::entity)

(s/def ::energy number?)
(s/def ::recharge (s/merge
                   (s/keys :req [::id])
                   (s/with-gen
                     (s/keys :req [::energy ::entity-type])
                     #(gen/hash-map
                       ::entity-type (gen/return ::recharge)
                       ::x (gen/large-integer* {:min 0 :max 63})
                       ::y (gen/large-integer* {:min 0 :max 63})
                       ::energy (gen/large-integer* {:min 10 :max 30})))))
(derive ::recharge ::entity)


(s/def ::sands (s/merge
                (s/keys :req [::id])
                (s/with-gen
                  (s/keys :req [::energy ::entity-type])
                  #(gen/hash-map
                    ::entity-type (gen/return ::sands)
                    ::x (gen/large-integer* {:min 0 :max 63})
                    ::y (gen/large-integer* {:min 0 :max 63})
                    ::energy (gen/large-integer* {:min -20 :max -5})))))
(derive ::sands ::entity)

(defmulti entity-mm (fn [ent] (::entity-type ent)))
(defmethod entity-mm ::location [ent] ::location)
(defmethod entity-mm ::recharge [ent] ::recharge)
(defmethod entity-mm ::sands [ent] ::sands)
(s/def ::entity (s/with-gen
                  (s/multi-spec entity-mm ::entity-type)
                  #(gen/frequency
                    [[80 (s/gen ::sands)]
                     [15 (s/gen ::recharge)]
                     [5 (s/gen ::location)]])))


(s/def ::entities (s/with-gen
                    (s/map-of uuid? ::entity)
                    #(gen/hash-map
                      (gen/generate gen/uuid) (gen/frequency
                                               [[70 (s/gen ::sands)]
                                                [20 (s/gen ::recharge)]
                                                [10 (s/gen ::location)]]))))

(defn gen-entities
  [x y]
  (let [entity-generator (s/gen ::entity)]
    (->>
     (for [x (range 0 x)
           y (range 0 y)]
       (merge
        (gen/generate entity-generator)
        {::x x
         ::y y}))
     (reduce (fn [result entity]
               (assoc result (::id entity) entity)) {}))))

(defn gen-rover
  []
  (gen/generate (s/gen ::rover)))



(comment
  (isa? ::rover ::entity)
  (gen/generate (s/gen ::id))
  (gen/generate (s/gen ::sands))
  (gen/generate (s/gen ::rover))
  (gen/generate (s/gen ::recharge))
  (gen/generate (s/gen ::entity))

  (gen/sample (s/gen ::entities) 5)

  (gen/sample (s/gen ::entity) 5)


  (gen-entities 3 3)


  ;;
  )

