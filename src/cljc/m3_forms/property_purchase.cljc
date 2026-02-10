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

(ns m3-forms.property-purchase)

;;------------------------------------------------------------------------------
;; UK Residential Property Purchase — M2 Schema

(def property-purchase-m2
  {"$id" "PropertyPurchase"
   "$schema" "M3"
   "type" "object"
   "title" "Property Purchase"
   "required" ["buyer" "property" "financials"]
   "properties"
   (array-map
    "buyer"
    {"type" "object"
     "title" "Buyer"
     "required" ["fullName" "email" "telephone" "currentAddress"]
     "properties"
     (array-map
      "fullName"       {"type" "string" "title" "Full Name"}
      "email"          {"type" "string" "title" "Email" "format" "email"}
      "telephone"      {"type" "string" "title" "Telephone" "format" "telephone-number"}
      "currentAddress" {"type" "string" "title" "Current Address"}
      "solicitor"
      {"type" "object"
       "title" "Solicitor"
       "required" ["firmName" "contactName" "email"]
       "properties"
       (array-map
        "firmName"    {"type" "string" "title" "Firm Name"}
        "contactName" {"type" "string" "title" "Contact Name"}
        "email"       {"type" "string" "title" "Email" "format" "email"}
        "telephone"   {"type" "string" "title" "Telephone" "format" "telephone-number"}
        "reference"   {"type" "string" "title" "Reference"})})}

    "property"
    {"type" "object"
     "title" "Property"
     "required" ["address" "postcode" "propertyType" "tenure" "askingPrice"]
     "properties"
     (array-map
      "address"      {"type" "string" "title" "Address"}
      "postcode"     {"type" "string" "title" "Postcode"}
      "propertyType"
      {"type" "string" "title" "Property Type"
       "enum" ["Detached House" "Semi-Detached House" "Terraced House"
               "Flat/Apartment" "Bungalow" "Cottage" "New Build"]}
      "tenure"
      {"type" "string" "title" "Tenure"
       "enum" ["Freehold" "Leasehold" "Share of Freehold"]}
      "leaseRemaining"
      {"type" "integer" "title" "Lease Remaining (years)"
       "description" "Years remaining on lease (leasehold only)"}
      "bedrooms"     {"type" "integer" "title" "Bedrooms" "minimum" 0 "maximum" 20}
      "askingPrice"  {"type" "integer" "title" "Asking Price" "format" "money"}
      "agreedPrice"  {"type" "integer" "title" "Agreed Price" "format" "money"}
      "epcRating"
      {"type" "string" "title" "EPC Rating"
       "enum" ["A" "B" "C" "D" "E" "F" "G"]}
      "councilTaxBand"
      {"type" "string" "title" "Council Tax Band"
       "enum" ["A" "B" "C" "D" "E" "F" "G" "H"]}
      "floodRisk"
      {"type" "string" "title" "Flood Risk Zone"
       "enum" ["Zone 1 (Low)" "Zone 2 (Medium)" "Zone 3a (High)" "Zone 3b (Functional Floodplain)"]})}

    "financials"
    {"type" "object"
     "title" "Financial Details"
     "required" ["fundingMethod"]
     "properties"
     (array-map
      "fundingMethod"
      {"type" "string" "title" "Funding Method"
       "enum" ["Mortgage" "Cash" "Part Cash / Part Mortgage"]}
      "mortgage"
      {"type" "object"
       "title" "Mortgage"
       "properties"
       (array-map
        "lender"       {"type" "string" "title" "Lender"}
        "amount"       {"type" "integer" "title" "Mortgage Amount" "format" "money"}
        "term"         {"type" "integer" "title" "Term (years)"}
        "interestRate" {"type" "number" "title" "Interest Rate (%)"}
        "monthlyPayment" {"type" "integer" "title" "Monthly Payment" "format" "money"}
        "mortgageType"
        {"type" "string" "title" "Mortgage Type"
         "enum" ["Fixed Rate" "Variable Rate" "Tracker" "Discounted Variable"
                 "Offset" "Interest Only"]}
        "offerDate"    {"type" "string" "title" "Offer Date" "format" "date"}
        "offerExpiry"  {"type" "string" "title" "Offer Expiry" "format" "date"})}
      "deposit"      {"type" "integer" "title" "Deposit Amount" "format" "money"}
      "stampDuty"    {"type" "integer" "title" "Stamp Duty (SDLT)" "format" "money"}
      "legalFees"    {"type" "integer" "title" "Legal Fees" "format" "money"}
      "surveyFees"   {"type" "integer" "title" "Survey Fees" "format" "money"}
      "totalCost"    {"type" "integer" "title" "Total Cost" "format" "money"})}

    "searches"
    {"type" "object"
     "title" "Searches & Surveys"
     "properties"
     (array-map
      "localAuthority"    {"type" "boolean" "title" "Local Authority Search"}
      "environmental"     {"type" "boolean" "title" "Environmental Search"}
      "waterDrainage"     {"type" "boolean" "title" "Water & Drainage Search"}
      "chancelRepair"     {"type" "boolean" "title" "Chancel Repair Search"}
      "surveyType"
      {"type" "string" "title" "Survey Type"
       "enum" ["RICS Level 1 (Condition)" "RICS Level 2 (HomeBuyer)"
               "RICS Level 3 (Building)" "Valuation Only"]}
      "surveyDate"        {"type" "string" "title" "Survey Date" "format" "date"}
      "surveyValuation"   {"type" "integer" "title" "Survey Valuation" "format" "money"}
      "issues"
      {"type" "array"
       "title" "Issues Identified"
       "items"
       {"type" "object"
        "properties"
        (array-map
         "description" {"type" "string" "title" "Description"}
         "severity"    {"type" "string" "title" "Severity"
                        "enum" ["Low" "Medium" "High" "Critical"]}
         "estimatedCost" {"type" "integer" "title" "Estimated Repair Cost" "format" "money"})}})}

    "completion"
    {"type" "object"
     "title" "Completion"
     "properties"
     (array-map
      "exchangeDate"    {"type" "string" "title" "Exchange Date" "format" "date"}
      "completionDate"  {"type" "string" "title" "Completion Date" "format" "date"}
      "moveInDate"      {"type" "string" "title" "Move-in Date" "format" "date"}
      "specialConditions" {"type" "string" "title" "Special Conditions"})})

   ;; mdast document template
   "$document"
   {"type" "root"
    "children"
    [{"type" "heading" "depth" 2
      "children" [{"type" "text" "value" "Property Purchase Summary"}]}

     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Buyer"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children"
        [{"type" "tableCell" "header" true
          "children" [{"type" "text" "value" ""}]}
         {"type" "tableCell" "header" true
          "children" [{"type" "text" "value" "Details"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Name"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.fullName"
                       "description" "Buyer name" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Telephone"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.telephone"
                       "description" "Telephone" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Email"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.email"
                       "description" "Email" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Current Address"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.currentAddress"
                       "description" "Current address" "schemaType" "string"}]}]}]}

     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Solicitor"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children"
        [{"type" "tableCell" "header" true
          "children" [{"type" "text" "value" ""}]}
         {"type" "tableCell" "header" true
          "children" [{"type" "text" "value" "Details"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Firm"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.solicitor.firmName"
                       "description" "Solicitor firm" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Contact"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.solicitor.contactName"
                       "description" "Solicitor contact" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Reference"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "buyer.solicitor.reference"
                       "description" "Solicitor reference" "schemaType" "string"}]}]}]}

     {"type" "thematicBreak"}

     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Property Details"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children"
        [{"type" "tableCell" "header" true
          "children" [{"type" "text" "value" ""}]}
         {"type" "tableCell" "header" true
          "children" [{"type" "text" "value" "Details"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Address"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.address"
                       "description" "Property address" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Postcode"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.postcode"
                       "description" "Postcode" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Type"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.propertyType"
                       "description" "Property type" "schemaType" "string"
                       "enums" ["Detached House" "Semi-Detached House" "Terraced House"
                                "Flat/Apartment" "Bungalow" "Cottage" "New Build"]}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Tenure"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.tenure"
                       "description" "Tenure" "schemaType" "string"
                       "enums" ["Freehold" "Leasehold" "Share of Freehold"]}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Bedrooms"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.bedrooms"
                       "description" "Bedrooms" "schemaType" "integer"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Agreed Price"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.agreedPrice"
                       "description" "Agreed price" "schemaType" "integer"
                       "format" "money"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "EPC Rating"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.epcRating"
                       "description" "EPC rating" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Council Tax Band"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "property.councilTaxBand"
                       "description" "Council tax band" "schemaType" "string"}]}]}]}

     {"type" "thematicBreak"}

     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Financial Summary"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children"
        [{"type" "tableCell" "header" true
          "children" [{"type" "text" "value" ""}]}
         {"type" "tableCell" "header" true
          "children" [{"type" "text" "value" "Amount"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Funding Method"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.fundingMethod"
                       "description" "Funding method" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Mortgage Amount"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.mortgage.amount"
                       "description" "Mortgage amount" "schemaType" "integer"
                       "format" "money"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Deposit"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.deposit"
                       "description" "Deposit" "schemaType" "integer"
                       "format" "money"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Stamp Duty (SDLT)"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.stampDuty"
                       "description" "Stamp duty" "schemaType" "integer"
                       "format" "money"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Legal Fees"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.legalFees"
                       "description" "Legal fees" "schemaType" "integer"
                       "format" "money"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Survey Fees"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.surveyFees"
                       "description" "Survey fees" "schemaType" "integer"
                       "format" "money"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "emphasis"
                       "children" [{"type" "text" "value" "Total Cost"}]}]}
         {"type" "tableCell"
          "children" [{"type" "strong"
                       "children" [{"type" "fieldReference" "path" "financials.totalCost"
                                    "description" "Total cost" "schemaType" "integer"
                                    "format" "money"}]}]}]}]}

     {"type" "thematicBreak"}

     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Key Dates"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children"
        [{"type" "tableCell" "header" true
          "children" [{"type" "text" "value" "Milestone"}]}
         {"type" "tableCell" "header" true
          "children" [{"type" "text" "value" "Date"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Mortgage Offer"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "financials.mortgage.offerDate"
                       "description" "Mortgage offer date" "schemaType" "string"
                       "format" "date"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Survey"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "searches.surveyDate"
                       "description" "Survey date" "schemaType" "string"
                       "format" "date"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Exchange"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "completion.exchangeDate"
                       "description" "Exchange date" "schemaType" "string"
                       "format" "date"}]}]}
       {"type" "tableRow"
        "children"
        [{"type" "tableCell"
          "children" [{"type" "text" "value" "Completion"}]}
         {"type" "tableCell"
          "children" [{"type" "fieldReference" "path" "completion.completionDate"
                       "description" "Completion date" "schemaType" "string"
                       "format" "date"}]}]}]}]}})

;;------------------------------------------------------------------------------
;; M1 — Sample instance data

(def property-purchase-m1
  (array-map
   "buyer"
   (array-map
    "fullName" "Sarah Johnson"
    "email" "sarah.johnson@example.com"
    "telephone" "+44 7700 900123"
    "currentAddress" "42 Oak Lane, Islington, London, N1 2AB"
    "solicitor"
    (array-map
     "firmName" "Smith & Partners LLP"
     "contactName" "James Smith"
     "email" "j.smith@smithpartners.co.uk"
     "telephone" "+44 20 7946 0958"
     "reference" "SP/2025/JN-4521"))

   "property"
   (array-map
    "address" "17 Willow Crescent, Hampstead, London"
    "postcode" "NW3 7TH"
    "propertyType" "Semi-Detached House"
    "tenure" "Freehold"
    "bedrooms" 3
    "askingPrice" 850000
    "agreedPrice" 825000
    "epcRating" "C"
    "councilTaxBand" "F"
    "floodRisk" "Zone 1 (Low)")

   "financials"
   (array-map
    "fundingMethod" "Mortgage"
    "mortgage"
    (array-map
     "lender" "Nationwide Building Society"
     "amount" 618750
     "term" 25
     "interestRate" 4.49
     "monthlyPayment" 3421
     "mortgageType" "Fixed Rate"
     "offerDate" "2025-03-15"
     "offerExpiry" "2025-09-15")
    "deposit" 206250
    "stampDuty" 31250
    "legalFees" 2500
    "surveyFees" 800
    "totalCost" 859550)

   "searches"
   (array-map
    "localAuthority" true
    "environmental" true
    "waterDrainage" true
    "chancelRepair" true
    "surveyType" "RICS Level 2 (HomeBuyer)"
    "surveyDate" "2025-04-10"
    "surveyValuation" 830000
    "issues"
    [(array-map
      "description" "Minor damp in cellar — needs ventilation improvement"
      "severity" "Low"
      "estimatedCost" 1500)
     (array-map
      "description" "Roof tiles need replacement within 2-3 years"
      "severity" "Medium"
      "estimatedCost" 8000)])

   "completion"
   (array-map
    "exchangeDate" "2025-05-20"
    "completionDate" "2025-06-15"
    "moveInDate" "2025-06-16"
    "specialConditions" "Seller to leave garden shed and greenhouse")))

;;------------------------------------------------------------------------------
;; Workflow — multi-state conveyancing process

(def property-purchase-workflow-m1
  {"$schema" "/schemas/workflow"
   "$id" "property-purchase-workflow"
   "states"
   [{"$id" "buyer-info"
     "title" "Buyer Information"
     "views" [["buyer"]]
     "transitions"
     [{"title" "Property Details ->" "state" "property-details"}]}

    {"$id" "property-details"
     "title" "Property Details"
     "views" [["property"]]
     "transitions"
     [{"title" "<- Buyer Info" "state" "buyer-info"}
      {"title" "Financial Details ->" "state" "financial-details"}]}

    {"$id" "financial-details"
     "title" "Financial Details"
     "views" [["financials"]]
     "transitions"
     [{"title" "<- Property" "state" "property-details"}
      {"title" "Searches & Completion ->" "state" "searches-completion"}]}

    {"$id" "searches-completion"
     "title" "Searches & Completion"
     "views" [["searches"] ["completion"]]
     "transitions"
     [{"title" "<- Financials" "state" "financial-details"}]}]})
