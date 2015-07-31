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

(defonce app-state (atom {:xy{:element {}
                              :data {}}
                          :heatmap {:element {}
                                    :element-legend {}
                                    :data {}}}))

(enable-console-print!)

(om/root heatmap/chart
         (:heatmap @app-state)
         {:target (. js/document (getElementById "heatmap"))})

(om/root xy/chart
         (:xy @app-state)
         {:target (. js/document (getElementById "xy-plot"))})

;;
(defn on-js-reload [])
