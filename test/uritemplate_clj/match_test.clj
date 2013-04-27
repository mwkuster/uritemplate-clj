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
    (is (= (match-variables template uri1) (set (list values))))
    (is (= (match-variables template uri2) #{}))
    (is (= (match-variables template uri3) #{}))))

(deftest ambiguous-level3-parses-test 
  (let
      [ambiguous-template "/foo{/ba,bar}{/baz,bay}"
       values1 {"ba" "x", "bar" "y", "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}]
    (is (= (match-variables ambiguous-template "/foo/x/y/z") (set (list values1 values2))))))

(deftest ambiguous-level4-parses-test 
  (let
      [ambiguous-template "/foo{/ba*}{/baz,bay}"
       values1 {"ba" '("x" "y"), "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}]
    (is (= (match-variables ambiguous-template "/foo/x/y/z") (set (list values1 values2))))))

(deftest find-constant-parts-test
  (let
      [template1 "http://www.example.org/bla/{var}/{hello}"
       uri1 "http://www.example.org/bla/v/h"
       template2 "http://www.example.org/bla/{var}/z/{hello}"
       uri2 "http://www.example.org/bla/v/z/h"
       template3 "http://www.example.org/bla/{var}/z/{hello}/a"
       uri3 "http://www.example.org/bla/v/z/h/a"
       template4 "http://www.example.org/bla/{var}/z/{hello}/a"
       uri4 "http://www.example.org/bla/v/z/h/x"]
    (is (= (find-constant-parts (tokenize template1) uri1) '( (0, 27) (28 29))))
    (is (= (find-constant-parts (tokenize template2) uri2)  '( (0, 27) (28 31))))
    (is (= (find-constant-parts (tokenize template3) uri3) '( (0, 27) (28 31) (32 34))))
    (is (= (find-constant-parts (tokenize template4) uri4) nil))))