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

(ns m3-forms.json
  (:require
   #?(:clj  [cheshire.core :as cheshire]
      :cljs [cljs.core :as cljs])))

(defn with-array-map-as-vec [m f]
  (apply array-map (mapcat identity (f (vec m)))))

(defn json-update
  "like update but:
  - stretches vectors
  - tries to keeps maps in array-map state"
  [c k f & args]
  (cond
    (nil? c)
    (if (integer? k)
      (apply update (vec (repeat k nil)) k f args)
      (array-map k (apply f nil args)))

    (vector? c)
    (if (< (count c) k)
      (apply update (vec (concat c (repeat (- k (count c)) nil))) k f args)
      (apply update c k f args))

    (map? c)
    (if (< (count c) 8)
      (apply update c k f args)
      (with-array-map-as-vec c (fn [v] (conj v [k (apply f (c k) args)]))))

    :else
    (throw (ex-info "help" {:collection c :key k}))))

(defn json-assoc [c k v]
  (json-update c k (constantly v)))

(defn insertv
  "insert value (v) into vector (c) at index (i)"
  [c i v]
  (into (conj (subvec c 0 i) v) (subvec c i)))

(defn deletev
  "delete value from vector (c) at index (i)"
  [c i]
  (into (subvec c 0 i) (subvec c (inc i))))

(defn json-remove
  "remove property with key (k) from json object (o)"
  [o k]
  (if (integer? k)
    (deletev o k)
    (dissoc o k)))

(defn json-update-in
  "like update-in but uses json-update instead of update"
  [c [h & t] f & args]
  (if (seq t)
    (let [v (apply json-update-in (get c h) t f args)]
      (if (seq v)
        (json-assoc c h v)
        (json-remove c h)))
    (if h
      (apply json-update c h f args)
      (apply f c args))))

(defn json-assoc-in
  "append/update value (v) at path (p) in json object (o)"
  [o p v]
  (json-update-in o p (constantly v)))

(defn json-rekey
  "change keys of json object (o) using function (f)"
  [o f]
  (with-array-map-as-vec o (fn [ps] (mapv (fn [[k v]] [(f k) v]) ps))))

(defn json-rename
  "in json object (o), rename old-k to new-k"
  [o old-k new-k]
  (json-rekey o (fn [k] (if (= k old-k) new-k k))))

(defn json-rename-in
  [c p old-k new-k]
  (json-update-in c p (fn [m] (json-rename m old-k new-k))))

(defn json-insert
  "insert k-value-pair (kv) into json object (o) at index (i)"
  [o i kv]
  (with-array-map-as-vec o (fn [ps] (insertv ps i kv))))

(defn json-insert-in
  "insert k-value-pair (kv) into json object (o) at path [p] at index (i)"
  [o p i kv]
  (json-update-in o p (fn [m] (json-insert m i kv))))

(defn json-remove-in [o p]
  (json-update-in o (butlast p) (fn [m] (json-remove m (last p)))))

;;------------------------------------------------------------------------------

(def absent :absent)

(defn absent? [v]
  (= absent v))

(defn present? [v]
  (not (absent? v)))

;;------------------------------------------------------------------------------

(defn decode [s]
  #?(:clj
     (cheshire/decode s)
     :cljs
     (cljs/js->clj (js/JSON.parse s) :keywordize-keys false)))
