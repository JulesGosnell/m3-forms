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
