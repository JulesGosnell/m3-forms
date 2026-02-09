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
   [clojure.string :as str]
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

;;------------------------------------------------------------------------------
;; mdast $document migration — update field paths in embedded document trees

(defn- path-matches?
  "Does path start with prefix (dot-separated)?"
  [path prefix]
  (or (= path prefix)
      (str/starts-with? path (str prefix "."))))

(defn- rename-path
  "If path starts with old-prefix, replace that prefix with new-prefix."
  [path old-prefix new-prefix]
  (cond
    (= path old-prefix)
    new-prefix

    (str/starts-with? path (str old-prefix "."))
    (str new-prefix (subs path (count old-prefix)))

    :else path))

(defn- mdast-walk-rename
  "Recursively walk an mdast node, renaming paths in fieldReference, each, conditional."
  [node old-prefix new-prefix]
  (when (map? node)
    (let [node-type (get node "type")]
      (case node-type
        "fieldReference"
        (update node "path" rename-path old-prefix new-prefix)

        "each"
        (-> node
            (update "path" rename-path old-prefix new-prefix)
            (update "children" (fn [cs] (mapv #(mdast-walk-rename % old-prefix new-prefix) cs))))

        "conditional"
        (-> node
            (update "path" rename-path old-prefix new-prefix)
            (update "cases" (fn [cases]
                              (reduce-kv (fn [acc k children]
                                           (assoc acc k (mapv #(mdast-walk-rename % old-prefix new-prefix) children)))
                                         {} cases))))

        ;; Default: recurse into children
        (if (contains? node "children")
          (update node "children" (fn [cs] (mapv #(mdast-walk-rename % old-prefix new-prefix) cs)))
          node)))))

(defn- mdast-walk-delete
  "Recursively walk an mdast node, removing fieldReference nodes that reference deleted-path."
  [node deleted-path]
  (when (map? node)
    (let [node-type (get node "type")]
      (case node-type
        "fieldReference"
        (when-not (path-matches? (get node "path") deleted-path)
          node)

        "each"
        (if (path-matches? (get node "path") deleted-path)
          nil
          (update node "children"
                  (fn [cs] (vec (keep #(mdast-walk-delete % deleted-path) cs)))))

        "conditional"
        (if (path-matches? (get node "path") deleted-path)
          nil
          (update node "cases"
                  (fn [cases]
                    (reduce-kv (fn [acc k children]
                                 (assoc acc k (vec (keep #(mdast-walk-delete % deleted-path) children))))
                               {} cases))))

        ;; Default: recurse into children, filtering out nil results
        (if (contains? node "children")
          (update node "children"
                  (fn [cs] (vec (keep #(mdast-walk-delete % deleted-path) cs))))
          node)))))

(defn mdast-rename-path
  "Walk an mdast $document tree in m2, renaming field reference paths.
   old-prefix and new-prefix are dot-separated path strings."
  [m2 old-prefix new-prefix]
  (if-let [doc (get m2 "$document")]
    (assoc m2 "$document" (mdast-walk-rename doc old-prefix new-prefix))
    m2))

(defn mdast-delete-path
  "Walk an mdast $document tree in m2, removing nodes that reference deleted-path."
  [m2 deleted-path]
  (if-let [doc (get m2 "$document")]
    (assoc m2 "$document" (mdast-walk-delete doc deleted-path))
    m2))

;;------------------------------------------------------------------------------

(defmulti migrate
  (fn [m2-ctx m1-ctx {t "type" :as migration-m1} [m2 m1s]]
    (print "MIGRATE:" migration-m1)
    t))

(defmethod migrate :default [_m2-ctx _m1-ctx migration [m2 m1s]]
  (println "unrecognised migration:" migration))

(defmethod migrate "rename" [_m2-ctx _m1-ctx {p2 "m2-path" p1 "m1-path" s "source" t "target"} [m2 m1s]]
  [(-> (json-rename-in m2 p2 s t)
       (mdast-rename-path (str/join "." (conj (vec p1) s))
                          (str/join "." (conj (vec p1) t))))
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
  [(-> (dissoc-in m2 p2)
       (mdast-delete-path (str/join "." p1)))
   (mapv (fn [m1] (dissoc-in m1 p1)) m1s)])
