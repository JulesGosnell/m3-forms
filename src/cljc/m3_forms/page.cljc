(ns m3-forms.page
  [:require
   [m3-forms.bind :refer [bind]]])

;;------------------------------------------------------------------------------

(defn page-m2 [id]
  {"$schema" "/schemas/metaschema"
   "$id" (str "/schemas/page-" id)
   "type" "object"
   "properties"
   {"views"
    {"type" "array"
     "prefixItems" []}
    "transitions"
    {"type" "array"
     "items"
     {"type" "object"
      "properties"
      {"title" {"type" "string"}
       "state" {"type" "string"}}}}}})

(defn page-m1 [id]
  {"$schema" (str "/schemas/page-" id)
   "$id" id
   "views" []
   "transitions" []})

(defn ->page [[m2 {id "$id" :as m1}] {ss "states"} state-id]
  (println "PAGE:" id "->" state-id m1)
  (let [{vs "views" ts "transitions"} (some (fn [{id "$id" :as s}] (when (= state-id id) s)) ss)
        bindings (map-indexed (fn [i path] {"src" {"path" path} "tgt" {"path" ["views" i]}}) vs)
        pm1 (page-m1 state-id)
        pm1 (assoc pm1 "transitions" ts)]
    (bind bindings [m2 m1] [(page-m2 state-id) pm1])))
