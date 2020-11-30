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

(defonce state (scenario.render/create-state {}))
(scenario.core/create-watchers state)

(comment

  (swap! state assoc :random (rand-int 10))

  (scenario.chan/op
   {::op.spec/op-key ::scenario.chan/move-rover
    ::op.spec/op-type ::op.spec/fire-and-forget}
   channels
   {::scenario.core/x (rand-int scenario.core/x-size)
    ::scenario.core/y (rand-int scenario.core/y-size)})

  
  ;;
  )

(defn create-proc-ops
  [channels opts]
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
                (scenario.render/render-ui channels state {}))

              {::op.spec/op-key ::scenario.chan/move-rover
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (scenario.core/move-rover state value))

              {::op.spec/op-key ::scenario-api.chan/generate
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (println ::generate)
                (do (swap! state assoc ::scenario.core/ents
                           (scenario.core/gen-ents scenario.core/x-size scenario.core/y-size)) nil)
                (do (swap! state assoc ::scenario.core/rover (scenario.core/gen-rover)) nil))

              {::op.spec/op-key ::scenario-api.chan/reset
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (println ::reset))

              {::op.spec/op-key ::scenario-api.chan/resume
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (go
                  (loop [step 10]
                    (when (> step 0)
                      (let [rover (get @state ::scenario.core/rover)
                            ents-in-range (->>
                                               (scenario.core/filter-ents-in-range
                                                (get @state ::scenario.core/ents)
                                                rover)
                                               (scenario.core/filter-out-visited-locations
                                                (get @state ::scenario.core/visited-locations)))]
                        (when-let [response (<! (player.chan/op
                                                 {::op.spec/op-key ::player.chan/next-move
                                                  ::op.spec/op-type ::op.spec/request-response
                                                  ::op.spec/op-orient ::op.spec/request}
                                                 channels
                                                 {::scenario.core/rover rover
                                                  ::scenario.core/ents-in-range ents-in-range}))]
                          (scenario.core/add-location-to-visted
                           state
                           (select-keys response [::scenario.core/x ::scenario.core/y]))
                          (scenario.chan/op
                           {::op.spec/op-key ::scenario.chan/move-rover
                            ::op.spec/op-type ::op.spec/fire-and-forget}
                           channels
                           (select-keys response [::scenario.core/x ::scenario.core/y]))
                          (<! (timeout 1000))
                          (recur (dec step))))
                      )))
                (println ::resume))

              {::op.spec/op-key ::scenario-api.chan/pause
               ::op.spec/op-type ::op.spec/fire-and-forget}
              (let [{:keys []} value]
                (println ::pause)))))
        (recur)))))

(def rsocket (rsocket.impl/create-proc-ops
              channels
              {::rsocket.spec/connection-side ::rsocket.spec/initiating
               ::rsocket.spec/host "localhost"
               ::rsocket.spec/port RSOCKET_PORT
               ::rsocket.spec/transport ::rsocket.spec/websocket}))

(def ops (create-proc-ops channels {}))

(defn ^:export main
  []
  (println ::main)
  (println ::RSOCKET_PORT RSOCKET_PORT)
  (scenario.chan/op
   {::op.spec/op-key ::scenario.chan/init}
   channels
   {}))


(do (main))