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
   [reagent.core]
   [reagent.ratom]
   [reagent.dom]))


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

  (def log (r/track! (fn [& args]
                       (println ::log)
                       (println args)
                       (println @x3))))

  (swap! s assoc :foo :bar1) ; only x3 and log are invoked, as expected

  (reset! x2 5) ; Error Error: Assert failed: Reaction is read only

  ;;
  )


(comment

  ; Can't set! local var or non-mutable field
  (let [x 3]
    (println x)
    (set! x 4)
    (println x))

  (defprotocol Foo
    (-foo [_]))

  (def foo1 (reify Foo
              (-foo [_] 3)))

  (-foo foo1)
  (set! (.-dirty? foo1) true)
  (.-dirty? foo1)

  volatile!
  (apply + (concat [1 2] [3 4]))
  if-some

  ;;
  )

(comment

  (def state (r/atom {::x 0}))

  (def cursor-x (r/cursor state [::x]))
  
  (add-watch cursor-x ::x (fn [k atomref oldstate newstate]
                            (println @cursor-x)
                            (println ::atom-changed)
                            (println oldstate)
                            (println newstate)))
  #_(remove-watch cursor-x ::x)
  
  ; cursor is lazy, add-watch won't be invoked
  (swap! state assoc ::x 3)

  ;;
  )

(comment

  (def state (reagent.core/atom {::x 1
                                 ::foo ::foo
                                 ::baz ::baz}))
  (def c-x (reagent.core/cursor state [::x]))
  (def c-foo (reagent.core/cursor state [::foo]))
  (def c-baz (reagent.core/cursor state [::baz]))

  (reagent.ratom/run-in-reaction
   (fn []
     @c-foo
     @c-baz
     (do nil))
   state
   ::watch-x-foo
   (fn [_]
     (let [x (get @state ::x)]
       (swap! state assoc ::x (inc x))
       (println ::x (get @state ::x))))
   {:no-cache true #_true})
  
  (js-keys state)

  (swap! state assoc ::foo ::bar)
  (swap! state assoc ::baz ::bar)



  ;;
  )