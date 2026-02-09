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

(ns m3-forms.mui
  (:require
   [clojure.set :refer [rename-keys]]
   [clojure.string :as str]
   [react :as react]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [react-imask :refer [IMaskMixin]]
   ["@mui/material/Paper" :refer [Paper]]
   [reagent-mui.icons.account-circle     :refer [account-circle]]
   [reagent-mui.icons.add-box            :refer [add-box]]
   [reagent-mui.icons.delete-forever     :refer [delete-forever]]
   [reagent-mui.icons.drag-indicator     :refer [drag-indicator]]
   [reagent-mui.icons.edit               :refer [edit]]
   [reagent-mui.icons.notifications      :refer [notifications]]
   [reagent-mui.icons.work               :refer [work]]
   [reagent-mui.material.app-bar         :refer [app-bar]]
   [reagent-mui.material.badge           :refer [badge]]
   [reagent-mui.material.box             :refer [box]]
   [reagent-mui.material.breadcrumbs     :refer [breadcrumbs]]
   [reagent-mui.material.button          :refer [button]]
   [reagent-mui.material.button-group    :refer [button-group]]
   [reagent-mui.material.checkbox        :refer [checkbox]]
   [reagent-mui.material.container       :refer [container]]
   [reagent-mui.material.form-control    :refer [form-control]]
   [reagent-mui.material.grid            :refer [grid]]
   [reagent-mui.material.icon-button     :refer [icon-button]]
   [reagent-mui.material.input           :refer [input]]
   [reagent-mui.material.input-label     :refer [input-label]]
   [reagent-mui.material.link            :refer [link]]
   [reagent-mui.material.menu            :refer [menu]]
   [reagent-mui.material.menu-item       :refer [menu-item]]
   [reagent-mui.material.paper           :refer [paper]]
   [reagent-mui.material.stack           :refer [stack]]
   [reagent-mui.material.step            :refer [step]]
   [reagent-mui.material.step-label      :refer [step-label]]
   [reagent-mui.material.stepper         :refer [stepper]]
   [reagent-mui.material.tab             :refer [tab]]
   [reagent-mui.material.table           :refer [table]]
   [reagent-mui.material.table-body      :refer [table-body]]
   [reagent-mui.material.table-cell      :refer [table-cell]]
   [reagent-mui.material.table-container :refer [table-container]]
   [reagent-mui.material.table-footer    :refer [table-footer]]
   [reagent-mui.material.table-head      :refer [table-head]]
   [reagent-mui.material.table-row       :refer [table-row]]
   [reagent-mui.material.tabs            :refer [tabs]]
   [reagent-mui.material.slider           :refer [slider]]
   [reagent-mui.material.text-field      :refer [text-field]]
   [reagent-mui.material.toolbar         :refer [toolbar]]
   [reagent-mui.material.tooltip         :refer [tooltip]]
   [reagent-mui.material.typography      :refer [typography]]

   [m3-forms.log :as log]
   [m3-forms.json :refer [absent absent? present?]]
   [m3-forms.schema :as json]
   [m3-forms.util :refer [valid? check-formats conjv make-id mapl vector-remove-nth]]
   [m3-forms.render :refer [render-2 render-1 get-m1]]

   [m3-forms.divorce :refer [security-model]] ;TODO: should not be here
   ))

(defn key->label
  "Convert a camelCase or snake_case property key to a readable label.
   e.g. \"fullName\" -> \"Full Name\", \"email_address\" -> \"Email Address\""
  [k]
  (when (string? k)
    (-> k
        (str/replace #"([a-z])([A-Z])" "$1 $2")
        (str/replace #"[_-]" " ")
        (str/replace #"\b\w" str/upper-case))))

(let [state (r/atom {:roles (security-model "roles") :active-roles #{} :active-tab 0})]

  (defn inbox-button []
    (fn []
      [icon-button
       {:size "large"
        :aria-label "inbox"
        :color "inherit"}
       [badge
        {:badge-content 17
         :color "error"}
        [notifications]]]))

  (defn edit-button []
    (fn []
      [icon-button
       {:size "large"
        :aria-label "edit"
        :color "inherit"}
       [edit]]))

  (defn workflow-button []
    (fn []
      [icon-button
       {:size "large"
        :aria-label "workflow"
        :color "inherit"}
       [work]]))

  (defn change-role [e]
    (let [t (.-target e)]
      (swap! state update :active-roles (fn [rs] ((if (.-checked t) conj disj) rs (.-name t))))))

  (defn role-menu [id roles]
    (fn []
      [menu
       {:id "menu-appbar"
        :anchor-el (fn [] (id @state))
        :open (boolean (id @state))
        :on-close #(swap! state dissoc id)
        :anchor-origin {:vertical "top" :horizontal "right"}
        :transform-origin {:vertical "top" :horizontal "right"}}
       (doall
        (map
         (fn [role]
           [menu-item
            {:key role}
            [checkbox
             {:name role
              :aria-label role
              :checked (contains? (:active-roles @state) role)
              :on-change change-role}]
            [typography {:text-align :center} role]])
         roles))]))

  (defn role-button [id]
    (fn []
      [tooltip {:title "Select Role"}
       [icon-button
        {:size "large"
         :aria-label "roles"
         :color "inherit"
         :on-click (fn [e] (swap! state assoc id (.-target e)))}
        [badge
         {:badge-content (count (:active-roles @state))
          :color "info"}
         [account-circle]]]]))

  (defn my-app-bar []
    (fn []
      [box {:sx {:flex-grow 1}}
       [app-bar {:position :static}
        [toolbar {:disable-gutters true}
         [edit-button]
         [inbox-button]
         [workflow-button]
         [box {:sx {:flex-grow 1}}]
         [box {:sx {:display {:xs :none :md :flex}}}
          [role-menu :role-button (:roles @state)]
          [role-button :role-button]]]]]))

  (defn tab-panel [{i :index at :active-tab} child]
    (let [id (str "simple-tabpanel-" i)
          active? (= i at)]
      [:div
       {:role "tabpanel"
        :hidden (not active?)
        :id id
        :aria-label id}
       (when active?
         [box
          {:sx {:p 3}}
          [typography {:component :div} child]])]))

  (defn transition-buttons [bs]
    (when (seq bs)
      [box {:sx {:display :flex :justify-content :center}}
       [button-group
        {:variant :contained}
        (doall
         (map-indexed
          (fn [i {b "title" s "state"}]
            [button {:key i :on-click (fn [_e] (rf/dispatch [:transition s]))}
             b])
          bs))]]))

  (def item identity)

  (defn view-forms [{:keys [m2 m1 workflow state-id active-tab expanded]}]
    (let [states (get workflow "states")
          state-index (or (some (fn [[i s]] (when (= (get s "$id") state-id) i))
                                (map-indexed vector states))
                          0)
          current-state (get states state-index)
          views (get current-state "views")
          transitions (get current-state "transitions")
          m2-props (get m2 "properties")
          ;; "everything" means render the whole object
          everything? (and (= 1 (count views)) (= ["everything"] (first views)))]
      [grid {:container true :spacing 2 :justify-content :center}
       ;; Stepper (only if >1 state)
       (when (> (count states) 1)
         [grid {:item true :xs 12}
          [item
           [stepper {:active-step state-index :alternative-label true}
            (doall
             (map (fn [{id "$id"}]
                    (let [label (or (some->> (get (some #(when (= (get % "$id") id) %) states) "views")
                                             first first
                                             (get m2-props)
                                             (#(get % "title")))
                                    id)]
                      [step {:key id} [step-label label]]))
                  states))]]])
       ;; Top transition buttons
       [grid {:item true :xs 12 :justify-content :center}
        [item [transition-buttons transitions]]]
       ;; Views
       [grid {:item true :xs 12}
        [item
         (if everything?
           ;; Single "everything" view — render the whole object
           [:div
            ((render-1 {:ui ::mui :root m2 :check-format check-formats
                        :draft "latest" :$ref-merger :merge-over
                        :expanded expanded}
              [:m2] nil m2)
             {:root m1} [:m1] nil m1)]
           ;; Multiple views — render as tabs
           [box {:sx {:width "100%"}}
            [box {:sx {:border-bottom 1 :border-color "divider"}}
             [tabs {:variant :fullWidth
                    :value (min active-tab (max 0 (dec (count views))))
                    :on-change (fn [_e i] (rf/dispatch [:set-active-tab i]))}
              (doall
               (map-indexed
                (fn [i view-path]
                  (let [prop-name (first view-path)
                        title (or (get-in m2-props [prop-name "title"]) prop-name)]
                    [tab {:key i :label title :value i}]))
                views))]]
            (doall
             (map-indexed
              (fn [i view-path]
                (let [prop-name (first view-path)
                      view-m2 (get m2-props prop-name)
                      view-m1 (get m1 prop-name)]
                  [tab-panel {:key i :index i :active-tab active-tab}
                   ((render-1 {:ui ::mui :root m2 :check-format check-formats
                               :draft "latest" :$ref-merger :merge-over
                               :expanded expanded}
                     [:m2 "properties" prop-name] nil view-m2)
                    {:root m1} [:m1 prop-name] nil view-m1)]))
              views))])]]
       ;; Bottom transition buttons
       [grid {:item true :xs 12 :justify-content :center}
        [item [transition-buttons transitions]]]])))

;;------------------------------------------------------------------------------

(derive ::mui :m3-forms.render/html5) ;; if no mui method available, use the html5 one...

(def masked-text-field
  (r/adapt-react-class
   (IMaskMixin
    (fn [props]
      (r/as-element [text-field (rename-keys (js->clj props) {})])))))

(defn render-money-2 [c2 p2 k2 {t "type" title "title" des "description" es "enum" d "default" c "const" min "minimum" max "maximum" mo "multipleOf" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        scale (if (= t "number") 2 0)
        signed (or (not min) (< min 0))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [helper (when (and req? (absent? m1)) "Required")]
        [:div {:style {:padding "8px"} :class (v? c1 m1)}
         [masked-text-field
          {:mask "£num"
           :blocks {"num"
                    {:mask js/Number,
                     :radix "."
                     :scale scale
                     :signed signed
                     :thousands-separator ","
                     :pad-fractional-zeros false
                     :normalize-zeros true
                     :min min
                     :max max}}
           :value (str m1)
           :unmask true
           :label label
           :required req?
           :full-width true}]]))))

(def render-money (memoize render-money-2))

(defmethod render-2 [::mui "integer" "money"] [c2 p2 k2 m2]
  (render-money c2 p2 k2 m2))

(defmethod render-2 [::mui "number" "money"] [c2 p2 k2 m2]
  (render-money c2 p2 k2 m2))

(defn render-imaskjs-2 [c2 p2 k2 {title "title" des "description" es "enum" d "default" c "const" :as m2} mask unmask]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      [:div {:style {:padding "8px"} :class (v? c1 m1)}
       [masked-text-field
        {:mask mask
         :value m1
         :unmask unmask
         :placeholder-char \_
         :lazy false
         :overwrite true
         :label label
         :required req?
         :full-width true}]])))

(def render-imaskjs (memoize render-imaskjs-2))

(defmethod render-2 [::mui "string" "bank-sort-code"] [c2 p2 k2 m2]
  (render-imaskjs c2 p2 k2 m2 "00-00-00" false))

(defmethod render-2 [::mui "string" "bank-account-number"] [c2 p2 k2 m2]
  (render-imaskjs c2 p2 k2 m2 "{#} 00000000" true))

(defmethod render-2 [::mui "string" "telephone-number"] [c2 p2 k2 m2]
  (render-imaskjs c2 p2 k2 m2 "{+44 (\\0)}0000 000000" false))

(defmethod render-2 [::mui "string" :default]
  [c2 p2 k2
   {title "title" des "description" es "enum" d "default" c "const" minL "minLength" maxL "maxLength" ro "readOnly" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (or (absent? m1) (= "" m1))) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         (if (seq es)
           [text-field {:select true :label label :error (or err? (some? helper)) :full-width true
                        :required req? :helper-text helper
                        :value (or (when (present? m1) m1) "")
                        :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}
            (doall (map (fn [v] [menu-item {:key v :value v} v]) es))]
           [text-field
            {:label label :placeholder d :read-only (boolean (or c ro)) :error (or err? (some? helper)) :full-width true
             :required req? :helper-text helper
             :value (or c (when (present? m1) m1) "")
             :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}])]))))

(defmethod render-2 [::mui "string" "date"]
  [c2 p2 k2 {title "title" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (or (absent? m1) (= "" m1))) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         [text-field {:type "date" :label label :error (or err? (some? helper)) :full-width true
                      :required req? :helper-text helper
                      :value (or (when (present? m1) m1) "")
                      :InputLabelProps {:shrink true}
                      :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}]]))))

(defmethod render-2 [::mui "string" "date-time"]
  [c2 p2 k2 {title "title" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (or (absent? m1) (= "" m1))) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         [text-field {:type "datetime-local" :label label :error (or err? (some? helper)) :full-width true
                      :required req? :helper-text helper
                      :value (or (when (present? m1) m1) "")
                      :InputLabelProps {:shrink true}
                      :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}]]))))

(defmethod render-2 [::mui "string" "time"]
  [c2 p2 k2 {title "title" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (or (absent? m1) (= "" m1))) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         [text-field {:type "time" :label label :error (or err? (some? helper)) :full-width true
                      :required req? :helper-text helper
                      :value (or (when (present? m1) m1) "")
                      :InputLabelProps {:shrink true}
                      :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}]]))))

(defmethod render-2 [::mui "string" "year-month"]
  [c2 p2 k2 {title "title" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (or (absent? m1) (= "" m1))) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         [text-field {:type "month" :label label :error (or err? (some? helper)) :full-width true
                      :required req? :helper-text helper
                      :value (or (when (present? m1) m1) "")
                      :InputLabelProps {:shrink true}
                      :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}]]))))

(defmethod render-2 [::mui "null" :default]
  [c2 p2 k2 {title "title" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))]
    (fn [c1 p1 k1 m1]
      [:div {:style {:padding "8px"} :class (v? c1 m1)}
       [typography {:variant :body2 :color "text.secondary"}
        (str (or label "Null") ": ")
        [:em "null"]]
       [button {:size :small :variant :outlined
                :on-click (fn [_] (rf/dispatch [:assoc-in p1 nil]))}
        "Set null"]])))

(defmethod render-2 [::mui "integer" "range"]
  [c2 p2 k2 {title "title" min "minimum" max "maximum" mo "multipleOf" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))]
    (fn [c1 p1 k1 m1]
      [:div {:style {:padding "8px"} :class (v? c1 m1)}
       [typography {:gutterBottom true} label]
       [slider {:value (or (when (present? m1) m1) (or min 0))
                :min (or min 0) :max (or max 100) :step (or mo 1)
                :value-label-display "auto"
                :on-change (fn [_e v] (rf/dispatch [:assoc-in p1 v]))}]])))

(defmethod render-2 [::mui "number" "range"]
  [c2 p2 k2 {title "title" min "minimum" max "maximum" mo "multipleOf" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))]
    (fn [c1 p1 k1 m1]
      [:div {:style {:padding "8px"} :class (v? c1 m1)}
       [typography {:gutterBottom true} label]
       [slider {:value (or (when (present? m1) m1) (or min 0))
                :min (or min 0) :max (or max 100) :step (or mo 0.1)
                :value-label-display "auto"
                :on-change (fn [_e v] (rf/dispatch [:assoc-in p1 v]))}]])))

(defmethod render-2 [::mui "integer" :default]
  [c2 p2 k2 {title "title" d "default" c "const" min "minimum" max "maximum" es "enum" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (absent? m1)) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         (if (seq es)
           [text-field {:select true :label label :error (or err? (some? helper)) :full-width true
                        :required req? :helper-text helper
                        :value (or (when (present? m1) m1) "")
                        :on-change (fn [e] (rf/dispatch [:assoc-in p1 (js/parseInt (.-value (.-target e)) 10)]))}
            (doall (map (fn [v] [menu-item {:key v :value v} (str v)]) es))]
           [text-field {:type "number" :label label :error (or err? (some? helper)) :full-width true
                        :required req? :helper-text helper
                        :value (or c (when (present? m1) m1) "")
                        :input-props {:min min :max max}
                        :on-change (fn [e] (let [v (.-value (.-target e))]
                                             (if (empty? v)
                                               (rf/dispatch [:delete-in p1])
                                               (rf/dispatch [:assoc-in p1 (js/parseInt v 10)]))))}])]))))

(defmethod render-2 [::mui "number" :default]
  [c2 p2 k2 {title "title" d "default" c "const" min "minimum" max "maximum" es "enum" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")
            helper (when (and req? (absent? m1)) "Required")]
        [:div {:style {:padding "8px"} :class validity}
         (if (seq es)
           [text-field {:select true :label label :error (or err? (some? helper)) :full-width true
                        :required req? :helper-text helper
                        :value (or (when (present? m1) m1) "")
                        :on-change (fn [e] (rf/dispatch [:assoc-in p1 (js/parseFloat (.-value (.-target e)))]))}
            (doall (map (fn [v] [menu-item {:key v :value v} (str v)]) es))]
           [text-field {:type "number" :label label :error (or err? (some? helper)) :full-width true
                        :required req? :helper-text helper
                        :value (or c (when (present? m1) m1) "")
                        :on-change (fn [e] (let [v (.-value (.-target e))]
                                             (if (empty? v)
                                               (rf/dispatch [:delete-in p1])
                                               (rf/dispatch [:assoc-in p1 (js/parseFloat v)]))))}])]))))

(defmethod render-2 [::mui "boolean" :default]
  [c2 p2 k2 {title "title" c "const" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        req? (contains? (:required-keys c2) k2)]
    (fn [c1 p1 k1 m1]
      (let [validity (v? c1 m1)
            err? (= validity "invalid")]
        [:div {:style {:padding "8px"} :class validity}
         [form-control {:error err? :required req?}
          [checkbox {:checked (boolean (and (present? m1) m1))
                     :read-only (boolean c)
                     :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-checked (.-target e))]))}]
          [input-label (str label (when req? " *"))]]]))))

(defmethod render-2 [::mui "oneOf" :default]
  [c2 p2 k2 {oos "oneOf" t "title" des "description" :as m2}]
  (let [v? (valid? c2 m2)
        label (or t (key->label k2))]
    (fn [c1 p1 k1 m1]
      (let [{valid true invalid false}
            (if (present? m1)
              (group-by
               (comp not seq second)
               (mapv (fn [oo] [oo ((json/check-schema c2 p1 oo) c1 [] m1)]) oos))
              {})
            num-valid (count valid)
            match (ffirst valid)
            labels (mapv (fn [oo] (let [oo (json/expand-$ref c2 p2 oo)]
                                    (or (oo "title") (get-in oo ["properties" "type" "const"]) "?")))
                         oos)]
        [:div {:style {:padding "8px"} :class (v? c1 m1)}
         [paper {:sx {:p 2} :variant :outlined}
          (when label [typography {:variant :h6 :gutterBottom true} label])
          [text-field {:select true :label (or label "Select type") :full-width true
                       :value (or (some (fn [[i oo]] (when (= oo match) i)) (map-indexed vector oos)) "")
                       :on-change (fn [e] (let [idx (js/parseInt (.-value (.-target e)) 10)
                                                oo (nth oos idx)
                                                new-m1 (get-m1 c2 p2 oo)]
                                            (rf/dispatch [:assoc-in p1 new-m1])))}
           (doall (map-indexed (fn [i label] [menu-item {:key i :value i} label]) labels))]
          (when match
            [:div {:style {:padding-top "8px"}}
             ((render-1 c2 p2 k2 match) c1 p1 k1 m1)])]]))))

(defmethod render-2 [::mui "object" :default]
  [{expanded? :expanded ok :original-key :as c2}
   p2 k2
   {ps "properties" pps "patternProperties" aps "additionalProperties" title "title" es "enum" req "required" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))
        required-set (set req)]
    (fn [c1 p1 k1 m1]
      [:div {:style {:padding "8px"} :class (v? c1 m1)}
       [paper {:sx {:p 2} :variant :outlined}
        (when label [typography {:variant :h6 :gutterBottom true} label])
        [stack {:spacing 2}
         (doall
          (map
           (fn [[k {t "title" d "description" :as m2}]]
             (let [p2 (vec (concat p2 ["properties" k]))
                   p1 (conjv p1 k)
                   id (make-id p1)
                   child-c2 (assoc c2 :required-keys required-set)]
               [:div {:key id} ((render-1 child-c2 p2 k m2) c1 p1 k (get (when (present? m1) m1) k absent))]))
           ps))]]])))

(defmethod render-2 [::mui "array" :default]
  [c2 p2 k2
   {title "title" {def "default" :as is} "items" pis "prefixItems" maxIs "maxItems" ro? "readOnly" :as m2}]
  (let [v? (valid? c2 m2)
        label (or title (key->label k2))]
    (fn [c1 p1 k1 m1]
      (let [m1 (when (present? m1) m1)]
        [:div {:style {:padding "8px"} :class (v? c1 m1)}
         [stack
          (when label [typography {:variant :subtitle1 :gutterBottom true} label])
          [table-container {:component Paper}
           [table {:sx {} :size :small}
            [table-body
             (mapl
              (fn [n [prefix? m2 m1]]
                (let [p2 (vec (concat p2 (if prefix? ["prefixItems" n] ["items"])))
                      k2 nil
                      p1 (conjv p1 n)
                      k1 n
                      draggable? (and (not prefix?) (not ro?))]
                  [table-row {:key (make-id p1)
                              :draggable draggable?
                              :on-drag-start (fn [e]
                                               (.setData (.-dataTransfer e) "text/plain" (str n))
                                               (set! (.-effectAllowed (.-dataTransfer e)) "move"))
                              :on-drag-over (fn [e] (.preventDefault e)
                                              (set! (.-dropEffect (.-dataTransfer e)) "move"))
                              :on-drop (fn [e]
                                         (.preventDefault e)
                                         (let [from-idx (js/parseInt (.getData (.-dataTransfer e) "text/plain") 10)]
                                           (when-not (= from-idx n)
                                             (rf/dispatch [:array-reorder (vec (butlast p1)) from-idx n]))))}
                   (when draggable?
                     [table-cell {:sx {:width 32 :cursor "grab" :color "text.secondary"} :align :center}
                      [drag-indicator {:font-size :small}]])
                   [table-cell
                    ((render-1 c2 p2 k2 m2) c1 p1 k1 m1)]
                   [table-cell {:align :left}
                    (when draggable?
                      [button
                       {:start-icon (r/as-element [delete-forever])
                        :on-click (fn [_] (rf/dispatch [:update-in (butlast p1) vector-remove-nth (last p1)]))}])]]))
              (range)
              (concat
               (map
                (fn [m2 m1] [true m2 m1])
                pis
                (concat m1 (repeat nil)))
               (map
                (fn [m2 m1] [false m2 m1])
                (repeat is)
                (drop (count pis) m1))))]
            [table-footer
             [table-row
              [table-cell {:align :center :col-span 3}
               (when (and (not ro?) (or (not maxIs) (< (count m1) maxIs)))
                 [button {:start-icon (r/as-element [add-box])
                          :on-click (fn [_] (rf/dispatch [:update-in p1 (fnil conj []) (get-m1 c2 p2 is)]))}
                  "Add"])]]]]]]]))))
