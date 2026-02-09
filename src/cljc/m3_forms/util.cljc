(ns m3-forms.util
  (:require
   [clojure.string :refer [join]]
   [m3-forms.json :refer [present?]]
   [m3-forms.schema :refer [check-schema]]))

(def conjv (fnil conj []))

(defn index-by [k ms]
  (into (sorted-map) (map (fn [{v k :as m}][v m]) ms)))

(def index-by-$id (partial index-by "$id"))

(defn keyfn [k] (fn [m] {m k}))

(defn make-id [path]
  (join "." path))

(defn valid? [m2-ctx m2]
  (let [v? (check-schema m2-ctx [] m2)]
    (fn [m1-ctx m1]
      (let [r (when (present? m1) (v? m1-ctx [] m1))]
        (if (seq r)
          (do
            (prn r)
            "invalid")
          "valid")))))

(defn mapl [& args] (doall (apply map args)))

(defn vector-remove-nth [v n]
  (mapv second (filter (comp (complement #{n}) first) (map vector (range) v))))

;;------------------------------------------------------------------------------

;; Custom format checkers (stubs)
(defn check-format-year-month          [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-range               [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-money               [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-bank-account-number [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-bank-sort-code      [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))
(defn check-format-telephone-number    [_property _m2-ctx m2-path m2-doc m2-val] (fn [m1-ctx m1-path m1-doc]))

(def check-formats
  {"range"               check-format-range
   "money"               check-format-money
   "bank-account-number" check-format-bank-account-number
   "bank-sort-code"      check-format-bank-sort-code
   "telephone-number"    check-format-telephone-number})

;;------------------------------------------------------------------------------

(defn trace [m f]
  (fn [& args]
    (prn m "->" (rest args))
    (let [r (apply f args)]
      (prn m "<-" r)
      r)))
