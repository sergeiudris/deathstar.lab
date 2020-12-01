(ns deathstar.scenario.main
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]
   #?(:cljs [cljs.core.async.interop :refer-macros [<p!]])
   #?(:cljs [goog.string.format :as format])
   #?(:cljs [goog.object])
   #?(:cljs [goog.string.format :as format])
   #?(:cljs [cljs.reader :refer [read-string]])

   [clojure.string :as string]
   [cljctools.csp.op.spec :as op.spec]
   [cljctools.cljc.core :as cljc.core]

   [cljctools.rsocket.spec :as rsocket.spec]
   [cljctools.rsocket.chan :as rsocket.chan]
   [cljctools.rsocket.impl :as rsocket.impl]

   [deathstar.scenario-api.chan :as scenario-api.chan]
   [deathstar.scenario-api.spec :as scenario-api.spec]

   [deathstar.scenario.player.spec :as player.spec]
   [deathstar.scenario.player.chan :as player.chan]

   [deathstar.scenario.spec :as scenario.spec]
   [deathstar.scenario.chan :as scenario.chan]
   [deathstar.scenario.core :as scenario.core]

   [deathstar.scenario.render :as scenario.render]

   [lab.render.reagent]
   [lab.render.konva]))

(goog-define RSOCKET_PORT 0)

(def channels (merge
               (scenario.chan/create-channels)
               (scenario-api.chan/create-channels)
               (player.chan/create-channels)
               (rsocket.chan/create-channels)))

(pipe (::rsocket.chan/requests| channels) (::scenario.chan/ops| channels))

#_(pipe (::scenario-api.chan/ops| channels) (::rsocket.chan/ops| channels))
(pipe (::scenario-api.chan/ops| channels) (::scenario.chan/ops| channels))
(pipe (::player.chan/ops| channels) (::rsocket.chan/ops| channels))

(defonce state* (scenario.render/create-state* {}))

(comment

  (swap! state* assoc :random (rand-int 10))

  (scenario.chan/op
   {::op.spec/op-key ::scenario.chan/move-rovers
    ::op.spec/op-type ::op.spec/fire-and-forget}
   channels
   {::scenario.core/x (rand-int scenario.core/x-size)
    ::scenario.core/y (rand-int scenario.core/y-size)})


  (scenario.chan/op
   {::op.spec/op-key ::scenario.chan/move-rovers
    ::op.spec/op-type ::op.spec/fire-and-forget}
   channels
   {::scenario.core/choose-location ::scenario.core/closest
    ::scenario.core/location-type ::scenario.core/signal-tower
    ::scenario.core/x-offset nil
    ::scenario.core/y-offset nil})
  
  (keys @state*)
  
  ;;
  )

(defn create-proc-ops
  [channels state* opts]
  (let [{:keys [::scenario.chan/ops|]} channels]
    (go
      (loop []
        (when-let [[value port] (alts! [ops|])]
          (condp = port
            ops|
            (condp = (select-keys value [::op.spec/op-key ::op.spec/op-type ::op.spec/op-orient])

              {::op.spec/op-key ::scenario.chan/init}
              (let [{:keys []} value]
                (println ::init)
                (scenario-api.chan/op
                 {::op.spec/op-key ::scenario-api.chan/generate
                  ::op.spec/op-type ::op.spec/fire-and-forget}
                 channels
                 {})
                (scenario.render/render-ui channels state* {}))

              {::op.spec/op-key ::scenario-api.chan/reset
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (println ::reset))

              {::op.spec/op-key ::scenario-api.chan/resume
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                #_(go
                    (loop [step 10]
                      (when (> step 0)
                        (let [rover (get @state* ::scenario.core/rover)
                              entities-in-range (->>
                                                 (scenario.core/filter-entities-in-range
                                                  (get @state* ::scenario.core/entities)
                                                  rover)
                                                 (scenario.core/filter-out-visited-locations
                                                  (get @state* ::scenario.core/visited-locations)))]
                          (when-let [response (<! (player.chan/op
                                                   {::op.spec/op-key ::player.chan/next-move
                                                    ::op.spec/op-type ::op.spec/request-response
                                                    ::op.spec/op-orient ::op.spec/request}
                                                   channels
                                                   {::scenario.core/rover rover
                                                    ::scenario.core/entities-in-range entities-in-range}))]
                            (scenario.core/add-location-to-visted
                             state*
                             (select-keys response [::scenario.core/x ::scenario.core/y]))
                            (scenario.chan/op
                             {::op.spec/op-key ::scenario.chan/move-rovers
                              ::op.spec/op-type ::op.spec/fire-and-forget}
                             channels
                             (select-keys response [::scenario.core/x ::scenario.core/y]))
                            (<! (timeout 1000))
                            (recur (dec step)))))))
                (println ::resume))

              {::op.spec/op-key ::scenario-api.chan/pause
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (println ::pause))

              {::op.spec/op-key ::scenario-api.chan/generate
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []}
                    value

                    entities
                    (scenario.core/gen-entities
                     scenario.core/x-size
                     scenario.core/y-size)

                    {:keys [::scenario.core/rovers
                            ::scenario.core/locations]
                     :as entities-groups}
                    (scenario.core/entities-to-groups entities)

                    {:keys [::scenario.core/entities-in-rovers-range
                            ::scenario.core/entities-in-rovers-range-per-rover]
                     :as entities-in-range}
                    (scenario.core/entities-in-range
                     {::scenario.core/entities entities
                      ::scenario.core/rovers rovers})]
                (println ::generate)
                (swap! state* merge
                       {::scenario.core/entities entities}
                       entities-groups
                       entities-in-range))

              {::op.spec/op-key ::scenario.chan/move-rovers
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys [::scenario.core/choose-location
                            ::scenario.core/location-type
                            ::scenario.core/x-offset
                            ::scenario.core/y-offset]} value
                    {:keys [::scenario.core/rovers]
                     :as state} @state*]
                (cond
                  (= choose-location ::scenario.core/closest)
                  (as-> nil result
                    (reduce
                     (fn [result [k-rover rover]]
                       (let [location (scenario.core/rover-closest-location state rover value)]
                         (-> result
                             (update-in [::scenario.core/rovers k-rover]
                                        merge (select-keys location [::scenario.core/x
                                                                     ::scenario.core/y]))
                             (scenario.core/visited-location rover location))))
                     state rovers)
                    (merge result (scenario.core/entities-in-range result))
                    (swap! state* merge result))))
              ;
              )))
        (recur)))))

(def rsocket (rsocket.impl/create-proc-ops
              channels
              {::rsocket.spec/connection-side ::rsocket.spec/initiating
               ::rsocket.spec/host "localhost"
               ::rsocket.spec/port RSOCKET_PORT
               ::rsocket.spec/transport ::rsocket.spec/websocket}))

(def ops (create-proc-ops channels state* {}))

(defn ^:export main
  []
  (println ::main)
  (println ::RSOCKET_PORT RSOCKET_PORT)
  (scenario.chan/op
   {::op.spec/op-key ::scenario.chan/init}
   channels
   {}))


(do (main))


