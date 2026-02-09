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

(ns m3-forms.visit)

(defmulti visit-schema
  "apply a function to every [sub-]schema in a schema returning the resulting new schema"
  (fn [_f {t "type" oo "oneOf" alo "allOf" ano "anyOf" :as _schema}]
    (cond
      t t
      oo "oneOf"
      alo "allOf"
      ano "anyOf")))

(defn visit-schema-collection [f c]
  (reduce-kv
   (fn [acc k old-v]
     (let [new-v (visit-schema f old-v)]
       (if (identical? old-v new-v)
         acc
         (assoc acc k new-v))))
   c
   c))

(defn maybe-update [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(defmethod visit-schema "object" [f o]
  (f (-> o
         (maybe-update "$defs"                (partial visit-schema-collection f))
         (maybe-update "properties"           (partial visit-schema-collection f))
         (maybe-update "additionalProperties" (partial visit-schema f))
         (maybe-update "propertyNames"        (partial visit-schema f))
         (maybe-update "patternProperties"    (partial visit-schema-collection f)))))

(defmethod visit-schema "array" [f a]
  (f (-> a
         (maybe-update "prefixItems" (partial visit-schema-collection f))
         (maybe-update "items"       (partial visit-schema f)))))

(defmethod visit-schema "oneOf" [f oo]
  (f (update oo "oneOf" (partial visit-schema-collection f))))

(defmethod visit-schema "allOf" [f alo]
  (f (update alo "allOf" (partial visit-schema-collection f))))

(defmethod visit-schema "anyOf" [f ano]
  (f (update ano "anyOf" (partial visit-schema-collection f))))

(defmethod visit-schema :default [f v]
  (f v))
