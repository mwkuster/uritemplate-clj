(ns uritemplate-clj.core-test
  (:use clojure.test
        uritemplate-clj.core
        cheshire.core))

(deftest parse-token-test
  (is (= (parse-token "{var}") (->Token  (list (->Variable "var" nil)) nil)))
  (is (= (parse-token "{/var}") (->Token (list (->Variable "var" nil)) "/")))
  (is (= (parse-token "{/var:3,var*}") (->Token (list (->Variable "var" ":3") (->Variable "var" "*")) "/" )))
  (is (= (parse-token "{#var*}") (->Token (list (->Variable "var" "*")) "#")))
  (is (= (parse-token "{+var:7}") (->Token (list (->Variable "var" ":7")) "+"))))

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
         (if (vector? (second tc))
           (is (some #(= res %) (second tc)))
           (is (= res (second tc)))))))))

(def extended-tests  (cheshire.core/parse-stream (clojure.java.io/reader "test/uritemplate_clj/extended-tests.json")))

(deftest extended-test
  (let
      [level (extended-tests "Additional Examples 1")
       vars (level "variables")
       testcases (level "testcases")]
    (doall 
     (for [tc testcases] 
       (let [res (uritemplate (first tc) vars)]
         (println res)
         (if (vector? (second tc))
           (is (some #(= res %) (second tc)))
           (is (= res (second tc)))))))))

(deftest additional-test
  (let
      [template1 "abc{/type}{/agent*}{/year}{/natural_identifier,version,language}"
       template2 "abc{/type}/{agent*}{/year}{/natural_identifier,version,language}"
       values1 {"type" "dir", 
               "agent"  ["ep" "consil"], 
               "year"  "2003",
               "natural_identifier" "98",
               "version" "R3",
               "language" "SPA"}
       values2 {"type" "dir", 
               "agent"  ["ep" "consil"], 
               "year"  "2003",
               "natural_identifier" "98"}
       values3 {"type" "dir", 
                "year"  "2003",
                "natural_identifier" "98",
                "version" "R3",
                "language" "SPA"}
       values4 {"type" "dir", 
                "agent"  ["ep" "consil"], 
                "year"  "2003",
                "natural_identifier" "98",
                "language" "SPA"}]
    (is (= (uritemplate template1 values1) "abc/dir/ep/consil/2003/98/R3/SPA"))
    (is (= (uritemplate template1 values2) "abc/dir/ep/consil/2003/98"))
    (is (= (uritemplate template1 values3) "abc/dir/2003/98/R3/SPA"))
    (is (= (uritemplate template1 values4) "abc/dir/ep/consil/2003/98/SPA"))
    (is (= (uritemplate template2 values4) "abc/dir/ep,consil/2003/98/SPA"))))
    
