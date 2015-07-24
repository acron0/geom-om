(ns ^:figwheel-always geom-om.core
    (:require-macros [hiccups.core :as hiccups])
    (:require[om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]
             [thi.ng.geom.viz.core :as viz]
             [thi.ng.geom.svg.core :as svg]
             [thi.ng.geom.core.vector :as v]
             [thi.ng.math.core :as m :refer [PI]]
             [hiccups.runtime :as hiccupsrt]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"
                          :spec {}}))

(defn draw-viz
  [spec]
  (->> spec
       (viz/svg-plot2d-cartesian)
       (svg/svg {:width 700 :height 600})))

(defn set-new-chart-data!
  [cursor]
  (om/update! cursor :spec {:x-axis (viz/log-axis
                                    {:domain [1 201] :range [50 590] :pos 550})
                           :y-axis (viz/linear-axis
                                    {:domain [0.1 100] :range [550 20] :major 10 :minor 5 :pos 50
                                     :label-dist 15 :label {:text-anchor "end"}})
                           :grid   {:attribs {:stroke "#caa"}
                                    :minor-x true
                                    :minor-y true}
                           :data   [{:values  (map (juxt identity #(Math/sqrt %)) (range 0 200 2))
                                     :attribs {:fill "#0af" :stroke "none"}
                                     :layout  viz/svg-scatter-plot}
                                    {:values  (map (juxt identity #(m/random %)) (range 0 200 2))
                                     :attribs {:fill "none" :stroke "#f60"}
                                     :shape   (viz/svg-triangle-down 6)
                                     :layout  viz/svg-scatter-plot}]}))

(om/root
 (fn [data owner]
   (reify
     om/IWillMount
     (will-mount [_]
       (set-new-chart-data! data))
     om/IRender
     (render [_]
       (dom/div nil
                (dom/button #js {:onClick #(set-new-chart-data! data)} "Generate")
                (dom/div #js {:dangerouslySetInnerHTML
                              #js {:__html (hiccups/html (draw-viz (:spec @data)))}} nil)))))
  app-state
  {:target (. js/document (getElementById "app"))})
