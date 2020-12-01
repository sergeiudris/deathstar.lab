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

   ["@flatten-js/core"
    :default flattenjs
    :rename {BooleanOperations flattenjs.boolean
             Relations flattenjs.relations}]))


; https://github.com/sergeiudris/starnet/tree/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game2.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game3/data.cljc
; https://github.com/sergeiudris/starnet/blob/9002a81708a2317cbff88817093bba6182d0f110/system/test/starnet/pad/game1.cljc
; https://github.com/sergeiudris/starnet/blob/af86204ff94776ceab140208f5a6e0d654d30eba/common/test/starnet/common/pad/reagent1.cljs


(def ^:const x-size 63)
(def ^:const y-size 40)
(def ^:const box-size-px 14)

(s/def ::id uuid?)
(s/def ::entity-type keyword?)
(s/def ::x (s/with-gen
             int?
             #(gen/choose 0 x-size)))
(s/def ::y (s/with-gen
             int?
             #(gen/choose 0 y-size)))


(s/def ::x-offset int?)
(s/def ::y-offset int?)

(s/def ::energy-level number?)
(s/def ::energy number?)
(s/def ::rover-vision-range int?)
(s/def ::rover-scan-range int?)

(s/def ::scan-upgrade number?)
(s/def ::move-upgrade number?)
(s/def ::signal-tower-upgrade number?)
(s/def ::recharge-upgrade number?)


(s/def ::choose-location #{::closest ::highest-energy ::lowest-energy})
(s/def ::location-type #{::recharge ::signal-tower})
(s/def ::choose-upgrade #{::move ::signal-tower ::recharge ::scan})

(s/def ::rover (s/merge
                (s/keys :req [::id])
                (s/with-gen
                  (s/keys :req [::x
                                ::y
                                ::entity-type
                                ::energy-level
                                
                                ::scan-upgrade
                                ::move-upgrade
                                ::signal-tower-upgrade
                                ::recharge-upgrade
                                
                                ::rover-vision-range
                                ::rover-scan-range
                                ])
                  #(gen/hash-map
                    ::entity-type (gen/return ::rover)
                    ::x (gen/choose 26 34)
                    ::y (gen/choose 12 16)
                    ::energy-level (gen/return 100)
                    ::rover-vision-range (gen/return 4)
                    ::rover-scan-range (gen/return 8)

                    ::scan-upgrade (gen/return 1)
                    ::move-upgrade (gen/return 1)
                    ::signal-tower-upgrade (gen/return 1)
                    ::recharge-upgrade (gen/return 1)))))


(s/def ::signal-tower (s/merge
                       (s/keys :req [::id])
                       (s/with-gen
                         (s/keys :req [::x
                                       ::y
                                       ::entity-type])
                         #(gen/hash-map
                           ::entity-type (gen/return ::signal-tower)
                           ::x (gen/choose 0 x-size)
                           ::y (gen/choose 0 y-size)
                           ::energy (gen/choose -20 -5)))))
(derive ::signal-tower ::entity)


(s/def ::recharge (s/merge
                   (s/keys :req [::id])
                   (s/with-gen
                     (s/keys :req [::energy ::entity-type])
                     #(gen/hash-map
                       ::entity-type (gen/return ::recharge)
                       ::x (gen/choose 0 x-size)
                       ::y (gen/choose 0 y-size)
                       ::energy (gen/choose 10 30)))))
(derive ::recharge ::entity)


(s/def ::sands (s/merge
                (s/keys :req [::id])
                (s/with-gen
                  (s/keys :req [::energy ::entity-type])
                  #(gen/hash-map
                    ::entity-type (gen/return ::sands)
                    ::x (gen/choose 0 x-size)
                    ::y (gen/choose 0 y-size)
                    ::energy (gen/choose -20 -5))))) 
(derive ::sands ::entity)

(defmulti entity-mm (fn [ent] (::entity-type ent)))
(defmethod entity-mm ::signal-tower [ent] ::signal-tower)
(defmethod entity-mm ::recharge [ent] ::recharge)
(defmethod entity-mm ::rover [ent] ::rover)
(defmethod entity-mm ::sands [ent] ::sands)
(s/def ::entity (s/with-gen
                  (s/multi-spec entity-mm ::entity-type)
                  #(gen/frequency
                    [[200 (s/gen ::sands)]
                     [30 (s/gen ::recharge)]
                     [20 (s/gen ::signal-tower)]
                     [1 (s/gen ::rover)]])))
(def entity-gen (s/gen ::entity))

(s/def ::entities (s/with-gen
                    (s/map-of uuid? ::entity)
                    #(gen/hash-map
                      (gen/generate gen/uuid) entity-gen)))

(s/def ::state (s/keys :req [::entities
                             ::hovered-entity
                             ::visited-locations
                             ::entities-in-rovers-range
                             ::entities-in-rovers-range-per-rover
                             ::locations
                             ::rovers
                             ::visited-locations]))

(defn gen-entities
  [x y]
  (let [entity-generator (s/gen ::entity)]
    (->>
     (for [x (range 0 x)
           y (range 0 y)
           :let [entity (gen/generate entity-generator)]
           :when (not= (::entity-type entity) ::sands)]
       (do
         (merge
          entity
          {::x x
           ::y y})))
     (reduce (fn [result entity]
               (assoc result (::id entity) entity)) {}))))

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

(defn distance
  [entity1 entity2]
  (let [point1 (.point flattenjs (::x entity1)  (::y entity1))
        point2 (.point flattenjs (::x entity2)  (::y entity2))]
    (first (.distanceTo point1 point2))))


(defn rover-closest-location
  [state rover opts]
  (let [{:keys [::entities-in-rovers-range-per-rover
                ::visited-locations]} state
        {:keys [::location-type]} opts]
    (->> (get entities-in-rovers-range-per-rover (::id rover))
         (filter (fn [[k location]] (and
                                     (not=
                                      (select-keys rover [::x ::y])
                                      (select-keys location [::x ::y]))
                                     (not (get visited-locations k)))))
         (group-by (fn [[k location]] (::entity-type location)))
         (reduce (fn [result [entity-type locations]]
                   (->> locations
                        (sort-by (fn [[k location]]
                                   (distance rover location)))
                        (assoc result entity-type))) {})
         (keep (fn [[entity-type locations]]
                 (when (second (first locations))
                   [entity-type (second (first locations))])))
         (sort-by #(= (first %) location-type))
         (reverse)
         (first)
         (second))))


(defn visited-location
  [state rover location]
  (let []
    (assoc-in state [::visited-locations (::id location)]  location)))

(defn entities-to-groups
  [entities]
  (->>
   entities
   (reduce
    (fn [result [k entity]]
      (cond

        (= (::entity-type entity) ::rover)
        (assoc-in result [::rovers k] entity)

        :else
        (assoc-in result [::locations k] entity)))
    {})))

(defn entities-in-range
  [state]
  (let [{:keys [::rovers
                ::entities]} state
        
        make-range-geometry
        (fn [rover]
          (.circle
           flattenjs
           (.point flattenjs (::x rover) (::y rover))
           (::rover-vision-range rover)))

        make-entity-geometry
        (fn [entity]
          (.point flattenjs (::x entity) (::y entity)))

        geometries-intersect?
        (fn [range-geometry entity-geometry]
          (not (empty?
                (.intersect
                 range-geometry
                 entity-geometry))))

        partial-state
        (->>
         (for [[k-rover rover] rovers
               :let [range-geometry (make-range-geometry rover)]
               [k-entity entity] entities
               :let [entity-geometry (make-entity-geometry entity)]
               :when (geometries-intersect? range-geometry entity-geometry)]
           [[k-rover rover] [k-entity entity]])
         (reduce
          (fn [result [[k-rover rover] [k-entity entity]]]
            (-> result
                (assoc-in [::entities-in-rovers-range k-entity] entity)
                (assoc-in [::entities-in-rovers-range-per-rover k-rover k-entity] entity)))
          {}))]
    partial-state))


#_(defn create-state
    [opts]
    (let [state* (reagent.core/atom {})
          entities* (reagent.core/cursor state* [::entities])
          rovers* (reagent.core/cursor state* [::rovers])
          locations* (reagent.core/cursor state* [::locations])
          entities-in-rovers-range* (reagent.core/cursor state* [::entities-in-rovers-range])
          entities-in-rovers-range-per-rover* (reagent.core/cursor state* [::entities-in-rovers-range-per-rover])
          visited-locations* (reagent.core/cursor state* [::visited-locations])
          hovered-entity* (reagent.core/cursor state* [::hovered-entity])
          state  {::state* state*
                  ::entities* entities*
                  ::rovers* rovers*
                  ::locations* locations*
                  ::entities-in-rovers-range* entities-in-rovers-range*
                  ::entities-in-rovers-range-per-rover* entities-in-rovers-range-per-rover*
                  ::visited-locations* visited-locations*
                  ::hovered-entity* hovered-entity*}]
      (do
        (reagent.ratom/run-in-reaction
         (fn [] @entities*)
         state*
         ::derive-entity-groups
         (fn [_]
           (let [entities (::entities @state*)
                 partial-state
                 (->>
                  entities
                  (reduce
                   (fn [result [k entity]]
                     (cond

                       (= (::entity-type entity) ::rover)
                       (assoc-in result [::rovers k] entity)

                       :else
                       (assoc-in result [::locations k] entity)))
                   {})
                  (into {}))]
             (println ::derive-entity-groups)
             (println (count entities))
             (swap! state* merge partial-state)))
         {:no-cache false #_true})

        (reagent.ratom/run-in-reaction
         (fn [] @rovers*)
         state*
         ::entities-in-rovers-range
         (fn [_]
           )
         {:no-cache false #_true}))
      state))





#_(defn create-watchers
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
