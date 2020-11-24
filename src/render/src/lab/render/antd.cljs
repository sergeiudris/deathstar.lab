(ns lab.render.antd
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
   ["antd/lib/divider" :default AntDivider]
   ["@ant-design/icons/SmileOutlined" :default AntIconSmileOutlined]
   ["@ant-design/icons/LoadingOutlined" :default AntIconLoadingOutlined]
   ["@ant-design/icons/SyncOutlined" :default AntIconSyncOutlined]
   ["@ant-design/icons/ReloadOutlined" :default AntIconReloadOutlined]))


; https://github.com/sergeiudris/starnet/blob/af86204ff94776ceab140208f5a6e0d654d30eba/ui/src/starnet/ui/alpha/main.cljs
; https://github.com/sergeiudris/starnet/blob/af86204ff94776ceab140208f5a6e0d654d30eba/ui/src/starnet/ui/alpha/render.cljs

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

(def ant-icon-smile-outlined (r/adapt-react-class AntIconSmileOutlined))
(def ant-icon-loading-outlined (r/adapt-react-class AntIconLoadingOutlined))
(def ant-icon-sync-outlined (r/adapt-react-class AntIconSyncOutlined))
(def ant-icon-reload-outlined (r/adapt-react-class AntIconReloadOutlined))



#_(defn rc-main
    [{:keys [input|] :as channels} state]
    (r/with-let [data (r/cursor state [:data])
                 counter (r/cursor state [:counter])]
      (if (empty? @state)

        [:div "loading..."]

        [:<>
         [:pre {} (with-out-str (pprint @state))]
         [ant-button {:icon (r/as-element [ant-icon-sync-outlined])
                      :size "small"
                      :title "button"
                      :on-click (fn [] ::button-click)}]]

        #_[:<>
           [ant-tabs {:defaultActiveKey :connections}
            [ant-tab-pane {:tab "Connections" :key :connections}
             [rc-tab-connections channels state]]
            [ant-tab-pane {:tab "Multiplayer" :key :multiplayer}
             [:div  ::multiplayer]]
            [ant-tab-pane {:tab "State" :key :state}
             [rc-tab-state channels state]]]]
        #_[:<>
           #_[:div {} "rc-main"]
           #_[:button {:on-click (fn [e]
                                   (println "button clicked")
                                   #_(put! ops| ???))} "button"]
           #_[:div ":conf"]
           #_[:div {} (with-out-str (pprint @conf))]
           #_[:div @lrepl-id]
           #_[:div @ns-sym]
           [:br]
           [:div ":counter"]
           [:div {} (str @counter)]
           [:input {:type "button" :value "counter-inc"
                    :on-click #(swap! (ctx :state) update :counter inc)}]
           [:br]
           [:div ":data"]
           [:section
            (map-indexed (fn [i v]
                           ^{:key i} [:pre {} (with-out-str (pprint v))])
                         @data)]])))