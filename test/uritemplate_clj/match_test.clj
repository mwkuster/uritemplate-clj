(ns uritemplate-clj.match-test
  (:use clojure.test
        uritemplate-clj.core
        uritemplate-clj.match
        cheshire.core))

(deftest simple-parses-test 
  (let
      [template "http://www.example.org/bla/{var}/{hello}"
       uri1 "http://www.example.org/bla/v/h"
       uri2 "http://www.example.org/bla/v"
       uri3 "http://www.example.org/bla/v/h/z"
       values {"var" "v", "hello" "h"}]
    (is (= (find-parses template uri1) (set (list values))))
    (is (= (find-parses template uri2) #{}))
    (is (= (find-parses template uri3) #{}))))

(deftest ambiguous-level3-parses-test 
  (let
      [ambiguous-template "/foo{/ba,bar}{/baz,bay}"
       values1 {"ba" "x", "bar" "y", "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}]
    (is (= (find-parses ambiguous-template "/foo/x/y/z") (set (list values1 values2))))))

(deftest ambiguous-level4-parses-test 
  (let
      [ambiguous-template "/foo{/ba*}{/baz,bay}"
       values1 {"ba" '("x" "y"), "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}]
    (is (= (find-parses ambiguous-template "/foo/x/y/z") (set (list values1 values2))))))