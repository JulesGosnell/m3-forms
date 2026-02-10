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

(ns m3-forms.tax-return)

;;------------------------------------------------------------------------------
;; UK Self Assessment Tax Return (SA100) — M2 Schema

(def tax-return-m2
  {"$id" "TaxReturn"
   "$schema" "M3"
   "type" "object"
   "title" "Self Assessment Tax Return"
   "required" ["taxpayer" "income" "taxYear"]
   "properties"
   (array-map
    "taxYear"
    {"type" "object"
     "title" "Tax Year"
     "required" ["startDate" "endDate"]
     "properties"
     (array-map
      "startDate" {"type" "string" "title" "Tax Year Start" "format" "date"}
      "endDate"   {"type" "string" "title" "Tax Year End" "format" "date"})}

    "taxpayer"
    {"type" "object"
     "title" "Taxpayer"
     "required" ["fullName" "utr" "nationalInsurance"]
     "properties"
     (array-map
      "fullName"          {"type" "string" "title" "Full Name"}
      "utr"               {"type" "string" "title" "Unique Taxpayer Reference (UTR)"
                           "minLength" 10 "maxLength" 10}
      "nationalInsurance" {"type" "string" "title" "National Insurance Number"}
      "dateOfBirth"       {"type" "string" "title" "Date of Birth" "format" "date"}
      "address"           {"type" "string" "title" "Address"}
      "postcode"          {"type" "string" "title" "Postcode"}
      "telephone"         {"type" "string" "title" "Telephone" "format" "telephone-number"}
      "email"             {"type" "string" "title" "Email" "format" "email"})}

    "income"
    {"type" "object"
     "title" "Income"
     "required" ["employment"]
     "properties"
     (array-map
      "employment"
      {"type" "object"
       "title" "Employment Income"
       "properties"
       (array-map
        "grossPay"         {"type" "integer" "title" "Gross Pay" "format" "money"}
        "taxDeducted"      {"type" "integer" "title" "Tax Deducted (PAYE)" "format" "money"}
        "employerName"     {"type" "string" "title" "Employer Name"}
        "employerPAYE"     {"type" "string" "title" "Employer PAYE Reference"}
        "benefits"         {"type" "integer" "title" "Benefits in Kind (P11D)" "format" "money"}
        "expenses"         {"type" "integer" "title" "Allowable Employment Expenses" "format" "money"})}

      "selfEmployment"
      {"type" "object"
       "title" "Self-Employment Income"
       "properties"
       (array-map
        "businessName"     {"type" "string" "title" "Business Name"}
        "businessType"     {"type" "string" "title" "Business Type"
                           "enum" ["Sole Trader" "Partnership" "Other"]}
        "turnover"         {"type" "integer" "title" "Turnover" "format" "money"}
        "allowableExpenses" {"type" "integer" "title" "Allowable Business Expenses" "format" "money"}
        "netProfit"        {"type" "integer" "title" "Net Profit" "format" "money"})}

      "property"
      {"type" "object"
       "title" "Property Income"
       "properties"
       (array-map
        "rentalIncome"     {"type" "integer" "title" "Rental Income" "format" "money"}
        "allowableExpenses" {"type" "integer" "title" "Allowable Property Expenses" "format" "money"}
        "netPropertyIncome" {"type" "integer" "title" "Net Property Income" "format" "money"}
        "numProperties"    {"type" "integer" "title" "Number of Properties" "minimum" 0 "maximum" 50})}

      "savings"
      {"type" "object"
       "title" "Savings & Investment Income"
       "properties"
       (array-map
        "bankInterest"     {"type" "integer" "title" "Bank/Building Society Interest" "format" "money"}
        "dividends"        {"type" "integer" "title" "Dividends" "format" "money"}
        "otherInvestment"  {"type" "integer" "title" "Other Investment Income" "format" "money"})}

      "other"
      {"type" "object"
       "title" "Other Income"
       "properties"
       (array-map
        "statePension"     {"type" "integer" "title" "State Pension" "format" "money"}
        "privatePension"   {"type" "integer" "title" "Private/Occupational Pension" "format" "money"}
        "foreignIncome"    {"type" "integer" "title" "Foreign Income" "format" "money"}
        "otherIncome"      {"type" "integer" "title" "Other Taxable Income" "format" "money"})})}

    "deductions"
    {"type" "object"
     "title" "Deductions & Allowances"
     "properties"
     (array-map
      "personalAllowance" {"type" "integer" "title" "Personal Allowance" "format" "money"}
      "giftAid"           {"type" "integer" "title" "Gift Aid Donations" "format" "money"}
      "pensionContributions" {"type" "integer" "title" "Pension Contributions" "format" "money"}
      "marriageAllowance" {"type" "boolean" "title" "Marriage Allowance Transfer"}
      "blindPersons"      {"type" "boolean" "title" "Blind Person's Allowance"}
      "otherDeductions"   {"type" "integer" "title" "Other Deductions" "format" "money"})}

    "capitalGains"
    {"type" "object"
     "title" "Capital Gains"
     "properties"
     (array-map
      "disposals"
      {"type" "array"
       "title" "Disposals"
       "items"
       {"type" "object"
        "properties"
        (array-map
         "assetType"     {"type" "string" "title" "Asset Type"
                         "enum" ["Shares/Securities" "Residential Property"
                                 "Other Property" "Other Assets"]}
         "description"   {"type" "string" "title" "Description"}
         "disposalDate"  {"type" "string" "title" "Disposal Date" "format" "date"}
         "disposalValue" {"type" "integer" "title" "Disposal Value" "format" "money"}
         "acquisitionCost" {"type" "integer" "title" "Acquisition Cost" "format" "money"}
         "gain"          {"type" "integer" "title" "Gain/(Loss)" "format" "money"})}}
      "annualExemption" {"type" "integer" "title" "Annual Exempt Amount" "format" "money"}
      "totalGains"      {"type" "integer" "title" "Total Gains" "format" "money"}
      "totalLosses"     {"type" "integer" "title" "Total Losses" "format" "money"}
      "taxableGains"    {"type" "integer" "title" "Taxable Gains" "format" "money"})}

    "summary"
    {"type" "object"
     "title" "Tax Calculation Summary"
     "properties"
     (array-map
      "totalIncome"       {"type" "integer" "title" "Total Income" "format" "money"}
      "totalDeductions"   {"type" "integer" "title" "Total Deductions" "format" "money"}
      "taxableIncome"     {"type" "integer" "title" "Taxable Income" "format" "money"}
      "incomeTax"         {"type" "integer" "title" "Income Tax Due" "format" "money"}
      "nationalInsurance" {"type" "integer" "title" "National Insurance Due" "format" "money"}
      "capitalGainsTax"   {"type" "integer" "title" "Capital Gains Tax" "format" "money"}
      "totalTaxDue"       {"type" "integer" "title" "Total Tax Due" "format" "money"}
      "taxAlreadyPaid"    {"type" "integer" "title" "Tax Already Paid (PAYE etc.)" "format" "money"}
      "balanceDue"        {"type" "integer" "title" "Balance Due / Refund" "format" "money"}
      "paymentOnAccount"  {"type" "integer" "title" "Payments on Account" "format" "money"})})

   "$document"
   {"type" "root"
    "children"
    [{"type" "heading" "depth" 2
      "children" [{"type" "text" "value" "HM Revenue & Customs"}]}
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Self Assessment Tax Return"}]}

     ;; Tax Year
     {"type" "paragraph"
      "children" [{"type" "strong"
                   "children" [{"type" "text" "value" "Tax Year: "}]}
                  {"type" "fieldReference" "path" "taxYear.startDate"
                   "description" "Tax year start" "schemaType" "string" "format" "date"}
                  {"type" "text" "value" " to "}
                  {"type" "fieldReference" "path" "taxYear.endDate"
                   "description" "Tax year end" "schemaType" "string" "format" "date"}]}
     {"type" "thematicBreak"}

     ;; Taxpayer section
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Taxpayer Details"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children" [{"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" ""}]}
                    {"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" "Details"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Name"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "taxpayer.fullName"
                                  "description" "Full name" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "UTR"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "taxpayer.utr"
                                  "description" "UTR" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "NI Number"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "taxpayer.nationalInsurance"
                                  "description" "NI number" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Address"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "taxpayer.address"
                                  "description" "Address" "schemaType" "string"}]}]}]}
     {"type" "thematicBreak"}

     ;; Employment Income
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Employment Income"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children" [{"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" ""}]}
                    {"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" "Amount"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Employer"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.employment.employerName"
                                  "description" "Employer" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Gross Pay"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.employment.grossPay"
                                  "description" "Gross pay" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Tax Deducted (PAYE)"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.employment.taxDeducted"
                                  "description" "PAYE tax" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Benefits in Kind"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.employment.benefits"
                                  "description" "Benefits" "schemaType" "integer"
                                  "format" "money"}]}]}]}
     {"type" "thematicBreak"}

     ;; Self-Employment
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Self-Employment Income"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children" [{"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" ""}]}
                    {"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" "Amount"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Business"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.selfEmployment.businessName"
                                  "description" "Business name" "schemaType" "string"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Turnover"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.selfEmployment.turnover"
                                  "description" "Turnover" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Expenses"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.selfEmployment.allowableExpenses"
                                  "description" "Expenses" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Net Profit"}]}
                    {"type" "tableCell"
                     "children" [{"type" "strong"
                                  "children" [{"type" "fieldReference" "path" "income.selfEmployment.netProfit"
                                               "description" "Net profit" "schemaType" "integer"
                                               "format" "money"}]}]}]}]}
     {"type" "thematicBreak"}

     ;; Savings & Investments
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Savings & Investment Income"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children" [{"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" ""}]}
                    {"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" "Amount"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Bank Interest"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.savings.bankInterest"
                                  "description" "Bank interest" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Dividends"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "income.savings.dividends"
                                  "description" "Dividends" "schemaType" "integer"
                                  "format" "money"}]}]}]}
     {"type" "thematicBreak"}

     ;; Capital Gains disposals
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Capital Gains — Disposals"}]}
     {"type" "each" "path" "capitalGains.disposals"
      "children"
      [{"type" "table"
        "children"
        [{"type" "tableRow"
          "children" [{"type" "tableCell" "header" true
                       "children" [{"type" "text" "value" ""}]}
                      {"type" "tableCell" "header" true
                       "children" [{"type" "text" "value" "Details"}]}]}
         {"type" "tableRow"
          "children" [{"type" "tableCell"
                       "children" [{"type" "text" "value" "Asset"}]}
                      {"type" "tableCell"
                       "children" [{"type" "fieldReference" "path" "assetType"
                                    "description" "Asset type" "schemaType" "string"}]}]}
         {"type" "tableRow"
          "children" [{"type" "tableCell"
                       "children" [{"type" "text" "value" "Description"}]}
                      {"type" "tableCell"
                       "children" [{"type" "fieldReference" "path" "description"
                                    "description" "Description" "schemaType" "string"}]}]}
         {"type" "tableRow"
          "children" [{"type" "tableCell"
                       "children" [{"type" "text" "value" "Disposed"}]}
                      {"type" "tableCell"
                       "children" [{"type" "fieldReference" "path" "disposalDate"
                                    "description" "Disposal date" "schemaType" "string"
                                    "format" "date"}]}]}
         {"type" "tableRow"
          "children" [{"type" "tableCell"
                       "children" [{"type" "text" "value" "Proceeds"}]}
                      {"type" "tableCell"
                       "children" [{"type" "fieldReference" "path" "disposalValue"
                                    "description" "Disposal value" "schemaType" "integer"
                                    "format" "money"}]}]}
         {"type" "tableRow"
          "children" [{"type" "tableCell"
                       "children" [{"type" "text" "value" "Cost"}]}
                      {"type" "tableCell"
                       "children" [{"type" "fieldReference" "path" "acquisitionCost"
                                    "description" "Acquisition cost" "schemaType" "integer"
                                    "format" "money"}]}]}
         {"type" "tableRow"
          "children" [{"type" "tableCell"
                       "children" [{"type" "text" "value" "Gain/(Loss)"}]}
                      {"type" "tableCell"
                       "children" [{"type" "strong"
                                    "children" [{"type" "fieldReference" "path" "gain"
                                                 "description" "Gain/loss" "schemaType" "integer"
                                                 "format" "money"}]}]}]}]}
       {"type" "thematicBreak"}]}

     ;; Tax Calculation Summary
     {"type" "heading" "depth" 3
      "children" [{"type" "text" "value" "Tax Calculation Summary"}]}
     {"type" "table"
      "children"
      [{"type" "tableRow"
        "children" [{"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" ""}]}
                    {"type" "tableCell" "header" true
                     "children" [{"type" "text" "value" "Amount"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Total Income"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "summary.totalIncome"
                                  "description" "Total income" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Total Deductions"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "summary.totalDeductions"
                                  "description" "Total deductions" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Taxable Income"}]}
                    {"type" "tableCell"
                     "children" [{"type" "strong"
                                  "children" [{"type" "fieldReference" "path" "summary.taxableIncome"
                                               "description" "Taxable income" "schemaType" "integer"
                                               "format" "money"}]}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Income Tax Due"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "summary.incomeTax"
                                  "description" "Income tax" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "National Insurance"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "summary.nationalInsurance"
                                  "description" "NI contributions" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Capital Gains Tax"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "summary.capitalGainsTax"
                                  "description" "CGT" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Total Tax Due"}]}
                    {"type" "tableCell"
                     "children" [{"type" "strong"
                                  "children" [{"type" "fieldReference" "path" "summary.totalTaxDue"
                                               "description" "Total tax" "schemaType" "integer"
                                               "format" "money"}]}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "text" "value" "Tax Already Paid"}]}
                    {"type" "tableCell"
                     "children" [{"type" "fieldReference" "path" "summary.taxAlreadyPaid"
                                  "description" "Tax paid" "schemaType" "integer"
                                  "format" "money"}]}]}
       {"type" "tableRow"
        "children" [{"type" "tableCell"
                     "children" [{"type" "strong"
                                  "children" [{"type" "text" "value" "Balance Due"}]}]}
                    {"type" "tableCell"
                     "children" [{"type" "strong"
                                  "children" [{"type" "fieldReference" "path" "summary.balanceDue"
                                               "description" "Balance due" "schemaType" "integer"
                                               "format" "money"}]}]}]}]}]}})


;;------------------------------------------------------------------------------
;; M1 — Sample Instance Data

(def tax-return-m1
  (array-map
   "taxYear" (array-map
              "startDate" "2025-04-06"
              "endDate"   "2026-04-05")

   "taxpayer" (array-map
               "fullName"          "James Richardson"
               "utr"               "1234567890"
               "nationalInsurance" "AB 12 34 56 C"
               "dateOfBirth"       "1985-03-15"
               "address"           "14 Elm Street, Bristol, BS1 5QA"
               "postcode"          "BS1 5QA"
               "telephone"         "+44 (0)7700 900456"
               "email"             "james.richardson@example.com")

   "income" (array-map
             "employment" (array-map
                           "grossPay"     52000
                           "taxDeducted"  8200
                           "employerName" "Bristol Technology Ltd"
                           "employerPAYE" "123/A456"
                           "benefits"     2400
                           "expenses"     750)

             "selfEmployment" (array-map
                               "businessName"      "Richardson Consulting"
                               "businessType"      "Sole Trader"
                               "turnover"          18500
                               "allowableExpenses" 4200
                               "netProfit"         14300)

             "property" (array-map
                         "rentalIncome"      12000
                         "allowableExpenses" 3800
                         "netPropertyIncome" 8200
                         "numProperties"     1)

             "savings" (array-map
                        "bankInterest"    450
                        "dividends"       2800
                        "otherInvestment" 0)

             "other" (array-map
                      "statePension"   0
                      "privatePension" 0
                      "foreignIncome"  0
                      "otherIncome"    0))

   "deductions" (array-map
                 "personalAllowance"    12570
                 "giftAid"              500
                 "pensionContributions" 3600
                 "marriageAllowance"    false
                 "blindPersons"         false
                 "otherDeductions"      0)

   "capitalGains" (array-map
                   "disposals"
                   [(array-map
                     "assetType"       "Shares/Securities"
                     "description"     "500 shares in Acme Corp plc"
                     "disposalDate"    "2025-11-15"
                     "disposalValue"   15000
                     "acquisitionCost" 8000
                     "gain"            7000)
                    (array-map
                     "assetType"       "Other Assets"
                     "description"     "Vintage car (1967 MGB)"
                     "disposalDate"    "2025-09-20"
                     "disposalValue"   25000
                     "acquisitionCost" 12000
                     "gain"            13000)]
                   "annualExemption" 6000
                   "totalGains"      20000
                   "totalLosses"     0
                   "taxableGains"    14000)

   "summary" (array-map
              "totalIncome"       77950
              "totalDeductions"   16670
              "taxableIncome"     61280
              "incomeTax"         11456
              "nationalInsurance" 3200
              "capitalGainsTax"   2800
              "totalTaxDue"       17456
              "taxAlreadyPaid"    8200
              "balanceDue"        9256
              "paymentOnAccount"  4628)))

;;------------------------------------------------------------------------------
;; Workflow — 4 states

(def tax-return-workflow-m1
  {"$schema" "/schemas/workflow"
   "$id" "tax-return-workflow"
   "states"
   [{"$id" "taxpayer-details"
     "title" "Taxpayer"
     "views" [["taxYear"] ["taxpayer"]]
     "transitions" [{"title" "Income ->" "state" "income"}]}

    {"$id" "income"
     "title" "Income"
     "views" [["income"]]
     "transitions" [{"title" "<- Taxpayer" "state" "taxpayer-details"}
                    {"title" "Deductions ->" "state" "deductions"}]}

    {"$id" "deductions"
     "title" "Deductions"
     "views" [["deductions"] ["capitalGains"]]
     "transitions" [{"title" "<- Income" "state" "income"}
                    {"title" "Summary ->" "state" "summary"}]}

    {"$id" "summary"
     "title" "Summary"
     "views" [["summary"]]
     "transitions" [{"title" "<- Deductions" "state" "deductions"}]}]})
