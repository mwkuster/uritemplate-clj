(ns uritemplate-clj.core
  (:require [ring.util.codec :as codec]))

(def ^String special-chars "/?#[]@!$&'()*+,;=")

(defn full-encode [^String s]
  (if s
    (codec/url-encode s)
    ""))

(defn partial-encode [^String s]
  (clojure.string/join 
   (map (fn[c] 
          (if (>= (.indexOf special-chars (int c)) 0) c (codec/url-encode c))) s)))

(defrecord Token [text prefix])

(defn parse-token ^Token [token]
  (let
      [parts (re-find #"\{([\.#+/\.;\?\&])?([a-zA-Z0-9,_\*:]+)\}" token)]
    (->Token (nth parts 2) (nth parts 1))))

(defrecord Variable [text postfix])

(defn parse-variable ^Variable [variable]
  (let
      [parts (re-find #"([a-zA-Z0-9,_]+)(\*|:\d+)?" variable)]
    (->Variable (nth parts 1) (nth parts 2))))

(defmulti handle-value 
  (fn [^Variable variable values separator encoding-fn]
    (class (values (:text variable)))))

(defmethod handle-value String [^Variable variable values separator encoding-fn]
  (println "Print string")
  (encoding-fn 
   (values (:text variable))))

(defmethod handle-value java.util.Collection [^Variable variable values separator encoding-fn]
  (println "Print collection")
  (if (= (:postfix variable) "*")
    (clojure.string/join separator (map #(str (:text variable) "=" (encoding-fn %)) (values (:text variable))))
    (clojure.string/join "," (map encoding-fn (values (:text variable))))))

(defmethod handle-value clojure.lang.IPersistentMap [^Variable variable values separator encoding-fn]
  (println "Print map")
  (clojure.string/join ","  
                       (map encoding-fn (keys (values (:text variable))))))

(defmethod handle-value :default [variable values separator encoding-fn]
  (println "default handle value")
  (println variable)
  (println values)
  (println separator)
  "abc")

;; (defn handle-value [var values ]
;;   "Handle value(s) for this variable"
;;   (let
;;       [val (values var)]
;;     (if (vector? val)
;;       val
;;       (vector val))))

(defn split-variables [variable values separator encoding-fn]
  (clojure.string/join separator
     (map
      #(handle-value (parse-variable %) values separator encoding-fn)
      (clojure.string/split
       (:text variable) #","))))



(defn split-variables-with-vars [variable values separator]
  (clojure.string/join separator
     (map 
      #(let 
            [var (parse-variable %)]
         (handle-value (assoc var :postfix "*") values separator partial-encode))
      (clojure.string/split
       (:text variable) #","))))

(defmulti handle-token 
  (fn [variable values]
    (:prefix variable)))

(defmethod handle-token "#" [variable values]
  "Fragment expansion with multiple variables"
  (str "#"  (split-variables variable values "," partial-encode)))

(defmethod handle-token "/" [variable values]
  "Path segments, slash-prefixed"
  (str "/"  (split-variables variable values "/" partial-encode)))

(defmethod handle-token "." [variable values]
  "Label expansion, dot-prefixed"
  (str "." (split-variables variable values "." partial-encode)))

(defmethod handle-token "+" [variable values]
  "Reserved string expansion does not convert (cf. 1.5): "
  (split-variables variable values "," partial-encode))

(defmethod handle-token "?" [variable values]
  "Form-style query, ampersand-separated"
  (str "?" (split-variables-with-vars variable values "&")))

(defmethod handle-token "&" [variable values]
  "Form-style query continuation"
  (str "&" (split-variables-with-vars variable values "&")))

(defmethod handle-token ";" [variable values]
  "Path-style parameters, semicolon-prefixed"
  ;;Special rule in 3.2.7: if a variable is empty, no = should be appended. So ;x=1024;y=768;empty and not ;x=1024;y=768;empty=
  (str ";" (split-variables-with-vars variable values ";")))

(defmethod handle-token :default [variable values]
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
        (handle-token (parse-token token) values)
        token))
    (tokenize template))))
