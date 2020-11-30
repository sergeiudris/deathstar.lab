(ns lab.render.reagent
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
   [reagent.dom :as rdom]))


(comment

  (def s (r/atom {:x 0
                  :foo :foo}))
  (def x0 (r/cursor s [:x]))
  (def foo0 (r/cursor s [:foo]))

  (def x1 (r/track! (fn []
                      (println ::x1)
                      (inc @x0))))

  (def x2 (r/track! (fn []
                      (println ::x2)
                      (inc @x1))))



  (def x3 (r/track! (fn []
                      (println ::x3)
                      {:x (inc @x2)
                       :foo @foo0})))

  (def log (r/track! (fn []
                       (println ::log)
                       (println @x3))))

  (swap! s assoc :foo :bar) ; only x3 and log are invoked, as expected


  ;;
  )