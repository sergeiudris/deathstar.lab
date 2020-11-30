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
(s/def ::ent-type keyword?)
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
                                ::ent-type
                                ::energy-level
                                ::rover-vision-range
                                ::rover-scan-range
                                ::rover-travel-range])
                  #(gen/hash-map
                    ::ent-type (gen/return ::rover)
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
                                   ::ent-type])
                     #(gen/hash-map
                       ::ent-type (gen/return ::location)
                       ::x (gen/large-integer* {:min 0 :max x-size})
                       ::y (gen/large-integer* {:min 0 :max x-size})))))
(derive ::location ::ent)

(s/def ::energy number?)
(s/def ::recharge (s/merge
                   (s/keys :req [::id])
                   (s/with-gen
                     (s/keys :req [::energy ::ent-type])
                     #(gen/hash-map
                       ::ent-type (gen/return ::recharge)
                       ::x (gen/large-integer* {:min 0 :max x-size})
                       ::y (gen/large-integer* {:min 0 :max x-size})
                       ::energy (gen/large-integer* {:min 10 :max 30})))))
(derive ::recharge ::ent)


(s/def ::sands (s/merge
                (s/keys :req [::id])
                (s/with-gen
                  (s/keys :req [::energy ::ent-type])
                  #(gen/hash-map
                    ::ent-type (gen/return ::sands)
                    ::x (gen/large-integer* {:min 0 :max x-size})
                    ::y (gen/large-integer* {:min 0 :max x-size})
                    ::energy (gen/large-integer* {:min -20 :max -5})))))
(derive ::sands ::ent)

(defmulti ent-mm (fn [ent] (::ent-type ent)))
(defmethod ent-mm ::location [ent] ::location)
(defmethod ent-mm ::recharge [ent] ::recharge)
(defmethod ent-mm ::sands [ent] ::sands)
(s/def ::ent (s/with-gen
                  (s/multi-spec ent-mm ::ent-type)
                  #(gen/frequency
                    [[80 (s/gen ::sands)]
                     [15 (s/gen ::recharge)]
                     [5 (s/gen ::location)]])))


(s/def ::ents (s/with-gen
                    (s/map-of uuid? ::ent)
                    #(gen/hash-map
                      (gen/generate gen/uuid) (gen/frequency
                                               [[70 (s/gen ::sands)]
                                                [20 (s/gen ::recharge)]
                                                [10 (s/gen ::location)]]))))

(s/def ::hovered-ent any?)
(s/def ::ents-in-range ::ents)
(s/def ::visited-locations ::ents)

(defn gen-ents
  [x y]
  (let [ent-generator (s/gen ::ent)]
    (->>
     (for [x (range 0 x)
           y (range 0 y)]
       (merge
        (gen/generate ent-generator)
        {::x x
         ::y y}))
     (reduce (fn [result ent]
               (assoc result (::id ent) ent)) {}))))

(defn gen-rover
  []
  (gen/generate (s/gen ::rover)))



(defn filter-ents-in-range
  [entites rover]
  (let [fjs-rover-range (.circle fjs
                                 (.point fjs (::x rover)  (::y rover))
                                 (::rover-vision-range rover))
        ents-in-range (into {}
                                (comp
                                 (filter (fn [[k ent]]
                                           (and
                                            (not (= (::ent-type ent) ::sands))
                                            (not (empty?
                                                  (.intersect
                                                   fjs-rover-range
                                                   (.point fjs (::x ent) (::y ent))))))
                                           #_(.intersect fjsrel
                                                         fjs-rover-range
                                                         (.point fjs (::x v) (::y v))))))
                                entites)]
    ents-in-range))

(defn filter-out-visited-locations
  [visited-locations ents]
  (let [result (into {}
                     (comp
                      (filter (fn [[k value]]
                                (not (get visited-locations k)))))
                     ents)]
    result))

(defn move-rover
  [state value]
  (let []
    (swap! state update ::rover merge (select-keys value [::x ::y]))))

(defn add-location-to-visted
  [state {:keys [::x ::y] :as value}]
  (let [entites (get @state ::ents)
        location (second (first (filter (fn [[k ent]]
                                          (= (select-keys ent [::x ::y])
                                             (select-keys value [::x ::y]))) entites)))]
    (swap! state update ::visited-locations assoc (::id location) location)))

(defn create-watchers
  [state]
  (let [rover* (r/cursor state [::rover])
        ents* (r/cursor state [::ents])
        trackf-ents-in-range (fn []
                                   (let [rover @rover*
                                         ents @ents*]
                                     (when (and rover ents)
                                       (let [ents-in-range (filter-ents-in-range ents rover)]
                                         (println (count ents-in-range))
                                         (swap! state assoc ::ents-in-range ents-in-range)))
                                     #_(println (count ents))
                                     #_(println (select-keys [::x ::y] rover))))
        tracked-ents-in-range (r/track! trackf-ents-in-range)]
    #_(add-watch state ::watch-state
                 (fn [key atom-ref old-state new-state]
                   (when (and
                          (::ents new-state) (::rover new-state)
                          (or
                           (not (identical? (::ents old-state) (::ents new-state)))
                           (not (identical? (::rover old-state) (::rover new-state)))))
                     (let [entites (::ents new-state)
                           rover (::rover new-state)
                           ents-in-range (filter-ents-in-range entites rover)]
                       (swap! state assoc ::ents-in-range ents-in-range)))))
    #_(add-watch rover* ::watch-rover
                 (fn [key atom-ref old-state new-state]
                   (println ::watch-rover)))))


(comment
  
  (isa? ::rover ::ent)
  (gen/generate (s/gen ::id))
  (gen/generate (s/gen ::sands))
  (gen/generate (s/gen ::rover))
  (gen/generate (s/gen ::recharge))
  (gen/generate (s/gen ::ent))

  (gen/sample (s/gen ::ents) 5)

  (gen/sample (s/gen ::ent) 5)


  (gen-ents 3 3)


  ;;
  )

