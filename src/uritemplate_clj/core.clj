(ns uritemplate-clj.core
  (:require [ring.util.codec :as codec]))

(def ^String special-chars "/?#[]@!$&'()*+,;=")

(defn full-encode [^String s]
  (if s
    (codec/url-encode s)
    ""))

(defn partial-encode [^String s]
  (clojure.string/join 
   (map (fn[c] (if (>= (.indexOf special-chars (int c)) 0) c (codec/url-encode c))) s)))

(defrecord Token [text prefix])

(defn parse-token ^Token [token]
  (let
      [parts (re-find #"\{([\.#+/\.;\?\&])?([a-zA-Z0-9,_\*:]+)\}" token)]
    (->Token (nth parts 2) (nth parts 1))))

(defn split-variables [variable values separator encoding-fn]
  (clojure.string/join separator
     (map 
      #(encoding-fn 
        (values %))
      (clojure.string/split
       (:text variable) #","))))

(defn split-variables-with-vars [variable values separator]
  (clojure.string/join separator
     (map 
      #(str
        %  "=" 
        (partial-encode (values %)))
      (clojure.string/split
       (:text variable) #","))))

(defmulti fill-var 
  (fn [variable values]
    (:prefix variable)))

(defmethod fill-var "#" [variable values]
  "Fragment expansion with multiple variables"
  (str "#"  (split-variables variable values "," partial-encode)))

(defmethod fill-var "/" [variable values]
  "Path segments, slash-prefixed"
  (str "/"  (split-variables variable values "/" partial-encode)))

(defmethod fill-var "." [variable values]
  "Label expansion, dot-prefixed"
  (str "." (split-variables variable values "." partial-encode)))

(defmethod fill-var "+" [variable values]
  "Reserved string expansion does not convert (cf. 1.5): "
  (split-variables variable values "," partial-encode))

(defmethod fill-var "?" [variable values]
  "Form-style query, ampersand-separated"
  (str "?" (split-variables-with-vars variable values "&")))

(defmethod fill-var "&" [variable values]
  "Form-style query continuation"
  (str "&" (split-variables-with-vars variable values "&")))

(defmethod fill-var ";" [variable values]
  "Path-style parameters, semicolon-prefixed"
  ;;Special rule in 3.2.7: if a variable is empty, no = should be appended. So ;x=1024;y=768;empty and not ;x=1024;y=768;empty=
  (str ";" (split-variables-with-vars variable values ";")))

(defmethod fill-var :default [variable values]
  "Variable has no special modifier, so just apply simple string expansion"
   (split-variables variable values "," full-encode))

(defn tokenize [template]
  "Tokenize the template string, taken from https://bitbucket.org/dfa/uritemplate"
  (re-seq #"\{[^\{]+\}|[^{}]+" template)) 

(defn uritemplate [template values]
  "Take a URI template and a map of values and return the resulting URI"
  (clojure.string/join
   (map 
    (fn [token]
      (if (= \{ (first token))
        (fill-var (parse-token token) values)
        token))
    (tokenize template))))
