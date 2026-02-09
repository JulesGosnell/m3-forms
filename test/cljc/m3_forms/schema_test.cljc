(ns m3-forms.schema-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [m3-forms.schema :as schema]
   [m3-forms.divorce :as divorce]
   [m3-forms.final-terms :as ft]))

;;------------------------------------------------------------------------------
;; resolve-json-pointer

(deftest resolve-json-pointer-basic
  (testing "empty pointer returns root"
    (is (= {"a" 1} (schema/resolve-json-pointer {"a" 1} ""))))
  (testing "nil pointer returns root"
    (is (= {"a" 1} (schema/resolve-json-pointer {"a" 1} nil))))
  (testing "single-level pointer"
    (is (= 1 (schema/resolve-json-pointer {"a" 1} "/a"))))
  (testing "multi-level pointer"
    (is (= 42 (schema/resolve-json-pointer {"a" {"b" {"c" 42}}} "/a/b/c"))))
  (testing "missing key returns nil"
    (is (nil? (schema/resolve-json-pointer {"a" 1} "/b")))))

(deftest resolve-json-pointer-tilde-escaping
  (testing "~0 decodes to ~"
    (is (= "found" (schema/resolve-json-pointer {"a~b" "found"} "/a~0b"))))
  (testing "~1 decodes to /"
    (is (= "found" (schema/resolve-json-pointer {"a/b" "found"} "/a~1b"))))
  (testing "combined ~0 and ~1"
    (is (= "ok" (schema/resolve-json-pointer {"a~/b" "ok"} "/a~0~1b")))))

(deftest resolve-json-pointer-divorce-defs
  (testing "resolve Address $def from divorce schema"
    (let [result (schema/resolve-json-pointer divorce/divorce-m2 "/$defs/Address")]
      (is (= "Address" (get result "title")))
      (is (= "array" (get result "type")))))
  (testing "resolve PropertyAsset $def"
    (let [result (schema/resolve-json-pointer divorce/divorce-m2 "/$defs/PropertyAsset")]
      (is (= "Property" (get result "title")))
      (is (contains? (get result "properties") "value")))))

;;------------------------------------------------------------------------------
;; expand-$ref

(deftest expand-ref-simple
  (testing "no $ref returns schema unchanged"
    (let [schema {"type" "string"}
          ctx {:root {"$defs" {}}}]
      (is (= schema (schema/expand-$ref ctx [] schema)))))
  (testing "resolves simple $ref"
    (let [root {"$defs" {"Foo" {"type" "object" "title" "Foo"}}}
          ctx {:root root}
          schema {"$ref" "#/$defs/Foo"}
          result (schema/expand-$ref ctx [] schema)]
      (is (= "object" (get result "type")))
      (is (= "Foo" (get result "title")))
      (is (nil? (get result "$ref"))))))

(deftest expand-ref-property-override
  (testing "local properties override $ref'd properties"
    (let [root {"$defs" {"Base" {"type" "object" "title" "Base" "properties" {"a" {"type" "string"}}}}}
          ctx {:root root}
          schema {"$ref" "#/$defs/Base" "title" "Override"}
          result (schema/expand-$ref ctx [] schema)]
      (is (= "Override" (get result "title"))))))

(deftest expand-ref-deep-merge-properties
  (testing "properties are deep-merged, not replaced"
    (let [root {"$defs" {"Base" {"type" "object"
                                  "properties" {"a" {"type" "string"}
                                                "b" {"type" "number"}}}}}
          ctx {:root root}
          schema {"$ref" "#/$defs/Base"
                  "properties" {"b" {"type" "integer"}
                                "c" {"type" "boolean"}}}
          result (schema/expand-$ref ctx [] schema)]
      ;; $ref property "a" preserved
      (is (= {"type" "string"} (get-in result ["properties" "a"])))
      ;; local "b" overrides
      (is (= {"type" "integer"} (get-in result ["properties" "b"])))
      ;; local "c" added
      (is (= {"type" "boolean"} (get-in result ["properties" "c"]))))))

(deftest expand-ref-non-map-schema
  (testing "non-map schema returned as-is"
    (is (= true (schema/expand-$ref {:root {}} [] true)))
    (is (= false (schema/expand-$ref {:root {}} [] false)))
    (is (= nil (schema/expand-$ref {:root {}} [] nil)))))

(deftest expand-ref-divorce-real-refs
  (testing "expand Address $ref from divorce schema"
    (let [ctx {:root divorce/divorce-m2}
          schema {"$ref" "#/$defs/Address"}
          result (schema/expand-$ref ctx [] schema)]
      (is (= "Address" (get result "title")))
      (is (= "array" (get result "type"))))))

;;------------------------------------------------------------------------------
;; check-schema — type validation

(defn- run-check [m2-doc m1]
  (let [ctx {:root {}}
        validator (schema/check-schema ctx [] m2-doc)]
    (validator {} [] m1)))

(deftest check-schema-type-object
  (testing "object type — valid"
    (is (nil? (run-check {"type" "object"} {"a" 1}))))
  (testing "object type — invalid"
    (is (some? (run-check {"type" "object"} "not-an-object")))))

(deftest check-schema-type-array
  (testing "array type — valid vector"
    (is (nil? (run-check {"type" "array"} [1 2 3]))))
  (testing "array type — invalid"
    (is (some? (run-check {"type" "array"} "not-array")))))

(deftest check-schema-type-string
  (testing "string type — valid"
    (is (nil? (run-check {"type" "string"} "hello"))))
  (testing "string type — invalid"
    (is (some? (run-check {"type" "string"} 42)))))

(deftest check-schema-type-number
  (testing "number type — valid"
    (is (nil? (run-check {"type" "number"} 3.14))))
  (testing "number type — invalid"
    (is (some? (run-check {"type" "number"} "not-number")))))

(deftest check-schema-type-integer
  (testing "integer type — valid"
    (is (nil? (run-check {"type" "integer"} 42))))
  (testing "integer type — invalid"
    (is (some? (run-check {"type" "integer"} 3.14)))))

(deftest check-schema-type-boolean
  (testing "boolean type — valid"
    (is (nil? (run-check {"type" "boolean"} true))))
  (testing "boolean type — invalid"
    (is (some? (run-check {"type" "boolean"} "true")))))

(deftest check-schema-type-null
  (testing "null type — valid"
    (is (nil? (run-check {"type" "null"} nil))))
  (testing "null type — invalid with nil m1"
    ;; when m1 is nil the whole check-schema returns nil (guard clause)
    (is (nil? (run-check {"type" "string"} nil)))))

;;------------------------------------------------------------------------------
;; check-schema — const validation

(deftest check-schema-const
  (testing "const match — no error"
    (is (nil? (run-check {"type" "object"
                          "properties" {"k" {"const" "yes"}}}
                         {"k" "yes"}))))
  (testing "const mismatch — error"
    (let [errors (run-check {"type" "object"
                             "properties" {"k" {"const" "yes"}}}
                            {"k" "no"})]
      (is (some? errors))
      (is (some #(= "const" (:error %)) errors))))
  (testing "const — absent property is not an error"
    (is (nil? (run-check {"type" "object"
                          "properties" {"k" {"const" "yes"}}}
                         {})))))

;;------------------------------------------------------------------------------
;; check-schema — required validation

(deftest check-schema-required
  (testing "required property present — no error"
    (is (nil? (run-check {"type" "object"
                          "required" ["a"]
                          "properties" {"a" {"type" "string"}}}
                         {"a" "hi"}))))
  (testing "required property missing — error"
    (let [errors (run-check {"type" "object"
                             "required" ["a"]
                             "properties" {"a" {"type" "string"}}}
                            {})]
      (is (some? errors))
      (is (some #(= "required" (:error %)) errors))))
  (testing "all required missing — multiple errors"
    (let [errors (run-check {"type" "object"
                             "required" ["a" "b" "c"]
                             "properties" {"a" {"type" "string"}
                                           "b" {"type" "string"}
                                           "c" {"type" "string"}}}
                            {})]
      (is (= 3 (count (filter #(= "required" (:error %)) errors)))))))

;;------------------------------------------------------------------------------
;; check-schema — additionalProperties

(deftest check-schema-additional-properties
  (testing "no additional properties — no error"
    (is (nil? (run-check {"type" "object"
                          "properties" {"a" {"type" "string"}}
                          "additionalProperties" false}
                         {"a" "ok"}))))
  (testing "extra property — error"
    (let [errors (run-check {"type" "object"
                             "properties" {"a" {"type" "string"}}
                             "additionalProperties" false}
                            {"a" "ok" "b" "extra"})]
      (is (some? errors))
      (is (some #(= "additionalProperties" (:error %)) errors)))))

;;------------------------------------------------------------------------------
;; check-schema — property-level type discrimination

(deftest check-schema-property-type
  (testing "string vs array discrimination (Boolean vs TypeArray fix)"
    ;; A schema requiring property "type" to be a string
    (let [string-schema {"type" "object"
                         "properties" {"type" {"type" "string"}}}
          ;; A schema requiring property "type" to be an array
          array-schema {"type" "object"
                        "properties" {"type" {"type" "array"}}}]
      ;; m1 with string "type" value
      (is (nil? (run-check string-schema {"type" "boolean"})))
      (is (some? (run-check array-schema {"type" "boolean"})))
      ;; m1 with array "type" value
      (is (some? (run-check string-schema {"type" ["string" "null"]})))
      (is (nil? (run-check array-schema {"type" ["string" "null"]}))))))

;;------------------------------------------------------------------------------
;; check-schema — real-world oneOf discrimination (Final Terms payoff)

(deftest check-schema-final-terms-payoff-oneof
  (let [payoff-schema (get-in ft/final-terms-m2 ["properties" "payoff"])
        branches (get payoff-schema "oneOf")
        fixed-branch (nth branches 0)
        floating-branch (nth branches 1)
        zero-branch (nth branches 2)
        index-branch (nth branches 3)]

    (testing "Fixed Rate Notes — matches only fixed branch"
      (let [m1 {"type" "Fixed Rate Notes"
                "commercial_name" "Test"
                "indices" ["IDX1"]}]
        (is (nil? (run-check fixed-branch m1)))
        ;; const mismatch on other branches
        (is (some? (run-check floating-branch m1)))
        (is (some? (run-check zero-branch m1)))
        (is (some? (run-check index-branch m1)))))

    (testing "Index Linked Notes — matches only index branch"
      (let [m1 {"type" "Index Linked Notes"
                "commercial_name" "Test"
                "index" "IDX1"}]
        (is (some? (run-check fixed-branch m1)))
        (is (some? (run-check floating-branch m1)))
        (is (some? (run-check zero-branch m1)))
        (is (nil? (run-check index-branch m1)))))))

;;------------------------------------------------------------------------------
;; make-m3 — smoke tests

(deftest make-m3-structure
  (testing "draft2021-12 produces $defs"
    (let [m3 (schema/make-m3 {:draft "draft2021-12"})]
      (is (map? m3))
      (is (contains? m3 "$defs"))
      (is (not (contains? m3 "definitions")))
      (is (contains? (get m3 "$defs") "schemaM3"))
      (is (contains? (get m3 "$defs") "objectM3"))
      (is (contains? (get m3 "$defs") "commonPropertiesM3"))))

  (testing "draft4 produces definitions (not $defs)"
    (let [m3 (schema/make-m3 {:draft "draft4"})]
      (is (contains? m3 "definitions"))
      (is (not (contains? m3 "$defs")))))

  (testing "latest draft includes all format values"
    (let [m3 (schema/make-m3 {:draft "latest"})
          defs (get m3 "$defs")
          common (get defs "commonPropertiesM3")
          format-enum (get-in common ["properties" "format" "enum"])]
      ;; Should include both standard and custom formats
      (is (some #(= "date-time" %) format-enum))
      (is (some #(= "date" %) format-enum))
      (is (some #(= "money" %) format-enum))
      (is (some #(= "telephone-number" %) format-enum))))

  (testing "draft4 format enum lacks draft7 formats"
    (let [m3 (schema/make-m3 {:draft "draft4"})
          defs (get m3 "definitions")
          common (get defs "commonPropertiesM3")
          format-enum (get-in common ["properties" "format" "enum"])]
      (is (some #(= "date-time" %) format-enum))
      ;; "date" is draft7 only
      (is (not (some #(= "date" %) format-enum))))))
