(ns deathstar.scenario.spec
  #?(:cljs (:require-macros [deathstar.scenario.spec]))
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))


; https://github.com/sergeiudris/starnet/tree/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game2.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game3/data.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game1.cljc


(defn spec-number-in-range
  [spec_ min_ max_]
  (s/with-gen
    spec_
    #(gen/large-integer* {:min min_ :max max_})))


(s/def ::uuid uuid?)
(s/def ::type keyword?)
(s/def ::x (s/with-gen
             int?
             #(gen/large-integer* {:min 0 :max 63})))
(s/def ::y (s/with-gen
             int?
             #(gen/large-integer* {:min 0 :max 63})))


(s/def ::energy-level number?)
(s/def ::range int?)
(s/def ::color string?)

(s/def ::rover (s/merge
                (s/keys :req [::uuid])
                (s/with-gen
                  (s/keys :req [::x
                                ::y
                                ::type
                                ::energy-level
                                ::color
                                ::range])
                  #(gen/hash-map
                    ::type (gen/return ::rover)
                    ::x (gen/large-integer* {:min 26 :max 34})
                    ::y (gen/large-integer* {:min 26 :max 34})
                    ::energy-level (gen/return 100)
                    ::color (gen/return "blue")
                    ::range (gen/return 5)))))


(s/def ::location (s/merge
                   (s/keys :req [::uuid])
                   (s/with-gen
                     (s/keys :req [::x
                                   ::y
                                   ::color 
                                   ::type])
                     #(gen/hash-map
                       ::type (gen/return ::location)
                       ::color (gen/return "brown")
                       ::x (gen/large-integer* {:min 0 :max 63})
                       ::y (gen/large-integer* {:min 0 :max 63})))))
(derive ::location ::entity)

(s/def ::energy number?)
(s/def ::recharge (s/merge
                   (s/keys :req [::uuid])
                   (s/with-gen
                     (s/keys :req [::energy ::type ::color])
                     #(gen/hash-map
                       ::type (gen/return ::recharge)
                       ::color  (gen/return "green")
                       ::x (gen/large-integer* {:min 0 :max 63})
                       ::y (gen/large-integer* {:min 0 :max 63})
                       ::energy (gen/large-integer* {:min 10 :max 30})))))
(derive ::recharge ::entity)


(s/def ::sands (s/merge
                (s/keys :req [::uuid])
                (s/with-gen
                  (s/keys :req [::energy ::type ::color])
                  #(gen/hash-map
                    ::type (gen/return ::sands)
                    ::color  (gen/return "#D2B48Cff")
                    ::x (gen/large-integer* {:min 0 :max 63})
                    ::y (gen/large-integer* {:min 0 :max 63})
                    ::energy (gen/large-integer* {:min -20 :max -5})))))
(derive ::sands ::entity)

(defmulti entity-mm (fn [ent] (::type ent)))
(defmethod entity-mm ::location [ent] ::location)
(defmethod entity-mm ::recharge [ent] ::recharge)
(defmethod entity-mm ::sands [ent] ::sands)
(s/def ::entity (s/with-gen
                  (s/multi-spec entity-mm ::type)
                  #(gen/frequency
                    [[40 (s/gen ::sands)]
                     [20 (s/gen ::recharge)]
                     [10 (s/gen ::location)]])))


(s/def ::entities (s/with-gen
                    (s/map-of uuid? ::entity)
                    #(gen/hash-map
                      (gen/generate gen/uuid) (gen/frequency
                                               [[40 (s/gen ::sands)]
                                                [20 (s/gen ::recharge)]
                                                [10 (s/gen ::location)]]))))

(defn gen-entities
  [x y]
  (let [entity-generator (s/gen ::entity)]
    (for [x (range 0 x)
          y (range 0 y)]
      (merge
       (gen/generate entity-generator)
       {::x x
        ::y y}))))

(comment
  (isa? ::rover ::entity)
  (gen/generate (s/gen ::uuid))
  (gen/generate (s/gen ::sands))
  (gen/generate (s/gen ::rover))
  (gen/generate (s/gen ::recharge))
  (gen/generate (s/gen ::entity))

  (gen/sample (s/gen ::entities) 5)

  (gen/sample (s/gen ::entity) 5)


  (gen-entities 3 3)


  ;;
  )
