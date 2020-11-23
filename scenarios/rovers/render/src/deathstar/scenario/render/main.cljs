(ns deathstar.scenario.render.main
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [cljs.core.async.impl.protocols :refer [closed?]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [goog.string.format :as format]
   [goog.string :refer [format]]
   [goog.object]
   [clojure.string :as string]
   [cljs.reader :refer [read-string]]

   [cljctools.csp.op.spec :as op.spec]
   [cljctools.cljc.core :as cljc.core]

   [cljctools.rsocket.spec :as rsocket.spec]
   [cljctools.rsocket.chan :as rsocket.chan]
   [cljctools.rsocket.impl :as rsocket.impl]

   [deathstar.scenario.spec :as scenario.spec]
   [deathstar.scenario.chan :as scenario.chan]

   [deathstar.scenario.render.chan :as render.chan]
   [deathstar.scenario.render.spec :as render.spec]

   [deathstar.scenario.render.impl :as render.impl]))

(goog-define RSOCKET_PORT 0)

(def channels (merge
               (render.chan/create-channels)
               (scenario.chan/create-channels)
               (rsocket.chan/create-channels)))

(pipe (::rsocket.chan/requests| channels) (::render.chan/ops| channels))

(pipe (::scenario.chan/ops| channels) (::rsocket.chan/ops| channels))

(def state (render.impl/create-state
            {}))

(comment
  
  (swap! state assoc :random (rand-int 10))
  
  ;;
  )

(defn create-proc-ops
  [channels opts]
  (let [{:keys [::render.chan/ops|]} channels]
    (go
      (loop []
        (when-let [[value port] (alts! [ops|])]
          (condp = port
            ops|
            (condp = (select-keys value [::op.spec/op-key ::op.spec/op-type ::op.spec/op-orient])

              {::op.spec/op-key ::render.chan/init}
              (let [{:keys []} value]
                (println ::init)
                (render.impl/render-ui channels state {})))))
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
  (render.chan/op
   {::op.spec/op-key ::render.chan/init}
   channels
   {}))


(do (main))