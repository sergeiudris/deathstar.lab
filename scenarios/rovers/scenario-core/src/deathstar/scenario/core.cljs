(ns deathstar.scenario.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]

   [reagent.core]
   [reagent.dom]
   [reagent.ratom]

   ["@flatten-js/core" :default fjs :rename {BooleanOperations fjsbool
                                             Relations fjsrel}]))


; https://github.com/sergeiudris/starnet/tree/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game2.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game3/data.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game1.cljc
; https://github.com/sergeiudris/starnet/blob/af86204ff94776ceab140208f5a6e0d654d30eba/common/test/starnet/common/pad/reagent1.cljs
(defn spec-number-in-range
  [spec_ min_ max_]
  (s/with-gen
    spec_
    #(gen/large-integer* {:min min_ :max max_})))


(def ^:const x-size 63)
(def ^:const y-size 40)
(def ^:const box-size-px 14)

(s/def ::id uuid?)
(s/def ::entity-type keyword?)
(s/def ::x (s/with-gen
             int?
             #(gen/large-integer* {:min 0 :max x-size})))
(s/def ::y (s/with-gen
             int?
             #(gen/large-integer* {:min 0 :max x-size})))


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
                       ::x (gen/large-integer* {:min 0 :max x-size})
                       ::y (gen/large-integer* {:min 0 :max x-size})))))
(derive ::location ::entity)

(s/def ::energy number?)
(s/def ::recharge (s/merge
                   (s/keys :req [::id])
                   (s/with-gen
                     (s/keys :req [::energy ::entity-type])
                     #(gen/hash-map
                       ::entity-type (gen/return ::recharge)
                       ::x (gen/large-integer* {:min 0 :max x-size})
                       ::y (gen/large-integer* {:min 0 :max x-size})
                       ::energy (gen/large-integer* {:min 10 :max 30})))))
(derive ::recharge ::entity)


(s/def ::sands (s/merge
                (s/keys :req [::id])
                (s/with-gen
                  (s/keys :req [::energy ::entity-type])
                  #(gen/hash-map
                    ::entity-type (gen/return ::sands)
                    ::x (gen/large-integer* {:min 0 :max x-size})
                    ::y (gen/large-integer* {:min 0 :max x-size})
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

(s/def ::hovered-entity any?)
(s/def ::entities-in-range ::entities)
(s/def ::visited-locations ::entities)

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



(defn filter-entities-in-range
  [entites rover]
  (let [fjs-rover-range (.circle fjs
                                 (.point fjs (::x rover)  (::y rover))
                                 (::rover-vision-range rover))
        entities-in-range (into {}
                                (comp
                                 (filter (fn [[k entity]]
                                           (and
                                            (not (= (::entity-type entity) ::sands))
                                            (not (empty?
                                                  (.intersect
                                                   fjs-rover-range
                                                   (.point fjs (::x entity) (::y entity))))))
                                           #_(.intersect fjsrel
                                                         fjs-rover-range
                                                         (.point fjs (::x v) (::y v))))))
                                entites)]
    entities-in-range))

(defn filter-out-visited-locations
  [visited-locations entities]
  (let [result (into {}
                     (comp
                      (filter (fn [[k value]]
                                (not (get visited-locations k)))))
                     entities)]
    result))

(defn move-rover
  [state value]
  (let []
    (swap! state update ::rover merge (select-keys value [::x ::y]))))

(defn add-location-to-visted
  [state {:keys [::x ::y] :as value}]
  (let [entites (get @state ::entities)
        location (second (first (filter (fn [[k entity]]
                                          (= (select-keys entity [::x ::y])
                                             (select-keys value [::x ::y]))) entites)))]
    (swap! state update ::visited-locations assoc (::id location) location)))

(defn create-watchers
  [state]
  (let [rover* (reagent.core/cursor state [::rover])
        entities* (reagent.core/cursor state [::entities])
        trackf-entities-in-range (fn []
                                   (let [rover @rover*
                                         entities @entities*]
                                     (when (and rover entities)
                                       (let [entities-in-range (filter-entities-in-range entities rover)]
                                         #_(println (count entities-in-range))
                                         (swap! state assoc ::entities-in-range entities-in-range)))
                                     #_(println (count entities))
                                     #_(println (select-keys [::x ::y] rover))))
        tracked-entities-in-range (reagent.core/track! trackf-entities-in-range)]
    #_(add-watch state ::watch-state
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

(defn create-state
  [opts]
  (let [state* (reagent.core/atom {})
        entities* (reagent.core/cursor state* [::entities])
        rovers* (reagent.core/cursor state* [::rovers])]
    (do
      (reagent.ratom/run-in-reaction
       (fn [] @entities*)
       state*
       ::rovers
       (fn [_]
         (let [entities (get @state* ::entities)
               rovers (into {}
                            (filter (fn [[id entity]]
                                      (= (::entity-type entity) ::rover)))
                            entities)]
           (swap! state* assoc ::rovers rovers)))
       {:no-cache false #_true}))
    {::state* state*
     ::entities* entities*
     ::rovers* rovers*}))


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

