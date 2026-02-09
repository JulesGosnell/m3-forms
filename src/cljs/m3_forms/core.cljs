;; Copyright 2025 Julian Gosnell
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns m3-forms.core
  (:require
   [cljs.pprint :as ppt]
   [cljs.core :as cljs]
   ["handlebars$default" :as handlebars]
   ["marked" :refer [marked]]
   [reagent.core :as reagent]
   [reagent.dom.client :as rdc]
   [re-frame.core :as rf]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [m3-forms.log :as log]

   [m3-forms.util     :refer [index-by-$id check-formats]]
   [m3-forms.schema   :refer [make-m3]]
   [m3-forms.json     :refer [json-insert-in json-remove-in]]
   [m3-forms.migrate  :refer [migrate]]
   [m3-forms.render   :refer [render-1]]
   [m3-forms.mui      :refer [my-app-bar view-forms]]
   [m3-forms.page     :refer [->page]]

   [m3-forms.demo     :refer [demo-m2 demo-m1]]
   [m3-forms.divorce  :refer [divorce-workflow-m1 divorce-m2 divorce-m1]]
   [m3-forms.final-terms  :refer [final-terms-workflow-m1 final-terms-m2 final-terms-m1 final-terms-m0]]
   ))

;;------------------------------------------------------------------------------

(def m3 (make-m3 {:draft "latest"}))

(def products
  (array-map
   "Final Terms" {:m2 final-terms-m2 :m1 final-terms-m1 :m0 final-terms-m0
                  :workflow final-terms-workflow-m1}
   "Divorce" {:m2 divorce-m2 :m1 divorce-m1 :m0 ""
              :workflow divorce-workflow-m1}
   "Demo" {:m2 demo-m2 :m1 demo-m1 :m0 ""}))

(rf/reg-event-db
 :initialise
 (fn-traced [_ [_ db]]
            (log/info "INITIALISE")
            db))

(rf/reg-event-db
 :assoc-in
 (fn-traced [db [_ path v]]
            (assoc-in db path v)))

(rf/reg-event-db
 :update-in
 (fn-traced [db [_ path f & args]]
            (when (not (= path [:zip]))(println "UPDATE-IN:" path ":" f args))
            (apply update-in db path f args)))

;;------------------------------------------------------------------------------

(defn do-rename-in-tidy [db [_]]
  (dissoc db :original-key :current-key))

(rf/reg-event-db :rename-in-tidy (fn-traced [& args] (apply do-rename-in-tidy args)))

(defn do-rename-in [{ok :original-key ck :current-key m2 :m2 m1 :m1 z :zip :as db} [_ path old-k new-k]]
  (let [old-k (or ck old-k)
        migration
        {"type" "rename"
         "m2-path" (vec (rest path))
         "m1-path" (vec (rest (butlast (z (conj path old-k)))))
         "source" old-k
         "target" new-k}
        m2-ctx {:draft "latest" :$ref-merger :merge-over}
        m1-ctx {:draft "latest" :$ref-merger :merge-over}
        [m2 [m1]] (migrate m1-ctx m2-ctx migration [m2 [m1]])]
    (assoc
     db
     :m2 m2
     :m1 m1
     :current-key new-k
     :original-key {[path new-k] (or (get ok [path old-k]) old-k)})))

(rf/reg-event-db :rename-in (fn-traced [& args] (apply do-rename-in args)))

;;------------------------------------------------------------------------------

(defn do-delete-in [{m2 :m2 m1 :m1 z :zip :as db} [_ path]]
  (let [migration
        {"type" "delete"
         "m2-path" (vec (rest path))
         "m1-path" (vec (rest (z path)))}
        m2-ctx {:draft "latest" :$ref-merger :merge-over}
        m1-ctx {:draft "latest" :$ref-merger :merge-over}
        [m2 [m1]] (migrate m2-ctx m1-ctx migration [m2 [m1]])]
    (assoc db :m2 m2 :m1 m1)))

(rf/reg-event-db :delete-in (fn-traced [& args] (apply do-delete-in args)))

;;------------------------------------------------------------------------------

(rf/reg-event-db
 :move
 (fn-traced [db [_ src tgt]]
            (println "MOVE:" (type src) src "->" tgt)
            (let [v (get-in db src)]
              (println "V:" v)
              (-> db
                  (json-remove-in src)
                  (json-insert-in (butlast tgt) (last tgt) [(last src) v])))))

;;------------------------------------------------------------------------------

(rf/reg-event-db
 :expand
 (fn-traced [db [_ path]]
            (update-in db [:expanded] conj path)))

(rf/reg-event-db
 :collapse
 (fn-traced [db [_ path]]
            (update-in db [:expanded] disj path)))

;;------------------------------------------------------------------------------

(rf/reg-event-db
 :select-product
 (fn-traced [db [_ product-id]]
            (let [{:keys [m2 m1 m0 workflow]} (get (:products db) product-id)
                  first-state (get-in workflow ["states" 0 "$id"])]
              (assoc db
                     :product-id product-id
                     :m2 m2 :m1 m1 :m0 (or m0 "")
                     :workflow workflow
                     :state-id first-state
                     :active-tab 0))))

(rf/reg-event-db
 :transition
 (fn-traced [db [_ state-id]]
            (assoc db :state-id state-id :active-tab 0)))

(rf/reg-event-db
 :set-active-tab
 (fn-traced [db [_ i]]
            (assoc db :active-tab i)))

;;------------------------------------------------------------------------------

(rf/reg-sub :m3 (fn [db _] (:m3 db)))
(rf/reg-sub :m2 (fn [db _] (:m2 db)))
(rf/reg-sub :m1 (fn [db _] (:m1 db)))
(rf/reg-sub :m0 (fn [db _] (:m0 db)))
(rf/reg-sub :expanded (fn [db _] (:expanded db)))
(rf/reg-sub :original-key (fn [db _] (:original-key db)))
(rf/reg-sub :products (fn [db _] (:products db)))
(rf/reg-sub :product-id (fn [db _] (:product-id db)))
(rf/reg-sub :workflow (fn [db _] (:workflow db)))
(rf/reg-sub :state-id (fn [db _] (:state-id db)))
(rf/reg-sub :active-tab (fn [db _] (or (:active-tab db) 0)))

(defn pretty [json]
  (let [sb (goog.string/StringBuffer.)]
    (binding [*out* (StringBufferWriter. sb)
              ppt/*print-right-margin* 30]
      (ppt/pprint json *out*))
    (str sb)))

;;------------------------------------------------------------------------------

(def workflow-m2
  {"$id" "Workflow"
   "$schema" "M3"
   "type" "object"
   "$defs"
   {"State"
    {"type" "object"
     "properties"
     {"$id" {"type" "string"}
      "title" {"type" "string"}}}}
   "properties"
    {"states"
     {"type" "array"
      "items" {"$ref" "#/$defs/State"}}}})

(def workflow-m1
  (array-map
   "states" []))

;;------------------------------------------------------------------------------

(defn clj->json [clj]
  (when clj (.stringify js/JSON (clj->js clj) nil 2)))

(defn json->clj [json]
  (when (not (empty? json)) (js->clj (.parse js/JSON json))))

;;------------------------------------------------------------------------------
;; handlebars helpers

(defn switch-helper [value options]
  (this-as
   this
   (try
     (set! ^js/string (.-_switch_value_ this) value)
     (set! ^boolean (.-_switch_break_ this) false)
     ((.-fn options) this)
     (finally
       (js-delete this "_switch_value_")
       (js-delete this "_switch_break_")))))

(defn case-helper [value options]
  (this-as
   this
   (if (and
        (= ^js/string (.-_switch_value_ this) value)
        (not ^boolean (.-_switch_break_ this)))
     (do
       (set! ^boolean (.-_switch_break_ this) true)
       ((.-fn options) this))
     "")))

(.registerHelper handlebars "switch" switch-helper)
(.registerHelper handlebars "case" case-helper)

(defn eq-helper [arg1 arg2 options]
  (if (= arg1 arg2)
    (.-fn options)
    (.-inverse options)))

(.registerHelper handlebars "eq" eq-helper)

;; Configure marked to suppress deprecation warnings
(.use marked (clj->js {:mangle false :headerIds false}))

;;------------------------------------------------------------------------------

(defn html-string [html]
  [:div {:dangerouslySetInnerHTML {:__html html}}])

;;------------------------------------------------------------------------------

(defn home-page []
  (let [m3s (rf/subscribe [:m3])
        m2s (rf/subscribe [:m2])
        m1s (rf/subscribe [:m1])
        m0s (rf/subscribe [:m0])
        expanded (rf/subscribe [:expanded])
        original-key (rf/subscribe [:original-key])
        products-sub (rf/subscribe [:products])
        product-id-sub (rf/subscribe [:product-id])
        workflow-sub (rf/subscribe [:workflow])
        state-id-sub (rf/subscribe [:state-id])
        active-tab-sub (rf/subscribe [:active-tab])]
    [:div
     ;; App bar
     [:header [my-app-bar]]
     ;; Product selector
     [:div {:style {:padding "8px" :text-align "center" :background "#f5f5f5"}}
      [:label {:style {:margin-right "8px" :font-weight "bold"}} "Product: "]
      [:select {:value (or @product-id-sub "")
                :style {:font-size "16px" :padding "4px"}
                :on-change (fn [e] (rf/dispatch [:select-product (.-value (.-target e))]))}
       (doall
        (map (fn [id] [:option {:key id :value id} id])
             (keys @products-sub)))]]
     ;; MUI customer pane
     (when @workflow-sub
       [:div {:style {:padding "16px" :background "#fafafa" :border-bottom "2px solid #ddd"
                      :max-width "960px" :margin "0 auto"}}
        [view-forms {:m2 @m2s :m1 @m1s :workflow @workflow-sub
                     :state-id @state-id-sub :active-tab @active-tab-sub
                     :expanded (or @expanded #{})}]])
     ;; Developer pane
     [:details {:open true}
      [:summary {:style {:padding "8px" :cursor "pointer" :background "#e0e0e0" :font-weight "bold"}} "Developer View"]
      [:main
       [:table {:align "center"}
        [:tbody
         [:tr
          [:td
           [:table
            [:caption]
            [:thead
             [:tr
              [:th "M3 Editor"]
              [:th "M3 JSON"]
              [:th "M2 Editor"]
              [:th "M2 JSON"]
              [:th "M1 Editor"]
              [:th "M1 JSON"]
              [:th "M0 Template"]
              [:th "M0 Document"]]]
            [:tbody
             [:tr
              [:td {:valign :top} [:table [:tbody [:tr [:td ((render-1 {:draft "latest" :root @m3s :$ref-merger :merge-over :expanded @expanded :original-key @original-key} [:m3] nil @m3s) {:draft "latest" :root @m3s :$ref-merger :merge-over} [:m3] nil @m3s)]]]]]
              [:td {:valign :top} [:textarea {:rows 180 :cols 50 :read-only true :value (pretty @m3s)}]]
              [:td {:valign :top} [:table [:tbody [:tr [:td ((render-1 {:draft "latest" :root @m3s :$ref-merger :merge-over :expanded @expanded :original-key @original-key} [:m3] nil @m3s) {:draft "latest" :root @m2s :$ref-merger :merge-over} [:m2] nil @m2s)]]]]]
              [:td {:valign :top} [:textarea {:rows 180 :cols 50 :read-only false :value (clj->json @m2s) :on-change (fn [event] (rf/dispatch [:assoc-in [:m2] (json->clj (.-value (.-target event)))]))}]]
              [:td {:valign :top} [:table [:tbody [:tr [:td ((render-1 {:draft "latest" :root @m2s :$ref-merger :merge-over :check-format check-formats :expanded @expanded} [:m2] nil @m2s) {:draft "latest" :root @m1s :$ref-merger :merge-over} [:m1] nil @m1s)]]]]]
              [:td {:valign :top} [:textarea {:rows 180 :cols 50 :read-only false :value (clj->json @m1s) :on-change (fn [event] (rf/dispatch [:assoc-in [:m1] (json->clj (.-value (.-target event)))]))}]]
              [:td {:valign :top} [:textarea {:rows 180 :cols 50 :read-only false :value @m0s :on-change (fn [event] (rf/dispatch [:assoc-in [:m0] (.-value (.-target event))]))}]]
              [:td {:valign :top} [html-string (let [m1 @m1s m0 @m0s] (when (and m1 m0) (.parse marked ((.compile handlebars m0) (clj->js m1)))))]]]]]]]]]]]]))

;; -------------------------
;; Initialize app

(defonce root (rdc/create-root (.getElementById js/document "app")))

(defn mount-root []
  (rdc/render root [home-page]))

(defn ^:export init! []
  (let [default-id "Final Terms"
        {:keys [m2 m1 m0 workflow]} (products default-id)]
    (rf/dispatch [:initialise
                  {:m3 m3
                   :products products
                   :product-id default-id
                   :m2 m2 :m1 m1 :m0 (or m0 "")
                   :workflow workflow
                   :state-id (get-in workflow ["states" 0 "$id"])
                   :active-tab 0
                   :expanded #{}}])
    (mount-root)))

;; shadow-cljs auto-reload api
(defn ^:dev/after-load re-render []
  (mount-root))
