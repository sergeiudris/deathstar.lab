(ns deathstar.scenario.main
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]
   #?(:cljs [cljs.core.async.interop :refer-macros [<p!]])
   #?(:cljs [goog.string.format])
   #?(:cljs [goog.string :refer [format]])
   #?(:cljs [goog.object])
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
   {::scenario.spec/x (rand-int scenario.spec/x-size)
    ::scenario.spec/y (rand-int scenario.spec/y-size)})


  (scenario.chan/op
   {::op.spec/op-key ::scenario.chan/move-rovers
    ::op.spec/op-type ::op.spec/fire-and-forget}
   channels
   {::scenario.spec/choose-location ::scenario.spec/closest
    ::scenario.spec/location-type ::scenario.spec/signal-tower
    ::scenario.spec/x-offset nil
    ::scenario.spec/y-offset nil})
  
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
                (reset! state* {})
                (scenario-api.chan/op
                 {::op.spec/op-key ::scenario-api.chan/generate
                  ::op.spec/op-type ::op.spec/fire-and-forget}
                 channels
                 {}))

              {::op.spec/op-key ::scenario-api.chan/resume
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (println ::resume)
                (go
                  (loop [step 1]
                    (when (<= step 10)
                      (let []
                        (when-let [[response port] (alts! [(player.chan/op
                                                            {::op.spec/op-key ::player.chan/next-move
                                                             ::op.spec/op-type ::op.spec/request-response
                                                             ::op.spec/op-orient ::op.spec/request}
                                                            channels
                                                            {::scenario.spec/step step})
                                                           (timeout 50)])]
                          (let [{:keys [::scenario.chan/op]} response]
                            (if op
                              (scenario.chan/op
                               op
                               channels
                               op)
                              (println (format "no operation on step %s" step)))
                            (<! (timeout 1000))
                            (recur (inc step)))))))))

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
                     scenario.spec/x-size
                     scenario.spec/y-size)

                    {:keys [::scenario.spec/rovers
                            ::scenario.spec/locations]
                     :as entities-groups}
                    (scenario.core/entities-to-groups entities)

                    {:keys [::scenario.spec/entities-in-rovers-range
                            ::scenario.spec/entities-in-rovers-range-per-rover]
                     :as entities-in-range}
                    (scenario.core/entities-in-range
                     {::scenario.spec/entities entities
                      ::scenario.spec/rovers rovers})]
                (println ::generate)
                (swap! state* merge
                       {::scenario.spec/entities entities}
                       entities-groups
                       entities-in-range))

              {::op.spec/op-key ::scenario.chan/move-rovers
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys [::scenario.spec/choose-location
                            ::scenario.spec/location-type
                            ::scenario.spec/x-offset
                            ::scenario.spec/y-offset]} value
                    {:keys [::scenario.spec/rovers]
                     :as state} @state*]
                (cond
                  (= choose-location ::scenario.spec/closest)
                  (as-> nil result
                    (reduce
                     (fn [result [k-rover rover]]
                       (let [location (scenario.core/rover-closest-location state rover value)]
                         (-> result
                             (scenario.core/rover-visits-location rover location))))
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


