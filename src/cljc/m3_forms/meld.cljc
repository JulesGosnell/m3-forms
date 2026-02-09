(ns m3-forms.meld
  [:require
   [m3-forms.log :as log]
   [clojure.set :refer [intersection union]]
   [m3-forms.json :refer [absent absent? present?]]])

;;------------------------------------------------------------------------------

(defn gcd [a b]
  (if (zero? b)
    a
    (recur b (mod a b))))

(defn lcm [a b]
  (if (or (zero? a) (zero? b))
    0
    (/ (Math/abs (* a b)) (gcd a b))))

;;------------------------------------------------------------------------------

(defmulti melder (fn [ctx k l r] k))

(defn meld [m ctx left right]
  (reduce-kv
   (fn [[schema errors :as acc] k new]
     (let [old (get schema k absent)]
       (cond
         (absent? old)
         [(assoc schema k new) errors]
         (= old new)
         acc
         :else
         (if-let [melded (m ctx k old new)]
           [(assoc schema k melded) errors]
           [nil (conj errors (str "unable to meld:" k))]))))
   [left []]
   right))

(def meld-schemas (partial meld melder))
(def meld-properties (partial meld (comp first meld-schemas)))

(defmethod melder "$comment"              [ctx _ l r] nil)
(defmethod melder "$id"                   [ctx _ l r] nil)
(defmethod melder "description"           [ctx _ l r] nil)
(defmethod melder "title"                 [ctx _ l r] nil)
(defmethod melder "readOnly"              [ctx _ l r] (or l r))
(defmethod melder "writeOnly"             [ctx _ l r] (or l r))
(defmethod melder "default"               [ctx _ l r] nil)
(defmethod melder "$schema"               [ctx _ l r] nil)
(defmethod melder "contentEncoding"       [ctx _ l r] nil)
(defmethod melder "contentMediaType"      [ctx _ l r] nil)
(defmethod melder "contentSchema"         [ctx _ l r] nil)
(defmethod melder "deprecated"            [ctx _ l r] (or l r))
(defmethod melder "$defs"                 [ctx _ l r] nil)
(defmethod melder "definitions"           [ctx _ l r] nil)
(defmethod melder "examples"              [ctx _ l r] (vec (concat l r)))
(defmethod melder "type"                  [ctx _ l r] nil)
(defmethod melder "minimum"               [ctx _ l r] (max l r))
(defmethod melder "maximum"               [ctx _ l r] (min l r))
(defmethod melder "enum"                  [ctx _ l r] (vec (intersection (set l) (set r))))
(defmethod melder "const"                 [ctx _ l r] nil)
(defmethod melder "multipleOf"            [ctx _ l r] (lcm l r))
(defmethod melder "exclusiveMaximum"      [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "exclusiveMinimum"      [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "maxLength"             [ctx _ l r] (min l r))
(defmethod melder "minLength"             [ctx _ l r] (max l r))
(defmethod melder "pattern"               [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "format"                [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "uniqueItems"           [ctx _ l r] (or l r))
(defmethod melder "required"              [ctx _ l r] (vec (union (set l) (set r))))
(defmethod melder "items"                 [ctx _ l r] (first (meld-schemas ctx l r)))
(defmethod melder "additionalItems"       [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "prefixItems"           [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "unevaluatedItems"      [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "maxItems"              [ctx _ l r] (min l r))
(defmethod melder "minItems"              [ctx _ l r] (max l r))
(defmethod melder "contains"              [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "maxProperties"         [ctx _ l r] (min l r))
(defmethod melder "minProperties"         [ctx _ l r] (max l r))
(defmethod melder "properties"            [ctx _ l r] (first (meld-properties ctx l r)))
(defmethod melder "patternProperties"     [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "additionalProperties"  [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "unevaluatedProperties" [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "dependencies"          [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "propertyNames"         [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "dependentSchemas"      [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "propertyDependencies"  [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "dependentRequired"     [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "$ref"                  [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "anyOf"                 [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "oneOf"                 [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "allOf"                 [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "not"                   [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "if"                    [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "then"                  [ctx _ l r] (do (log/error "NYI") nil))
(defmethod melder "else"                  [ctx _ l r] (do (log/error "NYI") nil))
