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

(ns m3-forms.schema
  "Adapter layer providing schema operations for the forms UI.
  Bridges the old m3.validate API that the forms code expects
  with standalone implementations (and eventually the m3 validator)."
  (:require
   [clojure.string :as str]
   [m3-forms.mdast-schema :as mdast-schema]
   [m3-forms.visit :refer [maybe-update visit-schema]]))

;;------------------------------------------------------------------------------
;; Helpers

(defn concatv [& args]
  (vec (apply concat args)))

;;------------------------------------------------------------------------------
;; $ref resolution — simple JSON Pointer resolver for local $ref

(defn resolve-json-pointer
  "Resolve a JSON Pointer (RFC 6901) within a root document.
  pointer should be like '/properties/foo/items'."
  [root pointer]
  (if (or (nil? pointer) (= "" pointer))
    root
    (let [parts (->> (str/split pointer #"/")
                     rest ;; drop empty string before first /
                     (map #(-> %
                               (str/replace "~1" "/")
                               (str/replace "~0" "~"))))]
      (get-in root parts))))

(defn expand-$ref
  "Resolve $ref in a schema against the root schema in ctx.
  ctx must contain :root (the root schema/document).
  Returns the resolved schema (with $ref removed and referenced properties merged).
  Deep-merges 'properties' so that $ref'd common properties aren't lost.
  Resolves chained $refs (e.g. objectM3 -> commonPropertiesM3) with loop/seen guard."
  [{:keys [root]} _path schema]
  (if (or (not (map? schema)) (nil? root))
    schema
    (loop [schema schema seen #{}]
      (if-let [ref (get schema "$ref")]
        (if (or (not (string? ref)) (contains? seen ref))
          (dissoc schema "$ref") ;; break circular $ref or non-string ref
          (let [[_base fragment] (str/split ref #"#" 2)]
            (if fragment
              (if-let [resolved (resolve-json-pointer root fragment)]
                (let [schema' (dissoc schema "$ref")
                      merged (merge resolved schema')
                      ;; Deep-merge "properties" so $ref'd properties aren't overridden
                      merged (if (and (get resolved "properties") (get schema' "properties"))
                               (assoc merged "properties"
                                      (merge (get resolved "properties")
                                             (get schema' "properties")))
                               merged)]
                  (if (get merged "$ref")
                    (recur merged (conj seen ref))
                    merged))
                schema) ;; unresolved $ref — return schema as-is
              ;; No fragment — break out
              schema)))
        ;; No $ref — done
        schema))))

;;------------------------------------------------------------------------------
;; Schema validation — stubbed for now, will integrate with m3 validator later

(defn check-schema
  "Compile a schema into a validator function.
  Returns (fn [m1-ctx m1-path m1] -> errors-or-nil).
  Implements validation: top-level type check, property const + type matching,
  required, and additionalProperties:false — enough to discriminate oneOf branches."
  [m2-ctx _m2-path m2-doc]
  (let [expanded (expand-$ref m2-ctx _m2-path m2-doc)]
    (fn [_m1-ctx _m1-path m1]
      (when (and (map? expanded) (some? m1))
        (let [errors
              (concat
               ;; Top-level type check
               (when-let [t (get expanded "type")]
                 (when (string? t)
                   (let [ok? (case t
                               "object"  (map? m1)
                               "array"   (or (vector? m1) (sequential? m1))
                               "string"  (string? m1)
                               "number"  (number? m1)
                               "integer" (integer? m1)
                               "boolean" (boolean? m1)
                               "null"    (nil? m1)
                               true)]
                     (when-not ok? [{:error "type" :expected t}]))))
               ;; Property const matching — only fail if property exists AND mismatches
               (when-let [props (get expanded "properties")]
                 (when (map? m1)
                   (keep (fn [[k prop-schema]]
                           (when (map? prop-schema)
                             (let [v (get m1 k ::not-found)]
                               (when (not= v ::not-found)
                                 (or
                                  ;; const check
                                  (when-let [c (get prop-schema "const")]
                                    (when (not= v c)
                                      {:error "const" :property k :expected c :actual v}))
                                  ;; property-level type check
                                  (when-let [pt (get prop-schema "type")]
                                    (when (string? pt)
                                      (let [ok? (case pt
                                                  "object"  (map? v)
                                                  "array"   (or (vector? v) (sequential? v))
                                                  "string"  (string? v)
                                                  "number"  (number? v)
                                                  "integer" (integer? v)
                                                  "boolean" (boolean? v)
                                                  "null"    (nil? v)
                                                  true)]
                                        (when-not ok?
                                          {:error "property-type" :property k :expected pt})))))))))
                         props)))
               ;; required — fail if any required property is missing from m1
               (when-let [req (get expanded "required")]
                 (when (map? m1)
                   (keep (fn [k]
                           (when-not (contains? m1 k)
                             {:error "required" :property k}))
                         req)))
               ;; additionalProperties: false — data keys must be in schema properties
               (when (and (false? (get expanded "additionalProperties"))
                          (map? m1))
                 (let [allowed (set (keys (get expanded "properties")))]
                   (keep (fn [k]
                           (when-not (contains? allowed k)
                             {:error "additionalProperties" :property k}))
                         (keys m1)))))]
          (when (seq errors) (vec errors)))))))

(defn validate
  "Validate a document against a schema.
  Returns {:valid? bool :errors [...]}.
  Currently stubbed."
  ([_m2-ctx _schema _m1-ctx _document]
   ;; TODO: integrate with m3.json-schema/validate
   {:valid? true :errors nil})
  ([_m2-ctx _schema]
   ;; Returns a validator function
   (fn [_m1-ctx _document]
     {:valid? true :errors nil})))

;;------------------------------------------------------------------------------
;; externalise-$refs — rewrites local $refs to include a prefix

(defn- externalise-$ref-2 [prefix ref]
  (if (= \# (first ref))
    (str prefix ref)
    ref))

(defn- externalise-$ref [prefix schema]
  (maybe-update schema "$ref" (partial externalise-$ref-2 prefix)))

(defn externalise-$refs [prefix schema]
  (visit-schema
   (partial externalise-$ref prefix)
   schema))

;;------------------------------------------------------------------------------
;; make-m3 — generate the M3 meta-schema that constrains JSON Schemas
;; for form generation

(def draft->draft?
  {"draft3"       #{"draft3"}
   "draft4"       #{"draft3" "draft4"}
   "draft6"       #{"draft3" "draft4" "draft6"}
   "draft7"       #{"draft3" "draft4" "draft6" "draft7"}
   "draft2019-09" #{"draft3" "draft4" "draft6" "draft7" "draft2019-09"}
   "draft2020-12" #{"draft3" "draft4" "draft6" "draft7" "draft2019-09" "draft2020-12"}
   "draft2021-12" #{"draft3" "draft4" "draft6" "draft7" "draft2019-09" "draft2020-12" "draft2021-12"}
   "latest"       #{"draft3" "draft4" "draft6" "draft7" "draft2019-09" "draft2020-12" "draft2021-12"}
   "draft-next"   #{"draft3" "draft4" "draft6" "draft7" "draft2019-09" "draft2020-12" "draft2021-12" "draft-next"}})

(def initial-chars "a-zA-Z _\\-$")
(def subsequent-chars (str "0-9" initial-chars))
(def json-property-name-pattern
  (str "^" "[" initial-chars "]{1}[" subsequent-chars "]*$"))

(defn make-m3 [{draft :draft}]
  (let [draft? (draft->draft? draft)
        defs (cond (draft? "draft2021-12") "$defs" (draft? "draft4") "definitions")
        ->def (fn [p] (str "#/" defs "/" p))

        formats
        (concatv
         (when (draft? "draft3") [])
         (when (draft? "draft4")
           ["date-time" "email" "hostname" "ipv4" "ipv6" "uri"])
         (when (draft? "draft6")
           ["json-pointer" "uri-reference" "uri-template"])
         (when (draft? "draft7")
           ["time" "date" "idn-email" "idn-hostname" "iri" "iri-reference"
            "relative-json-pointer" "regex"])
         (when (draft? "draft2019-09")
           ["duration" "uuid"])
         (when (draft? "draft2020-12") [])
         (when (draft? "draft2021-12") [])
         ["unknown" "year-month" "range" "money"
          "bank-account-number" "bank-sort-code" "telephone-number"
          "mdast-ref"])

        common-properties-m3
        {"type" "object"
         "properties"
         (apply
          array-map
          (concatv
           (when (draft? "draft4")
             ["$schema" {"type" "string"}
              "id" {"type" "string"}
              "$ref" {"type" "string"}
              "title" {"type" "string"}
              "description" {"type" "string"}
              "format" {"type" "string" "enum" formats}])
           (when (draft? "draft6")
             ["$id" {"type" "string"}])
           (when (draft? "draft7")
             ["$comment" {"type" "string"}
              "readOnly" {"type" "boolean"}
              "writeOnly" {"type" "boolean"}])
           (when (draft? "draft2019-09")
             ["deprecated" {"type" "boolean"}
              "$anchor" {"type" "string"}
              "$recursiveAnchor" {"type" "boolean" "const" true "default" false}
              "$recursiveRef" {"type" "string"}])
           (when (draft? "draft2019-09")
             ["$vocabulary" {"type" "object"
                             "patternProperties"
                             {"$format:uri" {"type" "boolean"}}}])
           (when (draft? "draft2021-12")
             ["$defs" {"type" "object"
                       "additionalProperties"
                       {"$ref" (->def "schemaM3")}}])
           ;; M0 document model — mdast tree embedded in M2
           ["$document" {"$ref" (->def "mdastRoot")}]))}

        one-of-m3
        {"title" "OneOf"
         "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         {"oneOf" {"type" "array" "items" {"$ref" (->def "schemaM3")}}}
         "required" ["oneOf"]
         "additionalProperties" false}

        any-of-m3
        {"title" "AnyOf"
         "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         {"anyOf" {"type" "array" "items" {"$ref" (->def "schemaM3")}}}
         "required" ["anyOf"]
         "additionalProperties" false}

        all-of-m3
        {"title" "AllOf"
         "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         {"allOf" {"type" "array" "items" {"$ref" (->def "schemaM3")}}}
         "required" ["allOf"]
         "additionalProperties" false}

        not-m3
        {"title" "Not"
         "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         {"not" {"$ref" (->def "schemaM3")}}
         "required" ["not"]
         "additionalProperties" false}

        typed-properties-m3
        (fn [t]
          (concatv
           (when (draft? "draft4")
             ["type" {"type" "string" "const" t}
              "default" {"type" t}
              "enum" {"type" "array" "items" {"type" t}}])
           (when (draft? "draft6")
             ["const" {"type" t}
              "examples" {"type" "array" "items" {"type" t}}])))

        null-m3
        {"title" "Null" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties" (apply array-map (typed-properties-m3 "null"))
         "required" ["type"]
         "additionalProperties" false}

        boolean-m3
        {"title" "Boolean" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties" (apply array-map (typed-properties-m3 "boolean"))
         "required" ["type"]
         "additionalProperties" false}

        numeric-properties
        (fn [t]
          (concatv
           (typed-properties-m3 t)
           (when (draft? "draft4")
             ["minimum" {"type" t}
              "maximum" {"type" t}
              "multipleOf" {"type" t "minimum" 0 "exclusiveMinimum" true}
              "exclusiveMinimum" {"type" "boolean"}
              "exclusiveMaximum" {"type" "boolean"}])
           (when (draft? "draft6")
             ["multipleOf" {"type" t "exclusiveMinimum" 0}
              "exclusiveMinimum" {"type" t}
              "exclusiveMaximum" {"type" t}])))

        number-m3
        {"title" "Number" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties" (apply array-map (numeric-properties "number"))
         "required" ["type"]
         "additionalProperties" false}

        integer-m3
        {"title" "Integer" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties" (apply array-map (numeric-properties "integer"))
         "required" ["type"]
         "additionalProperties" false}

        string-m3
        {"title" "String" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         (apply
          array-map
          (concat
           (typed-properties-m3 "string")
           (when (draft? "draft4")
             ["minLength" {"type" "integer"}
              "maxLength" {"type" "integer"}
              "pattern" {"type" "string" "format" "regex"}])
           (when (draft? "draft7")
             ["contentMediaType" {"type" "string" "enum" ["application/json"]}
              "contentEncoding" {"type" "string" "enum" ["quoted-printable" "base16" "base32" "base64"]}
              "contentSchema" {"$ref" (->def "schemaM3")}])))
         "required" ["type"]
         "additionalProperties" false}

        object-m3
        {"title" "Object" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         (apply
          array-map
          (concat
           (typed-properties-m3 "object")
           (when (draft? "draft4")
             ["definitions" {"type" "object" "additionalProperties" {"$ref" (->def "schemaM3")}}
              "properties" {"type" "object" "additionalProperties" {"$ref" (->def "schemaM3") "title" "A Type"}}
              "additionalProperties" {"$ref" (->def "schemaM3")}
              "patternProperties" {"type" "object" "patternProperties" {"$format:regex" {"$ref" (->def "schemaM3")}}}
              "minProperties" {"type" "integer" "minimum" 0 "default" 0}
              "maxProperties" {"type" "integer" "minimum" 0}
              "required" {"type" "array" "items" {"type" "string" "pattern" json-property-name-pattern} "uniqueItems" true}
              "dependencies" {"type" "object" "description" "property dependencies"
                              "additionalProperties"
                              {"oneOf"
                               [{"type" "array" "items" {"type" "string" "pattern" json-property-name-pattern} "uniqueItems" true}
                                {"$ref" (->def "schemaM3")}]}}])
           (when (draft? "draft6")
             ["propertyNames"
              {"oneOf"
               [{"$ref" (->def "stringM3")}
                {"$ref" (->def "booleanM2")}]}])
           (when (draft? "draft7")
             ["if" {"$ref" (->def "schemaM3")}
              "then" {"$ref" (->def "schemaM3")}
              "else" {"$ref" (->def "schemaM3")}])
           (when (draft? "draft2019-09")
             ["dependentRequired" {"type" "object" "additionalProperties" {"type" "array" "items" {"type" "string" "pattern" json-property-name-pattern}}}
              "dependentSchemas" {"type" "object" "additionalProperties" {"$ref" (->def "schemaM3")}}
              "propertyDependencies" {"type" "object" "additionalProperties" {"type" "object" "additionalProperties" {"$ref" (->def "schemaM3")}}}
              "unevaluatedProperties" false])))
         "required" ["type"]
         "additionalProperties" false}

        array-m3
        {"title" "Array" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties"
         (apply
          array-map
          (concat
           (typed-properties-m3 "array")
           (when (draft? "draft4")
             ["items" {"oneOf" [{"type" "array" "items" {"$ref" (->def "schemaM3")}} {"$ref" (->def "schemaM3")}]}
              "minItems" {"type" "integer" "minimum" 0 "default" 0}
              "maxItems" {"type" "integer" "minimum" 0}
              "uniqueItems" {"type" "boolean"}
              "additionalItems" {"oneOf" [{"$ref" (->def "booleanM2")} {"$ref" (->def "schemaM3")}]}])
           (when (draft? "draft6")
             ["contains" {"$ref" (->def "schemaM3")}])
           (when (draft? "draft2019-09")
             ["items" {"description" "The remainder of the array described as a homogeneous list" "$ref" (->def "schemaM3")}
              "prefixItems" {"type" "array" "description" "The first part of the array described as a heterogeneous tuple." "items" {"$ref" (->def "schemaM3")}}
              "additionalItems" false
              "unevaluatedItems" {"oneOf" [{"$ref" (->def "booleanM2")} {"$ref" (->def "schemaM3")}]}
              "minContains" {"type" "integer" "minimum" 0}
              "maxContains" {"type" "integer" "minimum" 1}])))
         "required" ["type"]
         "additionalProperties" false}

        type-array-m3
        {"title" "Type Array" "type" "object"
         "$ref" (->def "commonPropertiesM3")
         "properties" {"type" {"type" "array" "items" {"type" "string"}}}
         "required" ["type"]
         "additionalProperties" false}

        boolean-m2
        {"title" "True/False" "type" "boolean"}

        schema-m3
        {"oneOf"
         [{"title" "Untyped"
           "$ref" (->def "commonPropertiesM3")
           "additionalProperties" false}
          {"$ref" (->def "nullM3")}
          {"$ref" (->def "booleanM3")}
          {"$ref" (->def "numberM3")}
          {"$ref" (->def "integerM3")}
          {"$ref" (->def "stringM3")}
          {"$ref" (->def "objectM3")}
          {"$ref" (->def "arrayM3")}
          {"$ref" (->def "oneOfM3")}
          {"$ref" (->def "anyOfM3")}
          {"$ref" (->def "allOfM3")}
          {"$ref" (->def "notM3")}
          {"$ref" (->def "typeArrayM3")}
          {"$ref" (->def "booleanM2")}]}

        definitions-m3
        (merge
         (array-map
          "commonPropertiesM3" common-properties-m3
          "nullM3" null-m3
          "booleanM3" boolean-m3
          "numberM3" number-m3
          "integerM3" integer-m3
          "stringM3" string-m3
          "objectM3" object-m3
          "arrayM3" array-m3
          "oneOfM3" one-of-m3
          "anyOfM3" any-of-m3
          "allOfM3" all-of-m3
          "notM3" not-m3
          "typeArrayM3" type-array-m3
          "booleanM2" boolean-m2
          "schemaM3" schema-m3)
         (mdast-schema/all-mdast-defs ->def))

        m3
        {"$id" "/schemas/metaschema"
         "$schema" "/schemas/metaschema"
         defs definitions-m3
         "title" "MetaSchema"
         "$ref" (->def "schemaM3")}]

    m3))
