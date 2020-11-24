(ns lab.render.konva
  (:require
   [clojure.core.async :as a :refer [chan go go-loop <! >!  take! put! offer! poll! alt! alts! close!
                                     pub sub unsub mult tap untap mix admix unmix pipe
                                     timeout to-chan  sliding-buffer dropping-buffer
                                     pipeline pipeline-async]]
   [goog.string.format :as format]
   [goog.string :refer [format]]
   [goog.object]
   [cljs.reader :refer [read-string]]
   [clojure.pprint :refer [pprint]]
   [reagent.core :as r]
   [reagent.dom :as rdom]

   ["react-konva" :rename {Stage KonvaStage
                           Layer KonvaLayer
                           Rect KonvaRect
                           Circle KonvaCircle}]))



(def konva-stage (r/adapt-react-class KonvaStage))
(def konva-layer (r/adapt-react-class KonvaLayer))
(def konva-rect (r/adapt-react-class KonvaRect))
(def konva-circle (r/adapt-react-class KonvaCircle))

(defn rc-konva-example-circle
  [channels state]
  (r/with-let []
    [konva-stage
     {:width js/window.innerWidth
      :height js/window.innerHeight}
     [konva-layer
      [konva-rect {:width 50
                   :height 50
                   :fill "red"}]
      [konva-circle {:width 200
                     :height 200
                     :stroke "black"
                     :radius 50
                     :fill "red"}]]]))