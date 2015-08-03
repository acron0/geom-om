(ns ^:figwheel-always geom-om.core
    (:require-macros [hiccups.core :as hiccups]
                     [cljs.core.async.macros :refer [go]])
    (:require  [cljs.core.async :refer [<! >! chan close! sliding-buffer put! alts!]]
               [cljs.reader :refer [read-string]]
               [om.core :as om :include-macros true]
               [om.dom :as dom :include-macros true]
               [thi.ng.geom.viz.core :as viz]
               [thi.ng.geom.svg.core :as svg]
               [thi.ng.geom.core.vector :as v]
               [thi.ng.geom.core :as g]
               [thi.ng.geom.core.utils :as gu]
               [thi.ng.math.simplexnoise :as n]
               [thi.ng.math.core :as m :refer [PI]]
               [thi.ng.color.gradients :as grad]
               [hiccups.runtime :as hiccupsrt]
               [goog.string :as gstring]
               [goog.string.format]
               [cljs-http.client :as http]
               [geom-om.xy :as xy]
               [geom-om.heatmap :as heatmap]))

(defonce xy-data-chan (chan))
(defonce heatmap-data-chan (chan))
(defonce app-state (atom {:xy {} :heatmap {}}))

(enable-console-print!)

;;;;;;;;;;
;; Heatmap

(go (let [resp (<! (http/get "/data/heatmap2.edn"))
          new-data (->> resp :body :data (map :value))]
      (put! heatmap-data-chan new-data)))

(om/root (heatmap/chart
          {:width 800
           :height 600
           :data-chan heatmap-data-chan})
         app-state
         {:target (. js/document (getElementById "heatmap"))
          :path [:heatmap]})

;;;;;;;;;;
;; XY plot

(go (let [resp (<! (http/get "/data/xyplot.edn"))
          new-data (:data (:body resp))]
      (put! xy-data-chan new-data)))

(om/root (xy/chart
          {:width 800
           :height 600
           :x-range [0 200]
           :y-range [0 200]
           :data-chan xy-data-chan})
         app-state
         {:target (. js/document (getElementById "xy-plot"))
          :path [:xy]})

;;
(defn on-js-reload [])
