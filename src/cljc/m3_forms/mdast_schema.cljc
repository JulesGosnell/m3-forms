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

(ns m3-forms.mdast-schema
  "mdast (Markdown Abstract Syntax Tree) JSON Schema definitions.

   Provides schema definitions for standard mdast nodes (per the mdast spec at
   https://github.com/syntax-tree/mdast) plus custom document-model extensions
   (fieldReference, each, conditional) used by M3 Forms.

   The standalone JSON Schema lives at resources/schemas/mdast.schema.json and
   can be submitted to the mdast project. This namespace generates equivalent
   Clojure data parameterised by ->def for embedding in the M3 meta-schema.

   Standard mdast nodes allow additional properties (data, position) per the
   unist spec. The M3-embedded versions use additionalProperties:false since
   our documents don't carry parser metadata and strict validation catches
   typos and unknown properties in the developer pane.")

;;------------------------------------------------------------------------------
;; Standard mdast node defs (faithful to the mdast + GFM spec)
;;
;; Each def uses additionalProperties:false for strict M3 validation.
;; The standalone JSON Schema in resources/ is more permissive (allows
;; data/position per unist).

(defn mdast-standard-defs
  "Returns an array-map of def-name → schema for standard mdast nodes.
   ->def is a function that turns a def name into a $ref string."
  [->def]
  (let [children {"type" "array" "items" {"$ref" (->def "mdastNode")}}]
    (array-map
     ;; Parent nodes
     "mdastRoot"
     {"title" "Root" "type" "object"
      "properties" {"type" {"type" "string" "const" "root"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastHeading"
     {"title" "Heading" "type" "object"
      "properties" {"type" {"type" "string" "const" "heading"}
                    "depth" {"type" "integer" "minimum" 1 "maximum" 6}
                    "children" children}
      "required" ["type" "depth" "children"]
      "additionalProperties" false}

     "mdastParagraph"
     {"title" "Paragraph" "type" "object"
      "properties" {"type" {"type" "string" "const" "paragraph"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastEmphasis"
     {"title" "Emphasis" "type" "object"
      "properties" {"type" {"type" "string" "const" "emphasis"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastStrong"
     {"title" "Strong" "type" "object"
      "properties" {"type" {"type" "string" "const" "strong"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastLink"
     {"title" "Link" "type" "object"
      "properties" {"type" {"type" "string" "const" "link"}
                    "url" {"type" "string"}
                    "title" {"type" "string"}
                    "children" children}
      "required" ["type" "url" "children"]
      "additionalProperties" false}

     "mdastLinkReference"
     {"title" "Link Reference" "type" "object"
      "properties" {"type" {"type" "string" "const" "linkReference"}
                    "identifier" {"type" "string"}
                    "label" {"type" "string"}
                    "referenceType" {"type" "string" "enum" ["shortcut" "collapsed" "full"]}
                    "children" children}
      "required" ["type" "identifier" "referenceType" "children"]
      "additionalProperties" false}

     "mdastBlockquote"
     {"title" "Blockquote" "type" "object"
      "properties" {"type" {"type" "string" "const" "blockquote"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastList"
     {"title" "List" "type" "object"
      "properties" {"type" {"type" "string" "const" "list"}
                    "ordered" {"type" "boolean"}
                    "start" {"type" "integer" "minimum" 0}
                    "spread" {"type" "boolean"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastListItem"
     {"title" "List Item" "type" "object"
      "properties" {"type" {"type" "string" "const" "listItem"}
                    "checked" {"type" "boolean"}
                    "spread" {"type" "boolean"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastTable"
     {"title" "Table" "type" "object"
      "properties" {"type" {"type" "string" "const" "table"}
                    "align" {"type" "array" "items" {"type" "string" "enum" ["left" "right" "center"]}}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastTableRow"
     {"title" "Table Row" "type" "object"
      "properties" {"type" {"type" "string" "const" "tableRow"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     "mdastTableCell"
     {"title" "Table Cell" "type" "object"
      "properties" {"type" {"type" "string" "const" "tableCell"}
                    ;; 'header' is an M3 Forms extension (not in the official spec,
                    ;; where the first tableRow is implicitly the header)
                    "header" {"type" "boolean"}
                    "children" children}
      "required" ["type" "children"]
      "additionalProperties" false}

     ;; Leaf nodes
     "mdastText"
     {"title" "Text" "type" "object"
      "properties" {"type" {"type" "string" "const" "text"}
                    "value" {"type" "string"}}
      "required" ["type" "value"]
      "additionalProperties" false}

     "mdastImage"
     {"title" "Image" "type" "object"
      "properties" {"type" {"type" "string" "const" "image"}
                    "url" {"type" "string"}
                    "title" {"type" "string"}
                    "alt" {"type" "string"}}
      "required" ["type" "url"]
      "additionalProperties" false}

     "mdastImageReference"
     {"title" "Image Reference" "type" "object"
      "properties" {"type" {"type" "string" "const" "imageReference"}
                    "identifier" {"type" "string"}
                    "label" {"type" "string"}
                    "referenceType" {"type" "string" "enum" ["shortcut" "collapsed" "full"]}
                    "alt" {"type" "string"}}
      "required" ["type" "identifier" "referenceType"]
      "additionalProperties" false}

     "mdastCode"
     {"title" "Code Block" "type" "object"
      "properties" {"type" {"type" "string" "const" "code"}
                    "value" {"type" "string"}
                    "lang" {"type" "string"}
                    "meta" {"type" "string"}}
      "required" ["type" "value"]
      "additionalProperties" false}

     "mdastHtml"
     {"title" "HTML" "type" "object"
      "properties" {"type" {"type" "string" "const" "html"}
                    "value" {"type" "string"}}
      "required" ["type" "value"]
      "additionalProperties" false}

     "mdastInlineCode"
     {"title" "Inline Code" "type" "object"
      "properties" {"type" {"type" "string" "const" "inlineCode"}
                    "value" {"type" "string"}}
      "required" ["type" "value"]
      "additionalProperties" false}

     "mdastDefinition"
     {"title" "Definition" "type" "object"
      "properties" {"type" {"type" "string" "const" "definition"}
                    "identifier" {"type" "string"}
                    "label" {"type" "string"}
                    "url" {"type" "string"}
                    "title" {"type" "string"}}
      "required" ["type" "identifier" "url"]
      "additionalProperties" false}

     "mdastThematicBreak"
     {"title" "Thematic Break" "type" "object"
      "properties" {"type" {"type" "string" "const" "thematicBreak"}}
      "required" ["type"]
      "additionalProperties" false}

     "mdastBreak"
     {"title" "Break" "type" "object"
      "properties" {"type" {"type" "string" "const" "break"}}
      "required" ["type"]
      "additionalProperties" false})))

;;------------------------------------------------------------------------------
;; M3 Forms custom document-model extensions
;;
;; These extend the standard mdast node set with domain-specific nodes for
;; reactive document rendering: field references, array iteration, and
;; conditional branches.

(defn mdast-extension-defs
  "Returns an array-map of def-name → schema for M3 Forms custom document nodes.
   ->def is a function that turns a def name into a $ref string."
  [->def]
  (let [children {"type" "array" "items" {"$ref" (->def "mdastNode")}}]
    (array-map
     "mdastFieldReference"
     {"title" "Field Reference" "type" "object"
      "properties" {"type" {"type" "string" "const" "fieldReference"}
                    "path" {"type" "string" "format" "mdast-ref"}
                    "description" {"type" "string"}
                    "schemaType" {"type" "string"
                                  "enum" ["string" "integer" "number" "boolean" "null" "array" "object"]}
                    "format" {"type" "string"}
                    "enums" {"type" "array" "items" {"type" "string"}}}
      "required" ["type" "path"]
      "additionalProperties" false}

     "mdastEach"
     {"title" "Each (Array Iteration)" "type" "object"
      "properties" {"type" {"type" "string" "const" "each"}
                    "path" {"type" "string" "format" "mdast-ref"}
                    "children" children}
      "required" ["type" "path" "children"]
      "additionalProperties" false}

     "mdastConditional"
     {"title" "Conditional" "type" "object"
      "properties" {"type" {"type" "string" "const" "conditional"}
                    "path" {"type" "string" "format" "mdast-ref"}
                    "cases" {"type" "object"
                             "additionalProperties"
                             {"type" "array" "items" {"$ref" (->def "mdastNode")}}}}
      "required" ["type" "path" "cases"]
      "additionalProperties" false})))

;;------------------------------------------------------------------------------
;; Combined: union type and full def map

(defn mdast-node-union
  "Returns the mdastNode oneOf union schema referencing all standard + extension node types.
   ->def is a function that turns a def name into a $ref string."
  [->def standard-defs extension-defs]
  {"oneOf"
   (mapv (fn [def-name] {"$ref" (->def def-name)})
         (concat (keys standard-defs) (keys extension-defs)))})

(defn all-mdast-defs
  "Returns the complete array-map of all mdast $defs (standard + extensions + union)
   for embedding in the M3 meta-schema.
   ->def is a function that turns a def name into a $ref string."
  [->def]
  (let [standard   (mdast-standard-defs ->def)
        extensions (mdast-extension-defs ->def)
        union      (mdast-node-union ->def standard extensions)]
    (merge standard extensions {"mdastNode" union})))
