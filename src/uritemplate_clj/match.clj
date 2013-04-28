(ns uritemplate-clj.match
  (:use uritemplate-clj.core)
  (:require [ring.util.codec :as codec]
            [clojure.string :as cs]))

(defn find-constant-parts 
  "Find all the constants templates tokens in a given URI. Returns a list of (first last) index points for those in the URI"
  ;TODO: this implementation works only up to level 2, needs to be generalized for levels 3 and 4
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

(defmulti match-token
  "Match of an individual token"
  (fn [current-token remaining-tokens ^String rest-uri result-map]
    (if (string? current-token)
      (first current-token)
      nil)))

(defmethod match-token \{ [current-token remaining-tokens ^String rest-uri result-map]
  "Match a token containing a variable"
  (println "current-token (var):" current-token)
  (println "rest-uri (var):" rest-uri)
  (println "result-map (var):" result-map)
  (let
      [tok (parse-token current-token)]
    (cond
      (:prefix tok) {} ; can't do that yet
      (not (= (count (:variables tok)) 1)) {} ; can't do that yet
      :else
      (let
          [var (re-find #"^[a-zA-Z0-9\.%,_]+" rest-uri)] 
                                        ;this assumes that we can always parse up to the next separator. 
                                        ;Without this assumption no meaningful parsing seems possible
                                        ;Inded, templates of type /foo{hello}{world} are a class of non-decidable URI templates
        (if var
          (match-token  
           (first remaining-tokens) 
           (rest remaining-tokens) 
           (subs rest-uri (count var))
           (assoc result-map (:text (first (:variables tok))) var))
          {})))))

(defmethod match-token nil [current-token remaining-tokens ^String rest-uri result-map]
  "Match an empty current-token --> the parsing is over"
  (println "nil")
  (if (empty? rest-uri)
    result-map ; return the result-map only if the URI has been fully consumed, otherwise there is no hit
    {}))

(defmethod match-token :default  [current-token remaining-tokens ^String rest-uri result-map]
  "Match a constant token"
  (println "current-token (s):" current-token)
  (println "rest-uri (s):" rest-uri)
  (println "result-map (s):" result-map)
  (if (.startsWith rest-uri current-token)
    (match-token (first remaining-tokens) (rest remaining-tokens) (subs rest-uri (count current-token)) result-map)
    {}))

(defn match-variables [^String template ^String uri]
  "Find all the parses a given uri can have against a URI template. Return this as a set of maps (possibly empty)"
  (let
      [tokens (tokenize (cs/lower-case template))]
    (set
     (match-token (first tokens) (rest tokens) (cs/lower-case uri) {}))))

(defn matches? [^String template ^String uri]
  "Indicates if a given URI has at least one match against a URI template"
  (if (empty? (match-variables template uri))
    false
    true))