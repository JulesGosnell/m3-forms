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

(ns m3-forms.migrate
  (:require
   [m3-forms.schema :refer [validate]]
   [m3-forms.json :refer [json-rename-in]]))

(def migration-m2
  {"oneOf"
   [{"type" "object"
     "properties"
     {"type" {"type" "string" "const" "rename"}
      "m2-path" {"type" "array" "items" {"type" "string"}}
      "m1-path" {"type" "array" "items" {"type" "string"}}
      "source" {"type" "string"}
      "target" {"type" "string"}}
     "required" ["type" "m2-path" "m1-path" "source" "target"]}
    {"type" "object"
     "properties"
     {"type" {"type" "string" "const" "move"}}}
    {"type" "object"
     "properties"
     {"type" {"type" "string" "const" "insert"}}}
    {"type" "object"
     "properties"
     {"type" {"type" "string" "const" "append"}}}
    {"type" "object"
     "properties"
     {"type" {"type" "string" "const" "delete"}
      "m2-path" {"type" "array" "items" {"type" "string"}}
      "m1-path" {"type" "array" "items" {"type" "string"}}}
     "required" ["type" "m2-path" "m1-path"]}]})

(defmulti migrate
  (fn [m2-ctx m1-ctx {t "type" :as migration-m1} [m2 m1s]]
    (print "MIGRATE:" migration-m1)
    ;; TODO: re-enable validation when validator is integrated
    ;; (let [v (validate m2-ctx migration-m2 m1-ctx migration-m1)]
    ;;   (when-not (:valid? v)
    ;;     (throw (ex-info "invalid migration document" v))))
    t))

(defmethod migrate :default [_m2-ctx _m1-ctx migration [m2 m1s]]
  (println "unrecognised migration:" migration))

(defmethod migrate "rename" [_m2-ctx _m1-ctx {p2 "m2-path" p1 "m1-path" s "source" t "target"} [m2 m1s]]
  [(json-rename-in m2 p2 s t)
   (mapv (fn [m1] (json-rename-in m1 p1 s t)) m1s)])

(defmethod migrate "move" [_m2-ctx _m1-ctx migration [m2 m1s]])
(defmethod migrate "insert" [_m2-ctx _m1-ctx migration [m2 m1s]])
(defmethod migrate "append" [_m2-ctx _m1-ctx migration [m2 m1s]])

(defn dissoc-in [m p]
  (let [h (butlast p)
        t (last p)]
    (if h
      (update-in m h dissoc t)
      (dissoc m t))))

(defmethod migrate "delete" [_m2-ctx _m1-ctx {p2 "m2-path" p1 "m1-path"} [m2 m1s]]
  [(dissoc-in m2 p2)
   (mapv (fn [m1] (dissoc-in m1 p1)) m1s)])
