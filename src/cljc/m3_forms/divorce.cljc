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

(ns m3-forms.divorce)

;;------------------------------------------------------------------------------

(def security-model
  {"roles" ["Appellant" "Respondent" "Solicitor" "Developer" "Court"]})

(def divorce-m2
  {"$schema" "/schemas/metaschema"
   "$id" "/schemas/divorce"

   "$defs"
   {"Address"
    {"title" "Address"
     "type" "array"
     "items" {"type" "string"}}
    "PropertyAsset"
    {"title" "Property"
     "type" "object"
     "properties"
     {"address" {"$ref" "#/$defs/Address"}
      "value" {"title" "Value" "type" "integer" "format" "money" "multipleOf" 1000}}}
    "BankAccount"
    {"type" "object"
     "properties"
     {"description"
      {"title" "Account Name" "type" "string" "minLength" 0 "maxLength" 20}
      "sortCode"
      {"title" "Sort Code" "type" "string" "format" "bank-sort-code"}
      "accountNumber"
      {"title" "Account Number" "type" "string" "format" "bank-account-number"
       "minLength" 8 "maxLength" 8 "pattern" "^[0-9].*$"}}}
    "PersonalInformation"
    {"type" "object"
     "properties"
     {"fullName" {"type" "string"}
      "address" {"$ref" "#/$defs/Address"}
      "telephoneNumber" {"title" "Telephone Number" "type" "string" "format" "telephone-number"}
      "emailAddress" {"type" "string"}}}
    "BankAccountAsset"
    {"type" "object"
     "properties"
     {"account" {"$ref" "#/$defs/BankAccount"}
      "value" {"title" "Balance" "type" "number" "format" "money" "multipleOf" 0.01}}}
    "InvestmentAccount"
    {"type" "object"
     "properties"
     {"description" {"title" "Company" "type" "string"}
      "accountId" {"title" "Account Number" "type" "string"}}}
    "InvestmentAccountAsset"
    {"type" "object"
     "properties"
     {"account" {"$ref" "#/$defs/InvestmentAccount"}
      "value" {"title" "Value" "type" "integer" "format" "money"}}}
    "CompanyAsset"
    {"type" "object"
     "properties"
     {"name" {"title" "Name" "type" "string"}
      "number" {"type" "string" "title" "Registered Number"}
      "bankAccount" {"$ref" "#/$defs/BankAccountAsset"}}}
    "ChattelAsset"
    {"type" "object"
     "properties"
     {"description" {"title" "Description" "type" "string"}
      "value" {"title" "Value" "type" "integer" "format" "money"}}}
    "Liability"
    {"type" "object"
     "properties"
     {"description" {"title" "Creditor" "type" "string"}
      "value" {"title" "Liability" "type" "integer" "format" "money" "exclusiveMaximum" 0}}}
    "PensionAsset"
    {"type" "object"
     "properties"
     {"description" {"title" "Company" "type" "string"}
      "planNumber" {"title" "Plan Number" "type" "string"}
      "value"  {"title" "Value" "type" "integer" "format" "money"}}}
    "Income"
    {"type" "object"
     "properties"
     {"description" {"title" "Source" "type" "string"}
      "value" {"title" "Income" "type" "integer" "format" "money"}}}}

   "type" "object"
   "properties"
   (array-map
    "personalInformation"
    {"title" "Personal Information" "type" "object" "$ref" "#/$defs/PersonalInformation"}
    "property"
    {"title" "Property (Legal Title)" "type" "array" "items" {"$ref" "#/$defs/PropertyAsset"}}
    "bankAccounts"
    {"title" "Bank Accounts / Cash" "type" "array" "items" {"$ref" "#/$defs/BankAccountAsset"}}
    "investments"
    {"title" "Investments / Policies" "type" "array" "items" {"$ref" "#/$defs/InvestmentAccountAsset"}}
    "businesses"
    {"title" "Business Interests (Inc. CGT)" "type" "array" "items" {"$ref" "#/$defs/CompanyAsset"}}
    "chattels"
    {"title" "Chattels" "type" "array" "items" {"$ref" "#/$defs/ChattelAsset"}}
    "other"
    {"title" "Other" "type" "array" "items" {"type" "string"}}
    "liabilities"
    {"title" "Liabilities" "type" "array" "items" {"$ref" "#/$defs/Liability"}}
    "pensions"
    {"title" "Pensions" "type" "array" "items" {"$ref" "#/$defs/PensionAsset"}}
    "incomes"
    {"title" "Annual Incomes" "type" "array" "items" {"$ref" "#/$defs/Income"}})})

(def divorce-m1
  (array-map
   "$schema" "/schemas/divorce"
   "$id" "my-divorce"

   "personalInformation"
   {"fullName" "Alex Example"
    "address" ["1 Demo Street" "Testville" "Sampleshire" "EX1 1AA"]
    "emailAddress" "alex@example.com"
    "telephoneNumber" "+44 0000 000000"}

   "property"
   [{"address" ["10 Example Lane" "Testbury" "Sampleshire" "EX2 2BB"]
     "value" 500000}
    {"address" ["25 Demo Court" "Testford" "EX3 3CC"]
     "value" 300000}]

   "bankAccounts"
   [{"account" {"description" "Joint Current" "sortCode" "00-00-00" "accountNumber" "00000001"} "value" 1000.00}
    {"account" {"description" "Savings" "sortCode" "00-00-00" "accountNumber" "00000002"} "value" 2000.00}]

   "investments"
   [{"account" {"description" "Example Fund" "accountId" "INV-001"} "value" 5000}
    {"account" {"description" "Sample ISA" "accountId" "INV-002"} "value" 3000}]

   "businesses"
   [{"name" "Example Consulting Ltd."
     "number" "EX000001"
     "bankAccount" {"account" {"description" "Business Current" "sortCode" "00-00-01" "accountNumber" "00000003"}
                    "value" 50000}}]

   "chattels"
   [{"description" "Furniture" "value" 5000}
    {"description" "Vehicle" "value" 8000}]

   "other" []

   "liabilities"
   [{"description" "Personal Loan" "value" -10000}
    {"description" "Credit Card" "value" -2000}]

   "pensions"
   [{"description" "Workplace Pension" "planNumber" "PEN-0001" "value" 25000}
    {"description" "Private Pension" "planNumber" "PEN-0002" "value" 15000}]

   "incomes"
   [{"description" "Employment" "value" 45000}
    {"description" "Rental" "value" 6000}]))

(def divorce-workflow-m1
  {"$schema" "/schemas/workflow"
   "$id" "divorce-workflow"
   "states"
   [{"$id" "personal-information"
     "views" [["personalInformation"]]
     "transitions"
     [{"title" "-> Assets & Liabilities" "state" "assets-and-liabilities"}]}
    {"$id" "assets-and-liabilities"
     "views"
     [["property"] ["bankAccounts"] ["investments"] ["businesses"]
      ["chattels"] ["other"] ["liabilities"] ["pensions"] ["incomes"]]
     "transitions"
     [{"title" "Personal Information <-" "state" "personal-information"}]}]})
