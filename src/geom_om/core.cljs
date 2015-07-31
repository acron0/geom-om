(ns ^:figwheel-always geom-om.core
    (:require-macros [hiccups.core :as hiccups]
                     [cljs.core.async.macros :refer [go]])
    (:require [cljs.core.async :refer [<! >! chan close! sliding-buffer put! alts!]]
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

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:heatmap {}
                          :heatmap-mini {}
                          :heatmap-data []}))
(def x-readings 14)
(def y-readings 48)
(def chart-width 800)
(def chart-height 600)
(def default-gradations 100)
(def chart-color-cosine [[0.5 0.5 0] [0.5 0.5 0] [0.1 0.5 0] [0.0 0.0 0]])

(defn linear-scale
  [domain range]
  (fn [x]
    (m/map-interval x domain range)))

(defn heatmap-spec
  [id heatmap-data size-x size-y lcb ucb grads]
  (let [matrix (viz/matrix-2d size-x size-y heatmap-data)]
    {:matrix        matrix
     :value-domain  [lcb ucb]
     :palette       (apply grad/cosine-gradient grads chart-color-cosine)
     :palette-scale linear-scale
     :layout        viz/svg-heatmap
     }))

(defn int-to-dow
  [num]
  (nth ["S" "M" "T" "W" "T" "F" "S"] (mod num 7)))

(defn int-to-tod
  [num]
  (let [hrs (mod (.floor js/Math (/ num 2)) 24)
        mins (if (zero? (mod num 2)) 0 30)]
    (gstring/format "%02d:%02d" hrs mins)))

(defn set-new-heatmap-data!
  [cursor data lcb ucb gradations]
  (let [lcb (if (nil? lcb) (.floor js/Math (apply min data)) lcb)
        ucb (if (nil? ucb) (.ceil js/Math (apply max data)) ucb)
        gradations (if (nil? gradations) default-gradations gradations)]
    (om/update! cursor :heatmap {:x-axis (viz/linear-axis
                                          {:domain [0 x-readings]
                                           :range [55 (+ chart-width 5)]
                                           :major 1
                                           :pos 30
                                           :label-dist -10
                                           :major-size -5
                                           :format #(int-to-dow (int %))
                                           :label {:text-anchor "right"}})
                                 :y-axis (viz/linear-axis
                                          {:domain [0 y-readings]
                                           :range [(- chart-height 10) 35]
                                           :major 1
                                           :pos 50
                                           :label-dist 15
                                           :format #(int-to-tod (- y-readings %))
                                           :label {:text-anchor "end"}})
                                 :data     [(merge (heatmap-spec
                                                    :yellow-magenta-cyan
                                                    data
                                                    x-readings
                                                    y-readings
                                                    lcb
                                                    ucb
                                                    gradations) nil)]})

    (om/update! cursor :heatmap-mini {:x-axis (viz/linear-axis
                                               {:domain [0 20]
                                                :range [20 400]
                                                :visible false})
                                      :y-axis (viz/linear-axis
                                               {:domain [0 gradations]
                                                :range [(- chart-height 10) 35]
                                                :major 1
                                                :major-size 0
                                                :pos 10
                                                :label-dist -35
                                                :label {:text-anchor "start"}
                                                :format #(if (= % 0) lcb (if (= % gradations) ucb))})
                                      :data     [(merge (heatmap-spec
                                                         :yellow-magenta-cyan
                                                         (vec (for [x (map #(/ % gradations)(range 0 gradations))]
                                                                (m/mix lcb ucb x)))
                                                         1
                                                         gradations
                                                         lcb
                                                         ucb
                                                         gradations) nil)]})))

(defn update-chart-settings
  [owner cursor]
   (set-new-heatmap-data!
     cursor
     (:heatmap-data cursor)
     (read-string (.-value (om/get-node owner "lcb-input")))
     (read-string (.-value (om/get-node owner "ucb-input")))
     (read-string (.-value (om/get-node owner "grads-input")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn heatmap-view
  [cursor owner]
  (om/component
    (dom/div nil
             (dom/div #js {:style #js {:float "left"}
                           :dangerouslySetInnerHTML #js
                            {:__html (->> (:heatmap cursor)
                                  (viz/svg-plot2d-cartesian)
                                  (svg/svg {:width chart-width :height chart-height})
                                  (hiccups/html))}})
              (dom/div #js {:dangerouslySetInnerHTML #js
                            {:__html (->> (:heatmap-mini cursor)
                                             (viz/svg-plot2d-cartesian)
                                             (svg/svg {:width chart-width :height chart-height})
                                             (hiccups/html))}})
      (dom/div nil
               (dom/div nil
                        (dom/span nil "Lower colour bound")
                        (dom/input #js {:ref "lcb-input" :placeholder (apply min (:heatmap-data cursor))})
                        (dom/span nil "Upper colour bound")
                        (dom/input #js {:ref "ucb-input" :placeholder (apply max (:heatmap-data cursor))})
                        (dom/span nil "Gradations")
                        (dom/input #js {:ref "grads-input" :placeholder default-gradations})
                        (dom/button #js {:onClick
                                         #(update-chart-settings owner cursor)} "Refresh")))
               )))

(defn test-view [data owner foo]
  (reify
    om/IRender
    (render [this]
      (dom/div nil (str foo)))))

(om/root
 (fn [data owner]
   (reify
     om/IWillMount
     (will-mount [_]
       (go (let [resp (<! (http/get "/data/heatmap.edn"))
                 new-data (:data (:body resp))]
             (om/update! data :heatmap-data new-data)
             (set-new-heatmap-data! data new-data nil nil nil))))
     om/IRender
     (render [_]
       (dom/div nil (om/build heatmap-view data)))))
 app-state {:target (. js/document (getElementById "app"))})

;;
(defn on-js-reload [])
