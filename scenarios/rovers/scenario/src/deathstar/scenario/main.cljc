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

  ;;  [cljctools.rsocket.spec :as rsocket.spec]
  ;;  [cljctools.rsocket.chan :as rsocket.chan]
  ;;  [cljctools.rsocket.impl :as rsocket.impl]

   [deathstar.scenario-api.chan :as scenario-api.chan]
   [deathstar.scenario-api.spec :as scenario-api.spec]

   [deathstar.scenario.player.spec :as scenario.player.spec]
   [deathstar.scenario.player.chan :as scenario.player.chan]

   [deathstar.scenario.spec :as scenario.spec]
   [deathstar.scenario.chan :as scenario.chan]))

(goog-define RSOCKET_PORT 0)

(def channels (merge
               (scenario.chan/create-channels)
               (player.chan/create-channels)
               (rsocket.chan/create-channels)))

(pipe (::rsocket.chan/requests| channels) (::scenario.chan/ops| channels))

(pipe (::scenario-api.chan/ops| channels) (::rsocket.chan/ops| channels))
(pipe (::player.chan/ops| channels) (::rsocket.chan/ops| channels))

(def state (atom {}))

(comment
  
  (swap! state assoc :random (rand-int 10))
  
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
                (println ::init)))))
        (recur)))))

#_(def rsocket (rsocket.impl/create-proc-ops
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