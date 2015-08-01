(ns geom-om.xy
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
               [cljs-http.client :as http]))

(defn chart2
  [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_])
    om/IRender
    (render [_]
      (dom/div nil "FOOBAdwdwwwdwdR"))))

(def chart-width 800)
(def chart-height 600)

(defn set-new-xy-plot-data!
  [cursor data]
  (om/update! cursor :element {:x-axis (viz/linear-axis
                                        {:domain [0 200]
                                         :range [50 (- chart-width 10)]
                                         :pos 550
                                         :major 20
                                         :minor 10})
                               :y-axis (viz/linear-axis
                                        {:domain [0 200]
                                         :range [550 20]
                                         :major 10
                                         :minor 5
                                         :pos 50
                                         :label-dist 15 :label {:text-anchor "end"}})
                               :grid   {:attribs {:stroke "#caa"}
                                        :minor-x true
                                        :minor-y true}
                               :data   [{:values  (:values data)
                                         :attribs {:fill "#06f" :stroke "#06f"}
                                         :shape   (viz/svg-square 2)
                                         :layout  viz/svg-scatter-plot}
                                        {:values (:line data)
                                         :attribs {:fill "none" :stroke "#f23"}
                                         :layout viz/svg-line-plot}]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-data!
  [cursor]
  (go (let [resp (<! (http/get "/data/xyplot.edn"))
            new-data (:data (:body resp))]
        (om/update! cursor :data new-data)
        (set-new-xy-plot-data! cursor new-data))))

(defn chart
  [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (load-data! cursor))
    om/IRender
    (render [_]
      (dom/div #js {:dangerouslySetInnerHTML #js
                      {:__html (->> @cursor
                                    :element
                                    viz/svg-plot2d-cartesian
                                    (svg/svg {:width chart-width :height chart-height})
                                    hiccups/html)}}))))
