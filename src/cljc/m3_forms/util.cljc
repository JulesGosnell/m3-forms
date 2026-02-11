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

(ns m3-forms.util
  (:require
   [clojure.string :refer [join]]
   [m3-forms.json :refer [present?]]
   [m3-forms.schema :refer [check-schema]]))

(def conjv (fnil conj []))

(defn index-by [k ms]
  (into (sorted-map) (map (fn [{v k :as m}][v m]) ms)))

(def index-by-$id (partial index-by "$id"))

(defn keyfn [k] (fn [m] {m k}))

(defn make-id [path]
  (join "." path))

(defn valid? [m2-ctx m2]
  (let [v? (check-schema m2-ctx [] m2)
        ;; Compile format checker at L2 if a format and checker are available
        fmt (get m2 "format")
        check-fmt (when fmt (get (:check-format m2-ctx) fmt))
        fmt-l1 (when check-fmt (check-fmt "format" m2-ctx [] m2 fmt))]
    (fn [m1-ctx m1]
      (let [struct-errors (when (present? m1) (v? m1-ctx [] m1))
            format-errors (when (and fmt-l1 (present? m1)) (fmt-l1 m1-ctx [] m1))
            all-errors (concat struct-errors format-errors)]
        (if (seq all-errors)
          "invalid"
          "valid")))))

(defn valid-with-errors
  "Like valid? but returns a map with :class and :errors for richer validation UI.
   {:class \"valid\"/\"invalid\" :errors [\"error msg\" ...]}"
  [m2-ctx m2]
  (let [v? (check-schema m2-ctx [] m2)
        fmt (get m2 "format")
        check-fmt (when fmt (get (:check-format m2-ctx) fmt))
        fmt-l1 (when check-fmt (check-fmt "format" m2-ctx [] m2 fmt))]
    (fn [m1-ctx m1]
      (let [struct-errors (when (present? m1) (v? m1-ctx [] m1))
            format-errors (when (and fmt-l1 (present? m1)) (fmt-l1 m1-ctx [] m1))
            all-errors (concat struct-errors format-errors)]
        (if (seq all-errors)
          {:class "invalid" :errors (mapv str all-errors)}
          {:class "valid" :errors []})))))

(defn mapl [& args] (doall (apply map args)))

(defn vector-remove-nth [v n]
  (mapv second (filter (comp (complement #{n}) first) (map vector (range) v))))

;;------------------------------------------------------------------------------

;; Custom format checkers (stubs)
(defn check-format-year-month          [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-range               [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-money               [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-bank-account-number [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-bank-sort-code      [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-telephone-number    [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))

(defn resolve-schema-path
  "Walk an M2 schema following a dotted path through properties/items.
   Returns the sub-schema at the path, or nil if unresolvable.
   E.g. 'personalInformation.fullName' resolves through
   properties.personalInformation.properties.fullName."
  [m2 path-str]
  (when (and (map? m2) (string? path-str) (seq path-str))
    (let [segments (clojure.string/split path-str #"\.")]
      (loop [schema m2
             [seg & more] segments]
        (if-not seg
          schema ;; resolved successfully
          (let [;; Try object properties
                prop-schema (get-in schema ["properties" seg])]
            (if prop-schema
              (recur prop-schema more)
              ;; Try array items.properties (for paths inside 'each')
              (let [items (get schema "items")
                    item-prop (when (map? items) (get-in items ["properties" seg]))]
                (if item-prop
                  (recur item-prop more)
                  nil)))))))))

(defn check-format-mdast-ref
  "mdast-ref format checker — verifies fieldReference/each/conditional paths
   resolve against the instance root (the M2 schema being validated).
   Uses (:root m1-ctx) which m3 sets to the document root at validation time."
  [_property _m2-ctx _m2-path _m2-doc _m2-val]
  (fn [m1-ctx m1-path m1-doc]
    (when (and (string? m1-doc) (seq m1-doc))
      (when-not (resolve-schema-path (:root m1-ctx) m1-doc)
        [{:error "mdast-ref"
          :message (str "Path '" m1-doc "' does not resolve in M2 schema")
          :path m1-path}]))))

(def check-formats
  {"range"               check-format-range
   "money"               check-format-money
   "bank-account-number" check-format-bank-account-number
   "bank-sort-code"      check-format-bank-sort-code
   "telephone-number"    check-format-telephone-number
   "mdast-ref"           check-format-mdast-ref})

;;------------------------------------------------------------------------------

(defn trace [m f]
  (fn [& args]
    (prn m "->" (rest args))
    (let [r (apply f args)]
      (prn m "<-" r)
      r)))
