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

(ns m3-forms.render
  (:require
   [cljs.reader :as rdr]
   [goog.string :refer [format]]
   [re-frame.core :as rf]
   [m3-forms.log :as log]
   [m3-forms.util :refer [valid? conjv make-id vector-remove-nth]]
   [m3-forms.json :refer [absent present? absent?]]
   [m3-forms.schema :as json]))

;;------------------------------------------------------------------------------

(defn log [& args]
  ;;(apply println args)
  )

;;------------------------------------------------------------------------------

(defn map-remove-nth [m n]
  (apply array-map (flatten (vector-remove-nth m n))))

(defn map-rename-nth [m n k]
  (apply array-map (flatten (assoc-in (vec m) [n 0] k))))

(defn conjm [m [k v]]
  (apply array-map (concat (flatten (seq m)) [k v])))

;;------------------------------------------------------------------------------

(defn drop-down [_context path es e]
  (if (> (count es) 1000)
    (let [id (make-id (conj path "data"))]
      [:div
       [:datalist {:id id}
        (map
         (fn [n [k v]]
           [:option {:key (make-id (conjv path n)) :value (if (string? v) v (pr-str v)) :label k}])
         (range) es)]
       [:input
        {:list id
         :autoComplete "off"
         :value (if (and (present? e) e) (pr-str e) "")
         :on-change (fn [e] (let [v (rdr/read-string (.-value (.-target e)))] (if (empty? v) (rf/dispatch [:delete-in path]) (rf/dispatch [:assoc-in path v]))))}]])

    [:select
     {:value (if (and (present? e) e) (pr-str e) "")
      :on-change (fn [e] (let [v (rdr/read-string (.-value (.-target e)))] (println "DROP-DOWN:" path v) (rf/dispatch (if v [:assoc-in path v] [:delete-in path]))))}
     (map
      (fn [n [k v]]
        [:option {:key (make-id (conjv path n)) :value (pr-str v)} k])
      (range) (concat [["" nil]] es))]))

(defn squidgy-button [expanded path]
  (if (expanded path)
    [:td [:button {:on-click (fn [e] (rf/dispatch [:collapse path]))} "v"]]
    [:td [:button {:on-click (fn [e] (rf/dispatch [:expand path]))} "^"]]))

(defn squidgy? [{oo "oneOf" t "type" :as m2} m1]
  (cond
    oo true
    (= t "object") true
    :else false))

;;------------------------------------------------------------------------------

(defn render-key [{ui :ui :or {ui ::html5}} {oo "oneOf" t "type" f "format" :or {f :default}}]
  [ui (cond oo "oneOf" :else t) f])

(defmulti render-2 (fn [c2 p2 k2 m2] (render-key c2 m2)))

(def render (memoize render-2))

(defn render-1 [c2 p2 k2 m2]
  (let [expanded (json/expand-$ref c2 p2 m2)]
    (if (map? expanded)
      (let [r (render c2 p2 k2 expanded)]
        (fn [c1 p1 k1 m1]
          (r c1 p1 k1 (json/expand-$ref c2 p1 m1))))
      (fn [_c1 _p1 _k1 _m1] nil))))

(derive ::m0 ::html5)

(defmethod render-2 :default [c2 p2 k2 m2]
  (let [rk (render-key c2 m2)]
    (fn [c1 p1 k1 m1]
      (when (or m2 m1)
        (log/warn "render: no specific method:" rk m2 m1)))))

(defmethod render-2 [::html5 "null" :default] [c2 p2 k2 {title "title" des "description" :as m2}]
  (let [v? (valid? c2 m2)]
    (fn [c1 p1 k1 m1]
      (log "NULL:" p1 k1 m1)
      [:div {:style {:background "#cc99ff"} :class (v? c1 m1)}
       [:button {:on-click (fn [e] (rf/dispatch [:assoc-in p1 nil]))} "+"]])))

(defmethod render-2 [::html5 "boolean" :default]
  [c2 p2 k2
   {title "title" des "description" d "default" c "const" :as m2}]
  (let [v? (valid? c2 m2)
        ro (boolean c)]
    (fn [c1 p1 k1 m1]
      [:div {:style {:background "#ffcc66"} :class (v? c1 m1)}
       [:input {:type "checkbox" :read-only ro
                :checked (and (present? m1) m1)
                :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-checked (.-target e))]))}]])))

(defn render-number-2 [c2 p2 k2
                       {title "title" des "description" es "enum" d "default" c "const" min "minimum" max "maximum" mo "multipleOf" :as m2} f]
  (let [v? (valid? c2 m2)
        ro (boolean c)
        ddes (map (juxt identity identity) es)]
    (fn [c1 p1 k1 m1]
      (log "NUMBER:" p1 k1 m1)
      [:div {:style {:background "#99ff99"} :class (v? c1 m1)}
       (if (seq es)
         (drop-down c2 p1 ddes (when (present? m1) m1))
         [:input {:type f  :max max :min min :step mo :placeholder d :read-only ro :value (or c (when (present? m1) m1))
                  :on-change (fn [e]
                               (let [v (.-value (.-target e))]
                                 (if (empty? v)
                                   (rf/dispatch [:delete-in p1])
                                   (rf/dispatch [:assoc-in p1 (js/parseFloat v 10)]))))}])])))

(def render-number (memoize render-number-2))

(defmethod render-2 [::html5 "number" :default] [c2 p2 k2 m2]
  (render-number c2 p2 k2 m2 "number"))

(defmethod render-2 [::html5 "number" "range"] [c2 p2 k2 m2]
  (render-number c2 p2 k2 m2 "range"))

(defn render-integer-2 [c2 p2 k2
                        {title "title" des "description" es "enum" d "default" c "const" min "minimum" max "maximum" mo "multipleOf" :as m2} f]
  (let [v? (valid? c2 m2)
        ro (boolean c)
        ddes (map (juxt identity identity) es)]
    (fn [c1 p1 k1 m1]
      (log "INTEGER:" p1 k1 m1)
      [:div {:style {:background "#ccff33"} :class (v? c1 m1)}
       (if (seq es)
         (drop-down c2 p1 ddes (when (present? m1) m1))
         [:input {:type f :max max :min min :step mo :placeholder d :read-only ro :value (or c (when (present? m1) m1))
                  :on-change (fn [e]
                               (let [v (.-value (.-target e))]
                                 (if (empty? v)
                                   (rf/dispatch [:delete-in p1])
                                   (rf/dispatch [:assoc-in p1 (js/parseInt v 10)]))))}])])))

(def render-integer (memoize render-integer-2))

(defmethod render-2 [::html5 "integer" :default] [c2 p2 k2 m2]
  (render-integer c2 p2 k2 m2 "number"))

(defmethod render-2 [::html5 "integer" "range"] [c2 p2 k2 m2]
  (render-integer c2 p2 k2 m2 "range"))

(defn render-string-2 [c2 p2 k2
                       {title "title" des "description" es "enum" d "default" c "const" minL "minLength" maxL "maxLength" ro "readOnly" :as m2} f]
  (let [v? (valid? c2 m2)
        readOnly (or (boolean c) ro)
        ddes (map (juxt identity identity) es)]
    (fn [c1 p1 k1 m1]
      (log "STRING:" p1 k1 m1)
      [:div {:style {:background "#ffff99"} :class (v? c1 m1)}
       (if (seq es)
         (drop-down c2 p1 ddes (when (present? m1) m1))
         [:input {:type f :placeholder d :value (when (present? m1) m1) :readOnly readOnly :minLength minL :maxLength maxL :size maxL
                  :on-change (fn [e] (rf/dispatch [:assoc-in p1 (.-value (.-target e))]))}])])))

(def render-string (memoize render-string-2))

(defmethod render-2 [::html5 "string" :default] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "text"))
(defmethod render-2 [::html5 "string" "time"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "time"))
(defmethod render-2 [::html5 "string" "date"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "date"))
(defmethod render-2 [::html5 "string" "date-time"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "datetime-local"))
(defmethod render-2 [::html5 "string" "week"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "week"))
(defmethod render-2 [::html5 "string" "year-month"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "month"))
(defmethod render-2 [::html5 "string" "uri-reference"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "text"))
(defmethod render-2 [::html5 "string" "regex"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "text"))
(defmethod render-2 [::html5 "string" "bank-sort-code"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "text"))
(defmethod render-2 [::html5 "string" "bank-account-number"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "text"))
(defmethod render-2 [::html5 "string" "telephone-number"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "text"))
(defmethod render-2 [::html5 "integer" "money"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "number"))
(defmethod render-2 [::html5 "number" "money"] [c2 p2 k2 m2] (render-string c2 p2 k2 m2 "number"))

(defn get-m1 [c2 p2 m2]
  (let [{m1t "type" {{m2t "const"} "type" one-of-m3 "oneOf" any-of-m3 "anyOf" all-of-m3 "allOf"} "properties" one-of-m2 "oneOf" any-of-m2 "anyOf" all-of-m2 "allOf" :as m2} (json/expand-$ref c2 p2 m2)
        result
        (cond
          m2t {"type" m2t}
          m1t ({"boolean" true "string" "" "integer" 0 "number" 0.0 "null" nil "array" [] "object" {}} m1t)
          (= m1t "boolean") true
          one-of-m3 {"oneOf" []}
          any-of-m3 {"anyOf" []}
          all-of-m3 {"allOf" []}
          one-of-m2 true
          any-of-m2 true
          all-of-m2 true
          :else
          (println "get-m1: can't resolve m2:" (pr-str m2) "to an m1"))]
    result))

(defmethod render-2 [::html5 "object" :default]
  [{expanded? :expanded ok :original-key :as c2}
   p2 k2
   {ps "properties" pps "patternProperties" aps "additionalProperties" title "title" es "enum" :as m2}]
  (let [v? (valid? c2 m2)]
    (fn [c1 p1 k1 m1]
      (log "OBJECT:" "M2:" [p2 k2 m2] "M1:" [p1 k1 m1])
      [:div {:style {:background "#99ccff"} :class (v? c1 m1)}
       (if (seq es)
         (drop-down c2 p1 (map (juxt (fn [{t "title"}] t) identity) es) (when (present? m1) m1))
         (let [extra-ps-m1 (apply dissoc (when (present? m1) m1) (keys ps))
               pattern-ps-m2s-and-m1 (filter
                                      (fn [[m2s]] (seq m2s))
                                      (map
                                       (juxt (fn [[epk]] (reduce (fn [acc [ppk :as pp]] (if (re-find (re-pattern ppk) epk) (conj acc pp) acc)) [] pps)) identity)
                                       extra-ps-m1))
               pattern-ps-m1 (map second pattern-ps-m2s-and-m1)
               additional-ps (apply dissoc extra-ps-m1 (keys pattern-ps-m1))]
           [:table {:border 1}
            (when title [:caption [:h4 title]])
            [:tbody
             (when ps
               [:tr {:onDragOver (fn [e] (.preventDefault e))
                     :onDrop (fn [e]
                               (.preventDefault e)
                               (rf/dispatch [:move (rdr/read-string (.getData (.-dataTransfer e) "m1")) p1]))}
                [:td
                 [:table {:border 1}
                  [:caption "Named Properties"]
                  [:tbody
                   (doall
                    (map
                     (fn [[k {t "title" d "description" :as m2}]]
                       (let [p2 (vec (concat p2 ["properties" k]))
                             p1 (conjv p1 k)
                             id (make-id p1)
                             squidgable? (squidgy? m2 m1)
                             visible? (or (and squidgable? (expanded? p2)) (not squidgable?))]
                         ;; TODO: zip dispatch causes re-render cascade, re-enable for drag-and-drop
                         ;; (rf/dispatch [:update-in [:zip] (fnil assoc {}) p2 p1])
                         [:tr {:key id :id id :title d
                               :draggable true
                               :onDragStart (fn [e] (.setData (.-dataTransfer e) "m1" p1))
                               :onDragOver (fn [e] (.preventDefault e))
                               :onDrop (fn [e]
                                         (.preventDefault e)
                                         (.stopPropagation e)
                                         (rf/dispatch [:move (rdr/read-string (.getData (.-dataTransfer e) "m1")) p1]))}
                          [:td
                           [:table {:border 1}
                            (when t [:caption [:h4 t]])
                            [:tbody
                             [:tr
                              [:td [:input {:type "text" :value k :read-only true}]]
                              (when visible? [:td ((render-1 c2 p2 k m2) c1 p1 k (get (when (present? m1) m1) k absent))])
                              (when squidgable? (squidgy-button expanded? p2))
                              [:td [:button {:on-click (fn [e] (rf/dispatch [:delete-in p1]))} "-"]]]]]]]))
                     ps))]]]])
             (when pps
               [:tr
                [:td
                 [:table {:border 1}
                  [:caption "Pattern Properties"]
                  [:tbody
                   (concat
                    (map
                     (fn [[pattern {t "title" d "description" :as schema}]]
                       (let [p2 (vec (concat p2 ["patternProperties" pattern]))
                             id2 (make-id p2)
                             r (render-1 c2 p2 pattern schema)]
                         [:tr {:key id2 :id  id2 :title pattern}
                          [:td [:input {:type "text" :value pattern :read-only true}]]
                          [:td
                           [:table  {:border 1}
                            [:tbody
                             (map
                              (fn [[k v]]
                                (let [p2 (conj p2 k)
                                      p1 (conj p1 k)
                                      id1 (make-id p1)]
                                  [:tr {:key id1 :id  id1 :title (or t d)}
                                   [:td [:input {:type "text" :value k :pattern pattern :read-only true}]]
                                   (when (expanded? p2) [:td (r c1 p1 k v)])
                                   (squidgy-button expanded? p2)]))
                              (filter
                               (fn [[k]] (re-find (re-pattern pattern) k))
                               (when (present? m1) m1)))]]]]))
                     pps)
                    [[:tr {:key (make-id (conjv p2 "plus")) :align :center}
                      [:td [:button "+"]]
                      [:td [:button "+"]]]])]]]])

             (when aps
               [:tr {:onDragOver (fn [e] (.preventDefault e))
                     :onDrop (fn [e]
                               (.preventDefault e)
                               (rf/dispatch [:move (rdr/read-string (.getData (.-dataTransfer e) "m2")) p1]))}
                [:td
                 [:table {:border 1}
                  [:caption "Additional Properties"]
                  [:tbody
                   (doall
                    (concat
                     (map
                      (fn [n [k v]]
                        (let [p2 (conj p2 "additionalProperties")
                              old-p1 p1
                              p1 (conjv p1 k)
                              ok2 (get ok [old-p1 k])
                              id (make-id (if ok2 (conjv old-p1 ok2) p1))]
                          [:tr {:key id}
                           [:td
                            [:table {:border 1}
                             [:tbody
                              [:tr {:draggable (= "properties" k2)
                                    :onDragStart (fn [e] (.setData (.-dataTransfer e) "m2" p1))
                                    :onDragOver (fn [e] (.preventDefault e))
                                    :onDrop (fn [e]
                                              (.preventDefault e)
                                              (.stopPropagation e)
                                              (rf/dispatch [:move (rdr/read-string (.getData (.-dataTransfer e) "m2")) (conj (vec (butlast p1)) n)]))}
                               ;; TODO: zip dispatch causes re-render cascade, re-enable for drag-and-drop
                               ;; (rf/dispatch [:update-in [:zip] (fnil assoc {}) (conj p2 k) p1])
                               [:td [:input {:type "text" :value k :read-only false
                                             :on-change (fn [e] (let [siblings (disj (set (keys m1)) k) v (.-value (.-target e))] (if (siblings v) (log/warn "cannot name property same as sibling: " p1 v) (rf/dispatch [:rename-in old-p1 k v]))))
                                             :onBlur (fn [e] (rf/dispatch [:rename-in-tidy]))}]]
                               (when (expanded? (conj p2 k)) [:td ((render-1 c2 p2 k (when (map? aps) aps)) c1 p1 k v)])
                               (squidgy-button expanded? (conj p2 k))
                               [:td [:button {:on-click (fn [e] (rf/dispatch [:delete-in p1]))} "-"]]]]]]]))
                      (range)
                      additional-ps)
                     [[:tr {:key (make-id (conjv p1 "plus")) :align :center}
                       [:td
                        [:button
                         {:on-click (fn [e] (println "CLICK:") (rf/dispatch [:update-in p1 conjm [(str "property-" (count (when (present? m1) m1))) (get-m1 c2 p2 aps)]]))}
                         "+"]]]]))]]]])]]))])))

(defn render-array [minIs maxIs read-only? v? parent-path m1 rows on-add]
  [:div {:style {:background "#ffcccc"} :class v?}
   [:table
    [:tbody
     (doall
      (concat
       (map
        (fn [[path k fixed? delete-f td]]
          (let [draggable? (and (not read-only?) fixed?)]
            [:tr {:key (make-id path)
                  :draggable draggable?
                  :on-drag-start (fn [e]
                                   (.setData (.-dataTransfer e) "text/plain" (str k))
                                   (set! (.-effectAllowed (.-dataTransfer e)) "move"))
                  :on-drag-over (fn [e]
                                  (.preventDefault e)
                                  (set! (.-dropEffect (.-dataTransfer e)) "move")
                                  (let [tr (.closest (.-target e) "tr")]
                                    (when tr (.add (.-classList tr) "drag-over"))))
                  :on-drag-leave (fn [e]
                                   (let [tr (.closest (.-target e) "tr")]
                                     (when tr (.remove (.-classList tr) "drag-over"))))
                  :on-drop (fn [e]
                             (.preventDefault e)
                             (let [tr (.closest (.-target e) "tr")]
                               (when tr (.remove (.-classList tr) "drag-over")))
                             (let [from-idx (js/parseInt (.getData (.-dataTransfer e) "text/plain") 10)]
                               (when-not (= from-idx k)
                                 (rf/dispatch [:array-reorder parent-path from-idx k]))))}
             [:td {:class "drag-handle" :style {:width "20px"}} (when draggable? "\u2261")]
             [:td td]
             [:td (when draggable? [:button {:on-click (fn [_e] (rf/dispatch [:update-in parent-path delete-f k]))} "-"])]]))
        rows)
       (when (and (not read-only?) (or (not maxIs) (< (count (when (present? m1) m1)) maxIs)))
         [[:tr {:key (make-id (conjv parent-path "plus")) :align :center}
           [:td]
           [:td [:button {:on-click on-add} "+"]]]])))]]])

(defmethod render-2 [::html5 "array" :default]
  [c2 p2 k2 {{def "default" :as is} "items" pis "prefixItems" minIs "minItems" maxIs "maxItems" ro "readOnly" :as m2}]
  (let [v? (valid? c2 m2)]
    (fn [c1 p1 k1 m1]
      (let [rows
            (map
             (fn [n [prefix? m2] m1]
               (let [[p2-suffix k2] (if prefix? [["prefixItems" n] n] [["items"] "items"])
                     p2 (vec (concat p2 p2-suffix))
                     p1 (conjv p1 n)
                     td ((render-1 c2 p2 k2 m2) c1 p1 n m1)]
                 [p1 n (not prefix?) vector-remove-nth td]))
             (range)
             (concat (map (juxt (constantly true) identity) pis)
                     (map (juxt (constantly false)  identity) (repeat is)))
             (let [m1 (when (present? m1) m1)] (concat m1 (repeat (- (count pis) (count m1)) nil))))
            on-add (fn [_e] (rf/dispatch [:update-in p1 (fnil conj []) (get-m1 c2 p2 is)]))]
        (render-array minIs maxIs ro (v? c1 m1) p1 m1 rows on-add)))))

(defmethod render-2 [::html5 "oneOf" :default]
  [c2 p2 k2 {oos "oneOf" t "title" des "description" :as m2}]
  (let [v? (valid? c2 m2)]
    (fn [c1 p1 k1 m1]
      (let [{valid true invalid false}
            (if (present? m1)
              (group-by
               (comp not seq second)
               (mapv (fn [oo] [oo ((json/check-schema c2 p1 oo) c1 [] m1)]) oos))
              {})
            num-valid (count valid)
            match (ffirst valid)]
        (when (> num-valid 1)
          (log/warn (str "oneOf: " num-valid " schemas matched, using first match")))
        (if (or (absent? m1) (>= num-valid 1))
          [:div {:title des :style {:background "#cc6699"} :class (v? c1 m1)}
           [:table
            [:caption
             [:h4
              (str (or t "One of") ":  ")
              (drop-down c2 p1 (map (juxt (fn [oo] (let [oo (json/expand-$ref c2 p2 oo)] (or (oo "title") (get-in oo ["properties" "type" "const"] absent)))) (partial get-m1 c2 p2)) oos) (when (present? m1) (get-m1 c2 p2 match)))]]
            [:tbody
             (when match [:tr [:td ((render-1 c2 p2 k2 match) c1 p1 k1 m1)]])]]]
          (log/error [(format "oneOf: 0 schemas matched") (mapv second invalid)]))))))
