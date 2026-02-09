(ns m3-forms.workflow
  [:require
   [m3-forms.util :refer [index-by-$id]]
   [m3-forms.bind :refer [bindings-m2]]])

(def workflow-m2
  {"$id" "/schemas/workflow"
   "$schema" "/schemas/metaschema"
   "$defs"
   {"Principal"
    {"type" "string"}
    "View"
    {"type" "object"
     "properties"
     {"$id" {"type" "string"}
      "schema" {"type" "null"}
      "principals"
      {"type" "array"
       "items" {"$ref" "#/$defs/Principal"}
       "minItems" 1}}
     "required" ["principals"]}
    "Transition"
    {"type" "object"
     "properties"
     {"title" {"type" "string"}
      "state" {"type" "string"}}}
    "State"
    {"type" "object"
     "properties"
     {"$id" {"type" "string"}
      "views"
      {"type" "array"
       "items" {"$ref" "/schemas/bindings#/$defs/Path"}
       "minItems" 1}
      "transitions"
      {"type" "array"
       "items" {"$ref" "#/$defs/Transition"}}}
     "required" ["views"]}}
   "type" "object"
   "properties"
   {"states"
    {"type" "array"
     "items" {"$ref" "#/$defs/State"}
     "minItems" 2}}
   "required" ["states"]})
