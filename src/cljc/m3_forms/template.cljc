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

(ns m3-forms.template
  (:require [clojure.string :as str]))

(defn render-placeholder [template context]
  (if (map? context)
    (reduce-kv (fn [acc k v] (str/replace acc (str "{{" k "}}") (str v))) template context)
    (str/replace template (str "{{.}}") context)))

(defn render-if [content context]
  (if context content ""))

(declare render-template)

(defn render-each [content context]
  (apply str (map #(render-template content %) context)))

(defn render-tag [tag content context]
  (cond
    (= tag "if") (render-if content context)
    (= tag "each") (render-each content context)
    :else content))

(defn render-blocks [template context]
  (let [pattern #"\{\{\#(.*?)\s+(.*?)\}\}([\s\S]*?)\{\{\/\1\}\}"
        matches (re-seq pattern template)]
    (reduce (fn [acc match]
              (let [[_ tag variable content] match
                    new-content (render-tag tag content (get context variable))]
                (str/replace acc (re-pattern (str "\\{\\{\\#" tag "\\s+" variable "\\}\\}([\\s\\S]*?)\\{\\{\\/" tag "\\}\\}"))
                            new-content)))
            template
            matches)))

(defn render-template [template context]
  (-> template
      (render-blocks context)
      (render-placeholder context)))
