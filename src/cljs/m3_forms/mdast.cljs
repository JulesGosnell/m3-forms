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

(ns m3-forms.mdast
  "Renders mdast (Markdown AST) document trees embedded as $document in M2 schemas.
   Produces Reagent hiccup with reactive field references into M1 data.
   Supports WYSIWYG inline editing via standard React events (no dangerouslySetInnerHTML)."
  (:require
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [m3-forms.json :refer [option-get-in]]))

;;------------------------------------------------------------------------------
;; Path helpers

(defn coerce-path-segment
  "Coerce a string path segment to an integer if it looks numeric (for vector indexing)."
  [s]
  (if (re-matches #"\d+" s)
    (js/parseInt s 10)
    s))

(defn coerce-path
  "Coerce a mixed path — numeric strings become integers for vector access."
  [path]
  (mapv coerce-path-segment path))

;;------------------------------------------------------------------------------
;; Field value display helpers

(defn format-value
  "Format an M1 value for display based on schema type and format."
  [value schema-type fmt]
  (cond
    (nil? value) nil

    (and (= fmt "money") (= schema-type "integer") (some? value))
    (str "\u00A3" (.toLocaleString (js/Number. value) "en-GB"))

    (and (= fmt "money") (= schema-type "number") (some? value))
    (str "\u00A3" (.toFixed (js/Number. value) 2))

    (and (= schema-type "boolean"))
    (if value "Yes" "No")

    ;; Arrays displayed as comma-separated
    (sequential? value)
    (str/join ", " value)

    :else (str value)))

;;------------------------------------------------------------------------------
;; Field reference component — reactive leaf node

(defn field-reference
  "Renders a single field reference as a reactive span.
   Subscribes to M1 data at the resolved path."
  [{:keys [path description schema-type fmt enums base-path required-paths]}]
  (let [;; Resolve the actual M1 path
        path-parts (str/split path #"\.")
        full-path (into (vec base-path) path-parts)
        dot-path (str/join "." full-path)
        required? (contains? required-paths dot-path)]
    (fn [{:keys [path description schema-type fmt enums base-path required-paths]}]
      (let [path-parts (str/split path #"\.")
            full-path (into (vec base-path) path-parts)
            dot-path (str/join "." full-path)
            m1 @(rf/subscribe [:m1])
            opt (option-get-in m1 (coerce-path full-path))
            absent? (nil? opt)
            value (when opt (first opt))
            null? (and (not absent?) (nil? value))
            empty? (= "" value)
            state (cond absent? "absent" null? "null" empty? "empty" :else "value")
            display (cond
                      absent? nil
                      null? nil
                      empty? nil
                      :else (format-value value (or schema-type "string") (or fmt "")))
            css (str "m0-field"
                     (when absent? " m0-field-absent")
                     (when null? " m0-field-null")
                     (when empty? " m0-field-empty")
                     (when (and required? (or absent? null?)) " m0-field-invalid"))]
        [:span {:class css
                :data-path dot-path
                :data-state state
                :data-schema-type (or schema-type "string")
                :data-format (or fmt "")
                :data-enums (if (seq enums) (str/join "," enums) "")
                :title (or description dot-path)}
         (case state
           "absent" [:span.m0-placeholder (str "[" (or description dot-path) "]")]
           "null"   [:span.m0-null "null"]
           "empty"  "\u200A"
           display)]))))

;;------------------------------------------------------------------------------
;; mdast tree walker

(declare render-node render-children)

(defn render-children
  "Render an array of mdast child nodes."
  [children ctx]
  (doall
   (map-indexed
    (fn [i child]
      ^{:key i} [render-node child ctx])
    children)))

(defn render-node
  "Render a single mdast node as Reagent hiccup."
  [{node-type "type" :as node} ctx]
  (case node-type
    "root"
    [:div.m0-document (render-children (get node "children") ctx)]

    "heading"
    (let [depth (get node "depth" 2)
          tag (keyword (str "h" (min depth 6)))]
      [tag (render-children (get node "children") ctx)])

    "paragraph"
    [:p (render-children (get node "children") ctx)]

    "text"
    (get node "value" "")

    "emphasis"
    [:em (render-children (get node "children") ctx)]

    "strong"
    [:strong (render-children (get node "children") ctx)]

    "link"
    [:a {:href (get node "url")} (render-children (get node "children") ctx)]

    "image"
    [:img {:src (get node "url") :alt (get node "alt" "")}]

    "code"
    [:pre [:code (get node "value" "")]]

    "inlineCode"
    [:code (get node "value" "")]

    "blockquote"
    [:blockquote (render-children (get node "children") ctx)]

    "list"
    (let [ordered? (get node "ordered" false)
          tag (if ordered? :ol :ul)]
      [tag (render-children (get node "children") ctx)])

    "listItem"
    [:li (render-children (get node "children") ctx)]

    "table"
    [:table.m0-table [:tbody (render-children (get node "children") ctx)]]

    "tableRow"
    [:tr (render-children (get node "children") ctx)]

    "tableCell"
    (let [header? (get node "header" false)
          tag (if header? :th :td)]
      [tag (render-children (get node "children") ctx)])

    "thematicBreak"
    [:hr]

    "break"
    [:br]

    "html"
    nil  ;; skip raw HTML

    ;; Custom document nodes

    "fieldReference"
    [field-reference {:path (get node "path")
                      :description (get node "description")
                      :schema-type (get node "schemaType" "string")
                      :fmt (get node "format")
                      :enums (get node "enums")
                      :base-path (:base-path ctx)
                      :required-paths (:required-paths ctx)}]

    "each"
    (let [array-path (get node "path")
          path-parts (str/split array-path #"\.")
          full-path (into (vec (:base-path ctx)) path-parts)
          m1 @(rf/subscribe [:m1])
          items (get-in m1 (coerce-path full-path))]
      [:<>
       (when (sequential? items)
         (doall
          (map-indexed
           (fn [i _item]
             (let [item-base (conj (vec full-path) (str i))
                   child-ctx (assoc ctx :base-path item-base)]
               ^{:key i}
               [:<> (render-children (get node "children") child-ctx)]))
           items)))])

    "conditional"
    (let [cond-path (get node "path")
          path-parts (str/split cond-path #"\.")
          full-path (into (vec (:base-path ctx)) path-parts)
          m1 @(rf/subscribe [:m1])
          opt (option-get-in m1 (coerce-path full-path))
          value (when opt (first opt))
          cases (get node "cases")
          matching-children (get cases (str value))]
      (when matching-children
        [:<> (render-children matching-children ctx)]))

    ;; Default — unknown node type, skip
    nil))

;;------------------------------------------------------------------------------
;; WYSIWYG editing — floating editor component

(defn floating-editor
  "Floating inline editor for a field. Positioned over the clicked field."
  [{:keys [path schema-type fmt enums value top left width height on-close]}]
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
                    (on-close)
                    (rf/dispatch [:assoc-in m1-path parsed])))
        cancel! on-close
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

      (= fmt "date")
      [:input {:style base-style :type "date" :auto-focus true
               :default-value value
               :on-blur (fn [e] (commit! (.-value (.-target e))))
               :on-key-down key-handler}]

      (= fmt "date-time")
      [:input {:style (assoc base-style :min-width (max width 200)) :type "datetime-local" :auto-focus true
               :default-value value
               :on-blur (fn [e] (commit! (.-value (.-target e))))
               :on-key-down key-handler}]

      (= fmt "time")
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
               :on-key-down key-handler}])))

;;------------------------------------------------------------------------------
;; Top-level document component

(defn mdast-document
  "Top-level component for rendering an mdast $document tree.
   Supports WYSIWYG editing via React synthetic events on .m0-field spans."
  [_doc _required-paths]
  (let [edit-mode? (reagent/atom false)
        editing (reagent/atom nil)
        container-el (atom nil)]
    (fn [doc required-paths]
      (let [ctx {:base-path []
                 :required-paths (or required-paths #{})}
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
                                      :height (.-height rect)}))))))]
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
         ;; Document body — standard React events (no dangerouslySetInnerHTML!)
         [:div {:class (when @edit-mode? "m0-editable")
                :on-click handle-click}
          [render-node doc ctx]]
         ;; Floating inline editor
         (when-let [{:keys [path schema-type format enums value top left width height]} @editing]
           [floating-editor {:path path :schema-type schema-type :fmt format :enums enums
                             :value value :top top :left left :width width :height height
                             :on-close #(reset! editing nil)}])]))))

;;------------------------------------------------------------------------------
;; Utility: collect all field paths from an mdast tree

(defn collect-field-paths
  "Walk an mdast tree and return a set of all fieldReference paths (dot-separated)."
  ([doc] (collect-field-paths doc []))
  ([node base-path]
   (case (get node "type")
     "fieldReference"
     (let [path-parts (str/split (get node "path") #"\.")
           full-path (into (vec base-path) path-parts)]
       #{(str/join "." full-path)})

     "each"
     (let [array-path (get node "path")
           path-parts (str/split array-path #"\.")
           full-path (into (vec base-path) path-parts)
           ;; For each blocks, collect with a wildcard index
           child-base (conj (vec full-path) "*")]
       (reduce (fn [acc child] (into acc (collect-field-paths child child-base)))
               #{} (get node "children" [])))

     "conditional"
     (let [cases (get node "cases")]
       (reduce (fn [acc [_ children]]
                 (reduce (fn [a child] (into a (collect-field-paths child base-path)))
                         acc children))
               #{} cases))

     ;; Default: recurse into children
     (reduce (fn [acc child] (into acc (collect-field-paths child base-path)))
             #{} (get node "children" [])))))
