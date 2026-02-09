(ns m3-forms.json-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [m3-forms.json :as json]))

;;------------------------------------------------------------------------------
;; insertv / deletev

(deftest insertv-test
  (testing "insert at beginning"
    (is (= [:a :b :c] (json/insertv [:b :c] 0 :a))))
  (testing "insert in middle"
    (is (= [:a :x :b :c] (json/insertv [:a :b :c] 1 :x))))
  (testing "insert at end"
    (is (= [:a :b :c] (json/insertv [:a :b] 2 :c)))))

(deftest deletev-test
  (testing "delete from beginning"
    (is (= [:b :c] (json/deletev [:a :b :c] 0))))
  (testing "delete from middle"
    (is (= [:a :c] (json/deletev [:a :b :c] 1))))
  (testing "delete from end"
    (is (= [:a :b] (json/deletev [:a :b :c] 2)))))

;;------------------------------------------------------------------------------
;; json-update

(deftest json-update-nil-collection
  (testing "nil collection with string key creates array-map"
    (let [result (json/json-update nil "a" (constantly 1))]
      (is (= {"a" 1} result))
      (is (map? result))))
  (testing "nil collection with integer key creates vector"
    (let [result (json/json-update nil 2 (constantly :x))]
      (is (= [nil nil :x] result))
      (is (vector? result)))))

(deftest json-update-vector
  (testing "update existing index"
    (is (= [10 2 3] (json/json-update [1 2 3] 0 (constantly 10)))))
  (testing "stretch vector when key > count"
    (let [result (json/json-update [1] 3 (constantly :x))]
      (is (= [1 nil nil :x] result)))))

(deftest json-update-map
  (testing "update existing key in small map"
    (is (= {"a" 10} (json/json-update {"a" 1} "a" (constantly 10)))))
  (testing "add new key to small map"
    (is (= {"a" 1 "b" 2} (json/json-update {"a" 1} "b" (constantly 2)))))
  (testing "preserves array-map for large maps (>= 8 keys)"
    (let [m (apply array-map (mapcat (fn [i] [(str i) i]) (range 10)))
          result (json/json-update m "new" (constantly :v))]
      (is (= :v (get result "new")))
      ;; key order preserved — "new" is appended at end
      (is (= "new" (last (keys result)))))))

;;------------------------------------------------------------------------------
;; json-assoc-in

(deftest json-assoc-in-test
  (testing "set nested value in nil"
    (is (= {"a" {"b" 1}} (json/json-assoc-in nil ["a" "b"] 1))))
  (testing "set nested value in existing map"
    (is (= {"a" {"b" 2}} (json/json-assoc-in {"a" {"b" 1}} ["a" "b"] 2))))
  (testing "create intermediate maps"
    (is (= {"x" {"y" {"z" 3}}} (json/json-assoc-in nil ["x" "y" "z"] 3)))))

;;------------------------------------------------------------------------------
;; json-rename

(deftest json-rename-test
  (testing "rename preserves order"
    (let [m (apply array-map ["a" 1 "b" 2 "c" 3])
          result (json/json-rename m "b" "B")]
      (is (= {"a" 1 "B" 2 "c" 3} result))
      (is (= ["a" "B" "c"] (vec (keys result))))))
  (testing "rename non-existent key is a no-op"
    (let [m (array-map "a" 1)
          result (json/json-rename m "z" "Z")]
      (is (= {"a" 1} result)))))

;;------------------------------------------------------------------------------
;; json-insert / json-insert-in

(deftest json-insert-test
  (testing "insert at beginning"
    (let [m (array-map "b" 2 "c" 3)
          result (json/json-insert m 0 ["a" 1])]
      (is (= ["a" "b" "c"] (vec (keys result))))))
  (testing "insert at end"
    (let [m (array-map "a" 1 "b" 2)
          result (json/json-insert m 2 ["c" 3])]
      (is (= ["a" "b" "c"] (vec (keys result)))))))

(deftest json-insert-in-test
  (testing "insert into nested map"
    (let [m {"outer" (array-map "a" 1 "c" 3)}
          result (json/json-insert-in m ["outer"] 1 ["b" 2])]
      (is (= ["a" "b" "c"] (vec (keys (get result "outer"))))))))

;;------------------------------------------------------------------------------
;; json-remove-in

(deftest json-remove-in-test
  (testing "remove nested key"
    (is (= {"a" {"c" 3}} (json/json-remove-in {"a" {"b" 2 "c" 3}} ["a" "b"]))))
  (testing "remove from vector"
    (is (= {"a" [1 3]} (json/json-remove-in {"a" [1 2 3]} ["a" 1])))))

;;------------------------------------------------------------------------------
;; absent? / present?

(deftest absent-present-test
  (testing "absent sentinel"
    (is (json/absent? json/absent))
    (is (json/absent? :absent)))
  (testing "present values"
    (is (json/present? nil))
    (is (json/present? 0))
    (is (json/present? ""))
    (is (json/present? false)))
  (testing "absent is not present"
    (is (not (json/present? json/absent)))))
