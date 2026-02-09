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

(ns m3-forms.final-terms)

(def security-model
  {"roles" ["Structuring"]})

(def final-terms-m2
  {"$schema" "http://json-schema.org/draft-07/schema#",
   "type" "object",
   "properties"
   {"tranche_number" {"type" "integer"},
    "due_date" {"type" "string", "format" "date"},
    "lei" {"type" "string"},
    "issue_price" {"type" "number"},
    "dealer_name" {"type" "string"},
    "payoff"
    {"oneOf"
     [{"type" "object",
       "properties"
       {"type" {"type" "string", "const" "Fixed Rate Notes"},
        "commercial_name" {"type" "string"},
        "indices" {"type" "array", "items" {"type" "string"}}},
       "required" ["type" "commercial_name" "indices"]}
      {"type" "object",
       "properties"
       {"type" {"type" "string", "const" "Floating Rate Notes"},
        "commercial_name" {"type" "string"},
        "reference_entity" {"type" "string"},
        "senior_debt_due_date" {"type" "string", "format" "date"}},
       "required" ["type" "commercial_name" "reference_entity" "senior_debt_due_date"]}
      {"type" "object",
       "properties"
       {"type" {"type" "string", "const" "Zero Coupon Notes"},
        "commercial_name" {"type" "string"},
        "reference_entities" {"type" "array", "items" {"type" "string"}}},
       "required" ["type" "commercial_name" "reference_entities"]}
      {"type" "object",
       "properties"
       {"type" {"type" "string", "const" "Index Linked Notes"},
        "commercial_name" {"type" "string"},
        "index" {"type" "string"}},
       "required" ["type" "commercial_name" "index"]}]},
    "series_number" {"type" "integer"},
    "final_terms_date" {"type" "string", "format" "date"},
    "num_certificates" {"type" "integer"}},
   "required"
   ["lei" "num_certificates" "due_date" "series_number"
    "tranche_number" "issue_price" "dealer_name" "final_terms_date" "payoff"]

   "$document"
   {"type" "root"
    "children"
    [{"type" "heading" "depth" 2 "children" [{"type" "text" "value" "Final Terms"}]}
     {"type" "heading" "depth" 3 "children" [{"type" "text" "value" "EXAMPLE BANK S.p.A."}]}
     {"type" "thematicBreak"}
     ;; LEI
     {"type" "paragraph" "children"
      [{"type" "text" "value" "Legal entity identifier (LEI): "}
       {"type" "fieldReference" "path" "lei"
        "description" "LEI: Legal Entity Identifier for the issuer" "schemaType" "string"}]}
     {"type" "thematicBreak"}
     ;; Issue paragraph with conditional payoff type
     {"type" "paragraph" "children"
      [{"type" "strong" "children"
        [{"type" "text" "value" "Issue of up to "}
         {"type" "fieldReference" "path" "num_certificates"
          "description" "Num Certificates: Maximum number of certificates in this issuance" "schemaType" "integer"}
         {"type" "text" "value" " Certificates "}
         {"type" "conditional" "path" "payoff.type"
          "cases"
          {"Fixed Rate Notes"
           [{"type" "text" "value" "\"Fixed Rate Notes linked to Reference Rate Index due "}
            {"type" "fieldReference" "path" "due_date" "description" "Due Date: Maturity date of the securities" "schemaType" "string" "format" "date"}
            {"type" "text" "value" "\""}]
           "Floating Rate Notes"
           [{"type" "text" "value" "\"Floating Rate Notes linked to "}
            {"type" "fieldReference" "path" "payoff.reference_entity" "description" "Reference Entity" "schemaType" "string"}
            {"type" "text" "value" " Senior Debt due "}
            {"type" "fieldReference" "path" "due_date" "description" "Due Date: Maturity date of the securities" "schemaType" "string" "format" "date"}
            {"type" "text" "value" "\""}]
           "Zero Coupon Notes"
           [{"type" "text" "value" "\"Zero Coupon Notes linked to a basket of Reference Entities due "}
            {"type" "fieldReference" "path" "due_date" "description" "Due Date: Maturity date of the securities" "schemaType" "string" "format" "date"}
            {"type" "text" "value" "\""}]
           "Index Linked Notes"
           [{"type" "text" "value" "\"Index Linked Notes linked to "}
            {"type" "fieldReference" "path" "payoff.index" "description" "Index: The reference index" "schemaType" "string"}
            {"type" "text" "value" " due "}
            {"type" "fieldReference" "path" "due_date" "description" "Due Date: Maturity date of the securities" "schemaType" "string" "format" "date"}
            {"type" "text" "value" "\""}]}}
         {"type" "text" "value" " commercially named \""}
         {"type" "fieldReference" "path" "payoff.commercial_name"
          "description" "Commercial Name: Marketing name of the product" "schemaType" "string"}
         {"type" "text" "value" "\" under the Issuance Programme"}]}]}
     ;; Series and Tranche
     {"type" "paragraph" "children"
      [{"type" "text" "value" "SERIES NO: "}
       {"type" "fieldReference" "path" "series_number"
        "description" "Series Number" "schemaType" "integer"}
       {"type" "text" "value" " TRANCHE NO: "}
       {"type" "fieldReference" "path" "tranche_number"
        "description" "Tranche Number" "schemaType" "integer"}]}
     {"type" "thematicBreak"}
     ;; Issue Price
     {"type" "paragraph" "children"
      [{"type" "text" "value" "Issue Price: EUR "}
       {"type" "fieldReference" "path" "issue_price"
        "description" "Issue Price: Price per security in EUR" "schemaType" "number"}
       {"type" "text" "value" " per Security"}]}
     {"type" "thematicBreak"}
     ;; Dealer
     {"type" "paragraph" "children"
      [{"type" "text" "value" "Dealer: "}
       {"type" "fieldReference" "path" "dealer_name"
        "description" "Dealer: The dealer arranging the issuance" "schemaType" "string"}]}
     {"type" "thematicBreak"}
     ;; Final Terms Date
     {"type" "paragraph" "children"
      [{"type" "text" "value" "The date of these Final Terms is "}
       {"type" "fieldReference" "path" "final_terms_date"
        "description" "Final Terms Date" "schemaType" "string" "format" "date"}]}]}})

(def final-terms-m1
  {"tranche_number" 1,
   "due_date" "2025-12-31",
   "lei" "ABCD1234EFGH5678IJ90",
   "issue_price" 10000,
   "dealer_name" "Example Bank S.p.A.",
   "payoff"
   {"type" "Index Linked Notes",
    "commercial_name"
    "Example Certificates linked to Example Index 100",
    "index" "Example Index 100"},
   "series_number" 100,
   "final_terms_date" "2025-01-15",
   "num_certificates" 500})

(def final-terms-workflow-m1
  {"$schema" "/schemas/workflow"
   "$id" "final-terms-workflow"
   "states"
   [{"$id" "everything"
     "views" [["everything"]]}]})

