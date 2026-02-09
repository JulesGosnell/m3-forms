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

(ns m3-forms.m0
  "Auto-generated document rendering via ::m0 multimethod dispatch.
   Renders M2 schema + M1 data into document hiccup with WYSIWYG-editable spans."
  (:require
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [m3-forms.json :refer [absent present? absent? option-get-in]]
   [m3-forms.schema :as json]
   [m3-forms.render :refer [render-2 render-1]]
   [m3-forms.mui :refer [key->label]]
   [m3-forms.util :refer [valid? conjv make-id]]))

;;------------------------------------------------------------------------------
;; Helpers

(defn m1-path->dot-path
  "Convert an m1 path like [:m1 \"payoff\" \"index\"] to dot-separated \"payoff.index\"."
  [p1]
  (str/join "." (rest p1)))

(defn field-span
  "Wrap a value in an m0-field span with data attributes for WYSIWYG editing."
  [p1 schema-type fmt value]
  (let [dot-path (m1-path->dot-path p1)
        absent?  (= value absent)
        null?    (and (not absent?) (nil? value))
        empty?   (= "" value)
        state    (cond absent? "absent" null? "null" empty? "empty" :else "value")
        css      (str "m0-field"
                      (when absent? " m0-field-absent")
                      (when null?   " m0-field-null")
                      (when empty?  " m0-field-empty"))]
    [:span {:class css
            :data-path dot-path
            :data-state state
            :data-schema-type (or schema-type "string")
            :data-format (or fmt "")
            :data-enums ""
            :title dot-path}
     (cond
       absent? [:span.m0-placeholder (str "[" dot-path "]")]
       null?   [:span.m0-null "null"]
       empty?  "\u200A"  ;; hair space — keeps span clickable
       :else   (str value))]))

;;------------------------------------------------------------------------------
;; ::m0 renderers — leaf types

(defmethod render-2 [:m3-forms.render/m0 "string" :default]
  [c2 p2 k2 {title "title" es "enum" :as m2}]
  (let [fmt nil]
    (fn [c1 p1 k1 m1]
      (field-span p1 "string" fmt m1))))

(defmethod render-2 [:m3-forms.render/m0 "string" "date"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "string" "date" m1)))

(defmethod render-2 [:m3-forms.render/m0 "string" "date-time"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "string" "date-time" m1)))

(defmethod render-2 [:m3-forms.render/m0 "string" "time"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "string" "time" m1)))

(defmethod render-2 [:m3-forms.render/m0 "string" "year-month"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "string" "year-month" m1)))

(defmethod render-2 [:m3-forms.render/m0 "integer" :default]
  [c2 p2 k2 {fmt "format" :as m2}]
  (fn [c1 p1 k1 m1]
    (field-span p1 "integer" (or fmt "") m1)))

(defmethod render-2 [:m3-forms.render/m0 "integer" "money"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (let [display (if (and (present? m1) (some? m1))
                    (str "\u00A3" (.toLocaleString (js/Number. m1) "en-GB"))
                    m1)]
      (field-span p1 "integer" "money" (if (present? m1) display m1)))))

(defmethod render-2 [:m3-forms.render/m0 "integer" "range"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "integer" "range" m1)))

(defmethod render-2 [:m3-forms.render/m0 "number" :default]
  [c2 p2 k2 {fmt "format" :as m2}]
  (fn [c1 p1 k1 m1]
    (field-span p1 "number" (or fmt "") m1)))

(defmethod render-2 [:m3-forms.render/m0 "number" "money"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (let [display (if (and (present? m1) (some? m1))
                    (str "\u00A3" (.toFixed (js/Number. m1) 2))
                    m1)]
      (field-span p1 "number" "money" (if (present? m1) display m1)))))

(defmethod render-2 [:m3-forms.render/m0 "number" "range"]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "number" "range" m1)))

(defmethod render-2 [:m3-forms.render/m0 "boolean" :default]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (let [display (cond (absent? m1) m1 (nil? m1) m1 m1 "Yes" :else "No")]
      (field-span p1 "boolean" "" display))))

(defmethod render-2 [:m3-forms.render/m0 "null" :default]
  [c2 p2 k2 m2]
  (fn [c1 p1 k1 m1]
    (field-span p1 "null" "" nil)))

;;------------------------------------------------------------------------------
;; ::m0 renderers — compound types

(defmethod render-2 [:m3-forms.render/m0 "object" :default]
  [c2 p2 k2 {ps "properties" title "title" :as m2}]
  (fn [c1 p1 k1 m1]
    [:section
     (when title [:h3 title])
     (when ps
       [:dl
        (doall
         (mapcat
          (fn [[k {t "title" :as prop-m2}]]
            (let [label (or t (key->label k))
                  child-p2 (vec (concat p2 ["properties" k]))
                  child-p1 (conjv p1 k)
                  child-m1 (get (when (present? m1) m1) k absent)]
              [[:dt {:key (str (make-id child-p1) "-dt")} label]
               [:dd {:key (str (make-id child-p1) "-dd")}
                ((render-1 c2 child-p2 k prop-m2) c1 child-p1 k child-m1)]]))
          ps))])]))

(defmethod render-2 [:m3-forms.render/m0 "array" :default]
  [c2 p2 k2 {title "title" {def "default" :as is} "items" pis "prefixItems" :as m2}]
  (fn [c1 p1 k1 m1]
    (let [items (when (present? m1) m1)
          item-schemas (concat pis (repeat is))]
      [:div
       (when title [:h4 title])
       (if (seq items)
         [:table.m0-table
          [:tbody
           (doall
            (map-indexed
             (fn [i item]
               (let [prefix? (< i (count pis))
                     item-m2 (nth item-schemas i)
                     child-p2 (vec (concat p2 (if prefix? ["prefixItems" i] ["items"])))
                     child-p1 (conjv p1 i)]
                 [:tr {:key (make-id child-p1)}
                  [:td ((render-1 c2 child-p2 nil item-m2) c1 child-p1 i item)]]))
             items))]]
         [:p [:em "No items"]])])))

(defmethod render-2 [:m3-forms.render/m0 "oneOf" :default]
  [c2 p2 k2 {oos "oneOf" title "title" :as m2}]
  (fn [c1 p1 k1 m1]
    (let [{valid true}
          (if (present? m1)
            (group-by
             (comp not seq second)
             (mapv (fn [oo] [oo ((json/check-schema c2 p1 oo) c1 [] m1)]) oos))
            {})
          match (ffirst valid)]
      [:div
       (when title [:h4 title])
       (if match
         ((render-1 c2 p2 k2 match) c1 p1 k1 m1)
         [:p [:em "No matching variant"]])])))

;;------------------------------------------------------------------------------
;; Document view component

(defn document-view
  "Auto-generated M0 document from M2 schema + M1 data using ::m0 renderers.
   Supports WYSIWYG editing via native DOM click handlers on .m0-field spans,
   and post-render validation via DOM class manipulation."
  [_m2 _m1 _req-paths]
  (let [edit-mode? (reagent/atom false)
        editing (reagent/atom nil)
        container-el (atom nil)
        prev-content-el (atom nil)
        handle-click (fn [e]
                       (when @edit-mode?
                         (let [target (.-target e)
                               field (if (and (.-classList target) (.contains (.-classList target) "m0-field"))
                                       target
                                       (.closest target ".m0-field"))]
                           (when field
                             (let [container @container-el
                                   rect (.getBoundingClientRect field)
                                   container-rect (.getBoundingClientRect container)
                                   path (.getAttribute field "data-path")
                                   schema-type (or (.getAttribute field "data-schema-type") "string")
                                   fmt (or (.getAttribute field "data-format") "")
                                   enums-str (.getAttribute field "data-enums")
                                   state (.getAttribute field "data-state")
                                   value (case state
                                           "absent" ""
                                           "null"   ""
                                           "empty"  ""
                                           (.-textContent field))]
                               (reset! editing
                                 {:path path
                                  :schema-type schema-type
                                  :format fmt
                                  :enums (when (seq enums-str) (str/split enums-str #","))
                                  :value value
                                  :top (- (.-top rect) (.-top container-rect))
                                  :left (- (.-left rect) (.-left container-rect))
                                  :width (max (.-width rect) 120)
                                  :height (.-height rect)}))))))
        apply-validation! (fn [el req-paths m1]
                            (when el
                              (doseq [field (array-seq (.querySelectorAll el ".m0-field"))]
                                (let [path (.getAttribute field "data-path")
                                      opt  (option-get-in m1 (str/split path #"\."))
                                      schema-type (.getAttribute field "data-schema-type")
                                      required? (contains? req-paths path)
                                      err (cond
                                            (and (not opt) required?) "Required"
                                            (and opt (nil? (first opt)) (not= schema-type "null")) (str "Null — expected " schema-type)
                                            :else nil)]
                                  (if err
                                    (do (.add (.-classList field) "m0-field-invalid")
                                        (.setAttribute field "title"
                                          (str (.getAttribute field "title") " — " err)))
                                    (.remove (.-classList field) "m0-field-invalid"))))))
        attach-listener! (fn [el]
                           (when-let [prev @prev-content-el]
                             (.removeEventListener prev "click" handle-click))
                           (reset! prev-content-el el)
                           (when el
                             (.addEventListener el "click" handle-click)))]
    (fn [m2 m1 req-paths]
      (let [c2 {:ui :m3-forms.render/m0 :root m2 :draft "latest" :$ref-merger :merge-over}
            renderer (render-1 c2 [:m2] nil m2)]
        [:div {:style {:position "relative"}
               :ref (fn [el] (reset! container-el el))}
         ;; Header with toggle
         [:div {:style {:display "flex" :justify-content "space-between" :align-items "center" :margin-bottom "8px"}}
          [:h4 {:style {:margin 0 :color "#666"}} "Document Preview"]
          [:button {:style {:padding "4px 12px" :border "1px solid #ccc" :border-radius "4px"
                            :background (if @edit-mode? "#1976d2" "#fff")
                            :color (if @edit-mode? "#fff" "#333")
                            :cursor "pointer" :font-size "13px"}
                    :on-click #(do (.stopPropagation %) (reset! editing nil) (swap! edit-mode? not))}
           (if @edit-mode? "Preview" "Edit")]]
         ;; Document body — rendered as hiccup with ref for validation + click handling
         [:div {:class (when @edit-mode? "m0-editable")
                :ref (fn [el]
                       (attach-listener! el)
                       (apply-validation! el req-paths m1))}
          (renderer {:root m1} [:m1] nil m1)]
         ;; Floating inline editor
         (when-let [{:keys [path schema-type format enums value top left width height]} @editing]
           (let [base-style {:position "absolute"
                             :top top :left left
                             :min-width (max width 120) :height (max height 24)
                             :font-size "inherit" :font-family "inherit"
                             :border "2px solid #1976d2" :border-radius "2px"
                             :padding "0 4px" :box-sizing "border-box"
                             :z-index 10 :background "#fff"}
                 m1-path (vec (cons :m1 (str/split path #"\.")))
                 commit! (fn [raw-val]
                           (let [parsed (case schema-type
                                          "integer" (let [n (js/parseInt raw-val 10)] (if (js/isNaN n) raw-val n))
                                          "number" (let [n (js/parseFloat raw-val)] (if (js/isNaN n) raw-val n))
                                          "boolean" (= raw-val "true")
                                          raw-val)]
                             (reset! editing nil)
                             (rf/dispatch [:assoc-in m1-path parsed])))
                 cancel! #(reset! editing nil)
                 key-handler (fn [e]
                               (when (= (.-key e) "Enter") (.blur (.-target e)))
                               (when (= (.-key e) "Escape") (cancel!)))]
             (cond
               (seq enums)
               [:select {:style (assoc base-style :width (+ width 40))
                         :auto-focus true :default-value value
                         :on-blur (fn [e] (commit! (.-value (.-target e))))
                         :on-change (fn [e] (commit! (.-value (.-target e))))}
                (doall (map (fn [v] [:option {:key v :value v} v]) enums))]

               (= format "date")
               [:input {:style base-style :type "date" :auto-focus true
                        :default-value value
                        :on-blur (fn [e] (commit! (.-value (.-target e))))
                        :on-key-down key-handler}]

               (= format "date-time")
               [:input {:style (assoc base-style :min-width (max width 200)) :type "datetime-local" :auto-focus true
                        :default-value value
                        :on-blur (fn [e] (commit! (.-value (.-target e))))
                        :on-key-down key-handler}]

               (= format "time")
               [:input {:style base-style :type "time" :auto-focus true
                        :default-value value
                        :on-blur (fn [e] (commit! (.-value (.-target e))))
                        :on-key-down key-handler}]

               (= schema-type "integer")
               [:input {:style base-style :type "number" :step 1 :auto-focus true
                        :default-value value
                        :on-blur (fn [e] (commit! (.-value (.-target e))))
                        :on-key-down key-handler}]

               (= schema-type "number")
               [:input {:style base-style :type "number" :step "any" :auto-focus true
                        :default-value value
                        :on-blur (fn [e] (commit! (.-value (.-target e))))
                        :on-key-down key-handler}]

               :else
               [:input {:style base-style :type "text" :auto-focus true
                        :default-value value
                        :on-blur (fn [e] (commit! (.-value (.-target e))))
                        :on-key-down key-handler}])))]))))
