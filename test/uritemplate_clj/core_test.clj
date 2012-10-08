(ns uritemplate-clj.core-test
  (:use clojure.test
        uritemplate-clj.core
        cheshire.core))

(deftest parse-token-test
  (is (= (parse-token "{var}") (->Token  "var" nil)))
  (is (= (parse-token "{/var}") (->Token "var" "/")))
  (is (= (parse-token "{/var:3}") (->Token "var:3" "/" )))
  (is (= (parse-token "{#var*}") (->Token "var*" "#")))
  (is (= (parse-token "{+var:7}") (->Token "var:7" "+"))))

(deftest parse-variable-test
  (is (= (parse-variable "var:5") (->Variable  "var" ":5")))
  (is (= (parse-variable "var*") (->Variable "var" "*")))
  (is (= (parse-variable "var") (->Variable "var" nil ))))

(def spec-examples 
  (cheshire.core/parse-stream (clojure.java.io/reader "test/uritemplate_clj/spec-examples.json")))

(defn- level-test [selection]
  (let
      [level (spec-examples selection)
       vars (level "variables")
       testcases (level "testcases")]
    (println selection)
    (doall 
     (for [tc testcases] 
       (is (= (uritemplate (first tc) vars) (second tc)))))))

(deftest level1-test (level-test "Level 1 Examples"))
(deftest level2-test (level-test "Level 2 Examples"))
(deftest level3-test (level-test "Level 3 Examples"))

(deftest level4-test
 (let
      [level (spec-examples "Level 4 Examples")
       vars (level "variables")
       testcases (level "testcases")]
    (doall 
     (for [tc testcases] 
       (let [res (uritemplate (first tc) vars)]
         (if (seq? (second tc))
           (is (some #(= res %) (second tc)))
           (is (= res (second tc)))))))))
