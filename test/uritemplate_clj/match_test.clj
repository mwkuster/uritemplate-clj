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
       values {"var" "v", "hello" "h"}
       uri4 "http://www.example.org/bla/v/h%20w"
       values2 {"var" "v", "hello" "h w"}
       uri5 "http://www.example.org/bla/6f56ae19-4032-4bb3-9c6f-1a0ce2fbd4c8/example"
       values3 {"var" "6f56ae19-4032-4bb3-9c6f-1a0ce2fbd4c8", "hello" "example"}]
    (is (= (match-variables template uri1) (set values)))
    (is (= (match-variables template uri2) #{}))
    (is (= (match-variables template uri3) #{}))
    (is (= (match-variables template uri4) (set values2)))
    (is (= (match-variables template uri5) (set values3)))))

(deftest match-variables-is-case-sensitive
  (let
    [template "http://www.example.org/{streamId}/"
     uri "http://www.example.org/6f56ae19-4032-4bb3-9c6f-1a0ce2fbd4c8/"
     values {"streamId" "6f56ae19-4032-4bb3-9c6f-1a0ce2fbd4c8"}]
    (is (= (match-variables template uri) (set values)))))

(deftest ambiguous-level3-parses-test
  (let
      [ambiguous-template "/foo{/ba,bar}{/baz,bay}"
       values1 {"ba" "x", "bar" "y", "baz" "z"}
       values2 {"ba" "x", "baz" "y", "bay" "z"}]
    (is (= (match-variables ambiguous-template "/foo/x/y/z") (set (list values1 values2))))))

;; The additional value to handle highly ambiguous level4 parses seems unjustified for the associated complexity
;; (deftest ambiguous-level4-parses-test
;;   (let
;;       [ambiguous-template "/foo{/ba*}{/baz,bay}"
;;        values1 {"ba" '("x" "y"), "baz" "z"}
;;        values2 {"ba" "x", "baz" "y", "bay" "z"}]
;;     (is (= (match-variables ambiguous-template "/foo/x/y/z") (set (list values1 values2))))))

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

(deftest matches?-test
  (let
      [template "http://www.example.org/bla/{var}/{hello}"
       uri1 "http://www.example.org/bla/v/h"
       uri2 "http://www.example.org/bla/v"
       uri3 "http://www.example.org/bla/v/h/z"
       values {"var" "v", "hello" "h"}]
    (is (matches? template uri1))
    (is (not (matches? template uri2)))
    (is (not (matches? template uri3)))))

(deftest matches?-with-no-template-vars-test
  (let
    [template "http://www.example.org/no-template-vars"
     uri1 "http://www.example.org/no-template-vars"
     uri2 "http://www.example.org/other"]
    (is (matches? template uri1))
    (is (not (matches? template uri2)))))

(deftest fill-with-nulls-test
  (let
      [template "http://www.example.org/bla/{var}/{hello}"
       template2 "http://www.example.org/bla/{var}/z/{hello}"
       template3 "/foo{/ba,bar}{/baz,bay}"]
    (is (= (fill-with-nulls template) "http://www.example.org/bla/%00/%00"))
    (is (= (fill-with-nulls template2) "http://www.example.org/bla/%00/z/%00"))
    (is (= (fill-with-nulls template3) "/foo/%00/%00/%00/%00"))))


(deftest uritemplate-compare-test
  (let
      [template "http://example.org/abc{/type}{/agent}{/year}{/natural_identifier,version,language}"]
    (is (= (uritemplate-compare template "http://example.org/abc/dir/ep/consil/2003/98") 0))
    (is (= (uritemplate-compare template "http://example.org/abcd/dir/ep/consil/2003/98") 1))
    (is (= (uritemplate-compare template "http://example.org/abb/dir/ep/consil/2003/98") -1))))
