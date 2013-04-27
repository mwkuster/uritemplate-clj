(ns uritemplate-clj.match
  (:use uritemplate-clj.core)
  (:require [ring.util.codec :as codec]
            [clojure.string :as cs]))

(defn find-constant-parts 
  "Find all the constants templates tokens in a given URI. Returns a list of (first last) index points for those in the URI"
  ([token-list ^String uri]
     (find-constant-parts token-list uri nil 0))
  ([token-list ^String uri result-list index-last-hit]
     (let
         [constants (filter #(not (= \{ (first %))) token-list)]
       (if (empty? constants)
         (reverse result-list)
         (let
             [const (first constants)
              next-hit (.indexOf uri const index-last-hit)]
           (if (> next-hit -1)
             (find-constant-parts (rest constants) uri (conj result-list (list next-hit (+ next-hit (count const)))) (+ next-hit (count const)))
             nil ;some constant token cannot be matched against the URI -> there is no hit
             ))))))
    

(defn match-token [token-list ^String partial-uri & vars]
  "Try to match an element of a token list against a partial uri"
  (println "token-list:" token-list)
  (println "partial-uri:" partial-uri)
  (println "vars:" vars)
  (let
      [token (first token-list)]
    (if (empty? token-list)
      vars
      (if (= \{ (first token))
        (let
            [tok (parse-token token)
             uri-fragments (re-seq #"[/]?([^/]+)/(.*)" partial-uri)
             first-fragment (first uri-fragments)]
          (println "uri-fragments:!" uri-fragments "!")
          (println "first-fragment:!" first-fragment "!")
          (match-token (rest token-list) (second(conj vars first-fragment) ))
        (match-token (rest token-list) (cs/replace-first partial-uri token "") vars))))))
  
(defn match-variables [^String template ^String uri]
  "Find all the parses a given uri can have against a URI template. Return this as a set of maps (possibly empty)"
  (let
      [tokens (tokenize template)]
    (match-token tokens uri)
   #{}
  ))