(ns m3-forms.demo)

(def demo-m2
  {"$id" "Demo"
   "$schema" "M3"
   "type" "object"
   "title" "An Object"
   "properties"
   (array-map
    "a-string"
    {"type" "string"
     "title" "A String"
     "default" "blah..."}
    "a-string-enum"
    {"type" "string"
     "title" "A String Enum"
     "enum" ["eggs" "bacon" "sausage" "beans" "toast"]}
    "a-date"
    {"type" "string"
     "title" "A Date"
     "format" "date"
     "default" "2022-04-29"}
    "a-time"
    {"type" "string"
     "title" "A Time"
     "format" "time"
     "default" "20:28"}
    "a-date-time"
    {"type" "string"
     "title" "A Date-Time"
     "format" "date-time"
     "default" "2022-08-03T20:28"}
    "a-year-month"
    {"type" "string"
     "title" "A Year-Month"
     "format" "year-month"
     "default" "2022-02"}
    "an-integer"
    {"type" "integer"
     "title" "An Integer"
     "default" 0}
    "an-integer-enum"
    {"type" "integer"
     "title" "An Integer Enum"
     "enum" [1 2 3 4 5 6 7 8 9 0]
     "default" 0}
    "an-integer-range"
    {"type" "integer"
     "format" "range"
     "title" "An Integer Range"
     "default" 5
     "minimum" 0
     "maximum" 11}
    "a-number"
    {"type" "number"
     "title" "A Number"
     "default" 0.0}
    "a-number-enum"
    {"type" "number"
     "title" "A Number Enum"
     "enum" [1.1 2.2 3.3 4.4 5.5 6.6 7.7 8.8 9.9 0.0]
     "default" 0.0}
    "a-number-range"
    {"type" "number"
     "title" "A Number Range"
     "format" "range"
     "multipleOf" 0.5
     "minimum" 0
     "maximum" 10}
    "a-boolean"
    {"type" "boolean"
     "title" "A Boolean"
     "default" false}
    "a-null"
    {"type" "null"
     "title" "A Null"}
    "an-object"
    {"type" "object"
     "title" "Full English"
     "properties"
     {"number-of-sausages"
      {"type" "integer" "minimum" 0 "maximum" 4 "title" "How many sausages would you like?"}
      "type-of-sausages"
      {"type" "string" "enum" ["pork" "beef" "chicken" "vege"]  "title" "What type of sausages would you like ?"}}}
    "an-object-enum"
    {"type" "object"
     "properties"
     {"title" {"type" "string"}
      "$id" {"type" "string"}}
     "enum" [{"title" "Eggs" "$id" "eggs"} {"title" "Bacon" "$id" "bacon"} {"title" "Sausage" "$id" "sausage"} {"title" "Beans" "$id" "beans"} {"title" "Toast" "$id" "toast"}]}
    "an-array"
    {"type" "array"
     "title" "An Array"
     "prefixItems" [{"type" "string"} {"type" "integer"} {"type" "number"}]
     "items" {"type" "boolean"}
     "maxItems" 8})
   "patternProperties" {"SmallInteger$" {"type" "integer" "maximum" 10}}
   "additionalProperties" {"type" "string"}})

(def demo-m1
  (array-map
   "a-string" "a"
   "a-date" "2022-04-29"
   "an-integer" 1
   "a-number" 1.0
   "a-boolean" true
   "a-null" nil
   "an-array" ["item-1" 2 3 true false true]
   "a-SmallInteger" 9
   "an-additional-string" "hello!"
   "another-additional-string" "goodbye!"))
