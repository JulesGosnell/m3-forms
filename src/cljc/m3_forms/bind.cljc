(ns m3-forms.bind
  (:require
   [m3-forms.schema :refer [externalise-$refs]]))

;;------------------------------------------------------------------------------

(defn bind-get-in [[h & t :as path] [{ps "properties" pis "prefixItems" is "items" oos "oneOf" :as m2} m1]]
  (if (empty? path)
    [m2 m1]
    (bind-get-in
     t
     (cond
       (and (string? h) (map? ps))
       [(ps h) (m1 h)]

       (and (integer? h) (or pis is))
       [(nth (concat pis (repeat is)) h) (m1 h)]))))

(defn map-or-nil? [v]
  (or (map? v) (nil? v)))

(defn bind-put-in [[h & t :as path] [{ps "properties" pis "prefixItems" is "items" oos "oneOf" :as super-m2} super-m1] [sub-m2 sub-m1 :as sub]]
  (if (empty? path)
    [(merge super-m2 sub-m2)
     (if (and (map-or-nil? super-m1) (map-or-nil? sub-m1)) (merge super-m1 sub-m1) sub-m1)]
    (let [[m2 m1]
          (bind-put-in
           t
           (cond
             (and (string? h) (map? ps))
             [(ps h) (super-m1 h)]

             (and (integer? h) (or pis is))
             [(if (< h (count pis)) (nth pis h) is) (when (< h (count super-m1)) (nth super-m1 h))])
           sub)]

      [(cond
         (string? h)
         (update super-m2 "properties" (fnil (fn [ps] (assoc ps h m2)) (array-map)))
         (integer? h)
         (update super-m2 "prefixItems" (fnil (fn [ps] (assoc ps h m2)) (conj (vec (repeat h false)) m2))))
       (assoc
        (cond
          (string? h)
          super-m1
          (integer? h)
          (reduce (fnil conj []) super-m1 (repeat (- h (count super-m1)) nil)))
        h m1)])))

;;------------------------------------------------------------------------------

(defn bind-2 [src-path [{id "$id" :as src-m2} src-m1 :as srcs] tgt-path tgts]
  (bind-put-in tgt-path tgts ((fn [[m2 m1]][(externalise-$refs id m2) m1]) (bind-get-in src-path srcs))))

;;------------------------------------------------------------------------------

(def bindings-m2
  {"$schema" "/schemas/metaschema"
   "$id" "/schemas/bindings"
   "$defs"
   {"Path"
    {"type" "array"
     "items"
     {"oneOf"
      [{"type" "string"}
       {"type" "integer"}]}}
    "End"
    {"type" "object"
     "properties"
     {"path"
      {"$ref" "#/$defs/Path"}}}
    "Binding"
    {"type" "object"
   "properties"
   {"src"
    {"$ref" "#/$defs/End"}
    "tgt"
    {"$ref" "#/$defs/End"}}}}

   "type" "array"
   "items"
   {"$ref" "#/$defs/Binding"}})

;;------------------------------------------------------------------------------

(defn bind [bindings srcs tgts]
  (reduce
   (fn [acc {{sp "path"} "src" {tp "path"} "tgt"}]
     (bind-2 sp srcs tp acc))
   tgts
   bindings))

;;------------------------------------------------------------------------------

(defn flip [{s "src" t "tgt"}]
  {"src" t "tgt" s})
