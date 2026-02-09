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
    "tranche_number" "issue_price" "dealer_name" "final_terms_date" "payoff"]})

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

(def final-terms-m0
  "
```
Final Terms
EXAMPLE BANK S.p.A.

```
Legal entity identifier (LEI): {{lei}}
```
**Issue of up to {{num_certificates}} Certificates
{{#with payoff}}
 {{#switch type}}
  {{#case \"Fixed Rate Notes\"}}
   \"Fixed Rate Notes linked to Reference Rate Index due {{../due_date}}\"
  {{/case}}
  {{#case \"Floating Rate Notes\"}}
   \"Floating Rate Notes linked to {{reference_entity}} Senior Debt due {{../due_date}}\"
  {{/case}}
  {{#case \"Zero Coupon Notes\"}}
   \"Zero Coupon Notes linked to a basket of Reference Entities due {{../due_date}}\"
  {{/case}}
  {{#case \"Index Linked Notes\"}}
   \"Index Linked Notes linked to {{index}} due {{../due_date}}\"
  {{/case}}
 {{/switch}}
{{/with}}
commercially named
\"{{payoff.commercial_name}}\"
under the
Issuance Programme**
SERIES NO: {{series_number}}
TRANCHE NO: {{tranche_number}}

```
Issue Price: EUR {{issue_price}} per Security
```
```
Dealer: {{dealer_name}}
```
```
The date of these Final Terms is {{final_terms_date}}
```
")
