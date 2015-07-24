(ns ^:figwheel-always geom-om.core
    (:require-macros [hiccups.core :as hiccups])
    (:require[om.core :as om :include-macros true]
             [om.dom :as dom :include-macros true]
             [thi.ng.geom.viz.core :as viz]
             [thi.ng.geom.svg.core :as svg]
             [thi.ng.geom.core.vector :as v]
             [thi.ng.geom.core :as g]
             [thi.ng.geom.core.utils :as gu]
             [thi.ng.math.simplexnoise :as n]
             [thi.ng.math.core :as m :refer [PI]]
             [thi.ng.color.gradients :as grad]
             [hiccups.runtime :as hiccupsrt]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:xy-plot {}
                          :heatmap {}}))

(defn set-new-xy-plot-data!
  [cursor]
  (om/update! cursor :xy-plot {:x-axis (viz/log-axis
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

(defn test-matrix
  []
  (let [start (int (m/random 100))]
    (->> (for [y (range start (+ start 10)) x (range start (+ start 50))] (n/noise2 (* x 0.1) (* y 0.25)))
         (viz/matrix-2d 50 10))))

(defn heatmap-spec
  [id]
  (let [new-test-matrix (test-matrix)]
    {:matrix        new-test-matrix
     :value-domain  (viz/value-domain-bounds new-test-matrix)
     :palette       (->> id (grad/cosine-schemes) (apply grad/cosine-gradient 100))
     :palette-scale viz/linear-scale
     :layout        viz/svg-heatmap}))

(defn set-new-heatmap-data!
  [cursor]
  (om/update! cursor :heatmap {:x-axis (viz/linear-axis
                                        {:domain [0 50] :range [50 550] :major 10 :minor 5 :pos 280})
                               :y-axis (viz/linear-axis
                                        {:domain [0 10] :range [280 20] :major 1 :pos 50
                                         :label-dist 15 :label {:text-anchor "end"}})
                               :data   [(merge (heatmap-spec :rainbow2) nil)]} ))

(defn svg-chart
  [data graph-key & {:keys [width height] :or {width 600 height 600}}]
  (dom/div #js {:dangerouslySetInnerHTML
                #js {:__html (->> (graph-key @data)
                                    (viz/svg-plot2d-cartesian)
                                    (svg/svg {:width width :height height})
                                    (hiccups/html))}}))

(om/root
 (fn [data owner]
   (reify
     om/IWillMount
     (will-mount [_]
       (set-new-xy-plot-data! data)
       (set-new-heatmap-data! data))
     om/IRender
     (render [_]
       (dom/div nil
                (dom/button #js {:onClick #(set-new-xy-plot-data! data)} "Generate")
                (svg-chart data :xy-plot)
                (dom/button #js {:onClick #(set-new-heatmap-data! data)} "Generate")
                (svg-chart data :heatmap)))))
  app-state
  {:target (. js/document (getElementById "app"))})

;;
(defn on-js-reload [])
