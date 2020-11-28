(ns deathstar.scenario.render
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
   [clojure.walk]

   [cljctools.csp.op.spec :as op.spec]
   [cljctools.cljc.core :as cljc.core]

   [deathstar.scenario.spec :as scenario.spec]
   [deathstar.scenario.chan :as scenario.chan]
   [deathstar.scenario.core :as scenario.core]

   ["antd/lib/layout" :default AntLayout]
   ["antd/lib/menu" :default AntMenu]
   ["antd/lib/icon" :default AntIcon]
   ["antd/lib/button" :default AntButton]
   ["antd/lib/list" :default AntList]
   ["antd/lib/row" :default AntRow]
   ["antd/lib/col" :default AntCol]
   ["antd/lib/form" :default AntForm]
   ["antd/lib/input" :default AntInput]
   ["antd/lib/tabs" :default AntTabs]
   ["antd/lib/table" :default AntTable]
   ["react" :as React]
   ["antd/lib/checkbox" :default AntCheckbox]
   ["antd/lib/progress" :default AntProgress]



   ["antd/lib/divider" :default AntDivider]
   ["@ant-design/icons/SmileOutlined" :default AntIconSmileOutlined]
   ["@ant-design/icons/LoadingOutlined" :default AntIconLoadingOutlined]
   ["@ant-design/icons/SyncOutlined" :default AntIconSyncOutlined]


   ["konva" :default Konva]
   ["react-konva" :as ReactKonva :rename {Stage KonvaStage
                                          Layer KonvaLayer
                                          Rect KonvaRect
                                          Path KonvaPath
                                          Circle KonvaCircle
                                          Group KonvaGroup
                                          Wedge KonvaWedge}]

   ["@flatten-js/core" :default flattenjs]))


(def ant-row (r/adapt-react-class AntRow))
(def ant-col (r/adapt-react-class AntCol))
(def ant-divider (r/adapt-react-class AntDivider))
(def ant-layout (r/adapt-react-class AntLayout))
(def ant-layout-content (r/adapt-react-class (.-Content AntLayout)))
(def ant-layout-header (r/adapt-react-class (.-Header AntLayout)))

(def ant-menu (r/adapt-react-class AntMenu))
(def ant-menu-item (r/adapt-react-class (.-Item AntMenu)))
(def ant-icon (r/adapt-react-class AntIcon))
(def ant-button (r/adapt-react-class AntButton))
(def ant-button-group (r/adapt-react-class (.-Group AntButton)))
(def ant-list (r/adapt-react-class AntList))
(def ant-input (r/adapt-react-class AntInput))
(def ant-input-password (r/adapt-react-class (.-Password AntInput)))
(def ant-checkbox (r/adapt-react-class AntCheckbox))
(def ant-form (r/adapt-react-class AntForm))
(def ant-table (r/adapt-react-class AntTable))
(def ant-form-item (r/adapt-react-class (.-Item AntForm)))
(def ant-tabs (r/adapt-react-class AntTabs))
(def ant-tab-pane (r/adapt-react-class (.-TabPane AntTabs)))
(def ant-progress (r/adapt-react-class AntProgress))

(def ant-icon-smile-outlined (r/adapt-react-class AntIconSmileOutlined))
(def ant-icon-loading-outlined (r/adapt-react-class AntIconLoadingOutlined))
(def ant-icon-sync-outlined (r/adapt-react-class AntIconSyncOutlined))

; https://github.com/sergeiudris/starnet/blob/af86204ff94776ceab140208f5a6e0d654d30eba/ui/src/starnet/ui/alpha/main.cljs
; https://github.com/sergeiudris/starnet/blob/af86204ff94776ceab140208f5a6e0d654d30eba/ui/src/starnet/ui/alpha/render.cljs


(def konva-stage (r/adapt-react-class KonvaStage))
(def konva-layer (r/adapt-react-class KonvaLayer))
(def konva-rect (r/adapt-react-class KonvaRect))
(def konva-circle (r/adapt-react-class KonvaCircle))
(def konva-group (r/adapt-react-class KonvaGroup))
(def konva-path (r/adapt-react-class KonvaPath))
(def konva-wedge (r/adapt-react-class KonvaWedge))



(defn create-state
  [data]
  (r/atom data))

(declare  rc-main rc-grid rc-entity rc-rover)

(defn render-ui
  [channels state {:keys [id] :or {id "ui"}}]
  (rdom/render [rc-main channels state]  (.getElementById js/document id)))

(defn rc-main
  [channels state]
  (r/with-let []
    [:<>
     #_[:pre {} (with-out-str (pprint @state))]
     #_[ant-button {:icon (r/as-element [ant-icon-sync-outlined])
                    :size "small"
                    :title "button"
                    :on-click (fn [] ::button-click)}]
     [rc-grid channels state]
     [rc-entity channels state]
     [rc-rover channels state]
     
     
     #_[lab.render.konva/rc-konva-grid channels state]
     #_[lab.render.konva/rc-konva-example-circle channels state]]))

(def colors
  {::scenario.core/sands "#e9c48c" #_"#f7e5a9" #_"#D2B48Cff"
   ::scenario.core/location "brown"
   ::scenario.core/recharge "#30ad23"
   ::scenario.core/rover "blue"})


(defn rc-grid
  [channels state]
  (r/with-let [entities* (r/cursor state [::scenario.core/entities])
               rover* (r/cursor state [::scenario.core/rover])
               entities-in-range* (r/cursor state [::scenario.core/entities-in-range])
               selected-entity* (r/cursor state [::scenario.core/selected-entity])
               visited-locations* (r/cursor state [::scenario.core/visited-locations])
              ;;  width js/window.innerWidth
              ;;  height js/window.innerHeight
               box-size scenario.core/box-size-px

               entity-on-mouse-over (fn [evt]
                                      (let [node (.-target evt)
                                            entity (or (get @entities* (.id node)) @rover*)
                                            {:keys [::scenario.core/x ::scenario.core/y]} entity
                                            stage (.getStage node)
                                            layer-terrain (.findOne stage "#terrain")
                                            node-terrain (.findOne layer-terrain (str "#sand-" x "-" y))]
                                        #_(println (.id box))
                                        #_(println (get @entities* (.id box)))
                                        (swap! state assoc ::scenario.core/hovered-entity entity)
                                        #_(println (js-keys box))
                                        #_(println (.id box))
                                        #_(.fill box "#E5FF80")
                                        #_(.strokeWidth node 2)
                                        #_(.stroke node "white")
                                        #_(.brightness node 0)
                                        #_(.scale node #js {:x 1.2 :y 1.2})
                                        #_(.draw stage)
                                        (.fill node "#E5FF80")
                                        (.draw node)))
               entity-on-mouse-out (fn [evt]
                                     (let [node (.-target evt)
                                           entity (or (get @entities* (.id node)) @rover*)
                                           {:keys [::scenario.core/x ::scenario.core/y]} entity
                                           stage (.getStage node)
                                           layer-terrain (.findOne stage "#terrain")
                                           node-terrain (.findOne layer-terrain (str "#sand-" x "-" y))]
                                       #_(println (.id node-terrain))
                                       #_(println (.id node))
                                       #_(.fill box (::scenario.core/color entity))
                                       #_(.fill node-terrain "red")
                                       (.fill node (get colors (::scenario.core/entity-type entity)))
                                       #_(.draw node-terrain)
                                       #_(.strokeWidth node 0.001)
                                       #_(.stroke node "red")
                                       #_(.scale node #js {:x 1 :y 1})
                                       #_(.draw stage)
                                       #_(.brightness node 0.5)
                                       (.draw node)))]
    (let [entities-in-range @entities-in-range*
          visited-locations @visited-locations*
          selected-entity @selected-entity*]
      [:<>
       [:div "Get to the Ship"]
       [konva-stage
        {:width (* box-size scenario.core/x-size)
         :height (* box-size scenario.core/y-size)}
        [konva-layer
         {:id "terrain"
          :on-click (fn [evt]
                      (let [box (.-target evt)
                            entity (get @entities* (.id box))]
                        (scenario.chan/op
                         {::op.spec/op-key ::scenario.chan/click-entity
                          ::op.spec/op-type ::op.spec/fire-and-forget}
                         channels
                         entity)))
          :on-dblclick (fn [evt]
                         (let [box (.-target evt)]
                           (scenario.chan/op
                            {::op.spec/op-key ::scenario.chan/click-entity
                             ::op.spec/op-type ::op.spec/fire-and-forget}
                            channels
                            nil)))
          :on-mouseover (fn [evt]
                          (let [box (.-target evt)
                                entity (or (get @entities* (.id box)) @rover*)]
                            (swap! state assoc ::scenario.core/hovered-entity entity)
                            (.stroke box "white")
                            (.strokeWidth box 2)
                            (.draw box)))
          :on-mouseout (fn [evt]
                         (let [box (.-target evt)]
                           (.strokeWidth box 0.001)
                           (.stroke box false)
                           (.draw box)))}
         
          (map (fn [entity]
                (let [{:keys [::scenario.core/entity-type
                              ::scenario.core/x
                              ::scenario.core/y
                              ::scenario.core/id
                              ::scenario.core/color]} entity
                      in-range? false]
                  [konva-rect {:key (str x "-" y)
                               :width (- box-size 1)
                               :height (- box-size 1)
                               :id id
                               :x (* x box-size)
                               :y (* y box-size)
                               :fill (::scenario.core/sands colors)
                               :strokeWidth 0.001
                               :stroke "white"}])) (vals @entities*))
         #_(for [x (range 0 scenario.core/x-size)
                 y (range 0 scenario.core/y-size)]
             [konva-rect {:key (str x "-" y)
                          :width (- box-size 1)
                          :height (- box-size 1)
                          :id (str "sand-" x "-" y)
                          :x (* x box-size)
                          :y (* y box-size)
                          :fill (::scenario.core/sands colors)
                          :strokeWidth 0.001
                          :stroke "white"}])]
        [konva-layer
         {:on-mouseover entity-on-mouse-over
          :on-mouseout entity-on-mouse-out
          :on-click (fn [evt]
                      (let [box (.-target evt)
                            entity (or (get @entities* (.id box)) @rover*)]
                        (scenario.chan/op
                         {::op.spec/op-key ::scenario.chan/click-entity
                          ::op.spec/op-type ::op.spec/fire-and-forget}
                         channels
                         entity)))}
         (map (fn [entity]
                (let [{:keys [::scenario.core/entity-type
                              ::scenario.core/x
                              ::scenario.core/y
                              ::scenario.core/id
                              ::scenario.core/energy
                              ::scenario.core/energy-min
                              ::scenario.core/energy-max
                              ::scenario.core/color]} entity
                      in-range? false
                      visited-location? (boolean (get visited-locations id))]
                  (when (and (not= entity-type ::scenario.core/sands)
                             (not visited-location?))
                    (condp = entity-type

                      ::scenario.core/location
                      [konva-wedge {:key (str x "-" y)
                                    :x (+ (* x box-size) (/ box-size 2) -0.5)
                                    :y (+ (* y box-size) (/ box-size 2) 2)
                                    :id id
                                    :radius 7
                                    :angle 50
                                    :rotation -115
                              ;; :filters #js [(.. Konva -Filters -Brighten)]
                                    :fill (if visited-location? "teal" (get colors entity-type))
                                    :strokeWidth (if (= (::scenario.core/id selected-entity) id)
                                                   2
                                                   0)
                                    :stroke "white"}]
                   ;deafult
                      [konva-circle {:key (str x "-" y)
                                     :x (+ (* x box-size) (/ box-size 2) -0.5)
                                     :y (+ (* y box-size) (/ box-size 2) -0.5)
                                     :id id
                                     :radius (+ 2 (* 3 (/ (- energy energy-min) (- energy-max  energy-min))))
                              ;; :filters #js [(.. Konva -Filters -Brighten)]
                                     :fill (get colors entity-type)
                                     :strokeWidth (if (= (::scenario.core/id selected-entity) id)
                                                    2
                                                    0)
                                     :stroke "white"}])
                    #_[konva-rect {:key (str x "-" y)
                                   :x (+ (* x box-size) 2)
                                   :y (+ (* y box-size) 2)
                                   :id id
                                   :width (- box-size 5)
                                   :height (- box-size 5)
                              ;; :filters #js [(.. Konva -Filters -Brighten)]
                                   :fill (get colors entity-type)
                                   :strokeWidth 0.001
                                   :stroke "white"}]))) (vals @entities*))]
        (let [{:keys [::scenario.core/x
                      ::scenario.core/y
                      ::scenario.core/id
                      ::scenario.core/rover-vision-range]} @rover*]
          [:<>
           [konva-layer
            {:on-click (fn [evt]
                         (let [box (.-target evt)
                               entity (or (get @entities* (.id box)) @rover*)]
                           (scenario.chan/op
                            {::op.spec/op-key ::scenario.chan/click-entity
                             ::op.spec/op-type ::op.spec/fire-and-forget}
                            channels
                            entity)))
             :on-mouseover (fn [evt]
                             (let [node (.-target evt)
                                   entity @rover*
                                   {:keys [::scenario.core/x ::scenario.core/y]} entity
                                   stage (.getStage node)
                                   layer-range (.findOne stage "#rover-range")]
                               (swap! state assoc ::scenario.core/hovered-entity entity)
                               #_(.show layer-range)
                               #_(.draw layer-range)
                               (.fill node "#E5FF80")
                               (.draw node)))
             :on-mouseout (fn [evt]
                            (let [node (.-target evt)
                                  entity @rover*
                                  {:keys [::scenario.core/x ::scenario.core/y]} entity
                                  stage (.getStage node)
                                  layer-range (.findOne stage "#rover-range")]
                              #_(.hide layer-range)
                              #_(.draw layer-range)
                              (.fill node (get colors (::scenario.core/entity-type entity)))
                              (.draw node)))}
            [konva-circle {:x (+ (* x box-size) (/ box-size 2) -0.5)
                           :y (+ (* y box-size) (/ box-size 2) -0.5)
                           :id id
                           :radius 4
                           :fill (get colors ::scenario.core/rover)
                           :strokeWidth (if (= (::scenario.core/id selected-entity) id)
                                          2
                                          0)
                           :stroke "white"}]]
           [konva-layer
            {:id "rover-range"}
            (when (= (::scenario.core/entity-type selected-entity) ::scenario.core/rover)
              [konva-circle {:x (+ (* x box-size) (/ box-size 2) -0.5)
                             :y (+ (* y box-size) (/ box-size 2) -0.5)
                             :id id
                             :radius (* box-size rover-vision-range)
                             :strokeWidth 1
                             :strokeHitEnabled false
                             :fillEnabled false
                             :stroke "darkblue"}])]])]])))

(defn rc-entity
  [channels state]
  (r/with-let [hovered-entity* (r/cursor state [::scenario.core/hovered-entity])]
    [:div {:style {:position "absolute" 
                   :top (+ 20
                         (* scenario.core/box-size-px scenario.core/y-size))
                   :left 0 
                   :max-width "464px"
                   :background-color "#ffffff99"}}
     [:pre
      (with-out-str (pprint
                     (-> @hovered-entity*
                         (clojure.walk/stringify-keys)
                         (clojure.walk/keywordize-keys))))]]))

(defn rc-rover
  [channels state]
  (r/with-let [rover* (r/cursor state [::scenario.core/rover])]
    (let [energy-level (get @rover* ::scenario.core/energy-level)]
      [:<>
       (when energy-level
         [ant-progress {:percent energy-level
                        :size "default" #_"small"
                        :format (fn [percent success-percent]
                                  (format "%.4f" percent))
                        :style {:position "absolute"
                                :width "300px"
                                :top (+ 20
                                        (* scenario.core/box-size-px scenario.core/y-size))
                                :left 464
                                :max-width "464px"
                                :background-color "#ffffff99"}}])
       [:div {:style {:position "absolute"
                      :top (+ 20
                              (* scenario.core/box-size-px scenario.core/y-size)
                              20
                              )
                      :left 464
                      :max-width "464px"
                      :background-color "#ffffff99"}}
        [:pre
         (with-out-str (pprint
                        (-> @rover*
                            (clojure.walk/stringify-keys)
                            (clojure.walk/keywordize-keys))))]]])))