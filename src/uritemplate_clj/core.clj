(ns uritemplate-clj.core
  (:require [ring.util.codec :as codec]
            [clojure.string :as cs]
            [clojure.walk :as walk]))

;Author: Marc Wilhelm Kuester
;Code releazed under the Eclipse Public License

(def ^String special-chars "/?#[]@!$&'()*+,;=")

(defn full-encode [^String s]
  (if s
    (codec/url-encode s)
    ""))

(defn form-encode [^String s]
  (if s
    (cs/replace (codec/form-encode s) #"\+" "%20")
    ""))

(defn partial-encode [^String s]
  (clojure.string/join 
   (map (fn[c] 
          (if (>= (.indexOf special-chars (int c)) 0) c (codec/url-encode c))) s)))

(defrecord Variable [text postfix])

(defn parse-variable ^Variable [variable]
  (let
      [parts (re-find #"([a-zA-Z0-9\.%,_]+)(\*|:\d+)?" variable)]
    (->Variable (nth parts 1) (nth parts 2))))

(defrecord Token [variables prefix])

(defn parse-token ^Token [token]
  (let
      [parts (re-find #"\{([\.#+/\.;\?\&])?([a-zA-Z0-9\.%,_\*:]+)\}" token)]
   
    (->Token (map parse-variable (clojure.string/split (nth parts 2) #",")) (nth parts 1))))

(defmulti handle-value 
  "Handling of individual variables. Returns the variable replaced by the supplied values"
  (fn [^Variable variable values separator encoding-fn]
    (class (values (:text variable)))))

(defmethod handle-value String [^Variable variable values separator encoding-fn]
  (encoding-fn 
   (if (= (first (:postfix variable)) \:)
     (let
         [to-pos (Integer/parseInt (subs (:postfix variable) 1))
          res-val (values (:text variable))]
     (subs
      (values (:text variable))
      0 (if (< to-pos (count res-val)) to-pos (count res-val))))
     (values (:text variable)))))

(defmethod handle-value java.util.Collection [^Variable variable values separator encoding-fn]
  (if (= (:postfix variable) "*")
    (map encoding-fn (values (:text variable)))
    (cs/join "," (map encoding-fn (values (:text variable))))))


(defmethod handle-value clojure.lang.IPersistentMap [^Variable variable values separator encoding-fn]
  (if (= (:postfix variable) "*")
    (cs/join 
     (cond
      (= separator "/") "/"
      (= separator "&") "&"
      (= separator ";") ";"
      :else ",")
     ;The handling of maps are rather tricky, as here they map keys
     ;themselves become keys if the postfix is "*". However, the
     ;variable name stays key without that postfix
     (map #(str 
            (encoding-fn (first %))
            "="
            (encoding-fn (second %)))  (values (:text variable))))
    (cs/join "," (map encoding-fn (mapcat identity (values (:text variable)))))))
  
(defmethod handle-value nil [variable values separator encoding-fn] nil)

(defmethod handle-value Number [variable values separator encoding-fn] (values (:text variable)))

(defmethod handle-value java.util.UUID [variable values separator encoding-fn]
  (-> values (get (:text variable)) str))

(defmethod handle-value :default [variable values separator encoding-fn]
  (println "default handle value")
  (println variable)
  (println values)
  (println separator)
  "error-case")

(defn handle-variables 
  "Method calls handle-value for each variable named in the token"
  ([token values separator] (handle-variables token values separator partial-encode))
  ([token values separator encoding-fn ]
     (let
         [res (filter string?
                      (map
                       #(handle-value % values separator encoding-fn)
                       (:variables token)))]
       (if (not (empty? res))
         (clojure.string/join separator res)))))


(defmulti handle-token 
  "Takes a token and replaces its variables with the supplied values"
  (fn [token values]
    (:prefix token)))

(defn- process-token [token values separator first-char encoding-fn list-generator]
  (let
       [res
        (filter
         (fn [r] (not (empty? r)))
         (map 
          #(let
               [res (handle-value % values separator encoding-fn)]         
             (cs/join separator
                      (list-generator % values res)))
          (:variables token)))]
     (if (not (empty? res))
       (str
        first-char
        (cs/join separator res)))))

(defn- unnamed-list-generator [var values r]
  (cond
   (coll? r) r 
   (nil? r) nil
   :else (list r)) )

(defn- process-unnamed-token [token values separator first-char encoding-fn]
  (process-token token values separator first-char encoding-fn unnamed-list-generator))

(defmethod handle-token "." [token values]
  "Label expansion, dot-prefixed, cf. 3.2.5"
  (process-unnamed-token token values "." "." full-encode))

(defmethod handle-token "/" [token values]
  "Path segments, slash-prefixed, cf. 3.2.6"
  (process-unnamed-token token values "/" "/" full-encode))

(defmethod handle-token "#" [token values]
  "Fragment expansion with multiple variables"
  (process-unnamed-token token values "," "#" partial-encode))

(defmethod handle-token "+" [token values]
  "Reserved string expansion does not convert (cf. 1.5): "
  (process-unnamed-token token values "," "" partial-encode))

(defn- build-= [variable values r] 
  (str (if (and (map? (values (:text variable))) (= (:postfix variable) "*")) "" (str (:text variable) "=")) r))

(defn- named-list-generator [var values r]
  (map (fn[r] (build-= var values r))
       (cond
        (coll? r) r 
        (nil? r) nil
        :else (list r)) ))


(defn- process-named-token [token values separator first-char]
  (if (= separator "&")
    (process-token token values separator first-char form-encode named-list-generator)
    (process-token token values separator first-char full-encode named-list-generator)))

(defmethod handle-token "?" [token values]
  "Form-style query, ampersand-separated"
  (process-named-token token values "&" "?"))

(defmethod handle-token "&" [token values]
  "Form-style query continuation"
  (process-named-token token values "&" "&"))

(defmethod handle-token ";" [token values]
  "Path-style parameters, semicolon-prefixed"
  ;;Special rule in 3.2.7: if a variable is empty, no = should be appended. So ;x=1024;y=768;empty and not ;x=1024;y=768;empty=
  ;hack 
  (cs/replace
   (process-named-token token values ";" ";")
   #"=$|=;" ""))

(defmethod handle-token :default [token values]
  "Variable has no special modifier, so just apply simple string expansion"
  (process-unnamed-token token values "," "" full-encode))


(defn tokenize [template]
  "Tokenize the template string, taken from https://bitbucket.org/dfa/uritemplate"
  (re-seq #"\{[^\{]+\}|[^{}]+" template)) 

(defn uritemplate ^String [^String template ^clojure.lang.IPersistentMap values]
  "Take a URI template and a map of values and return the resulting URI"
  (let [stringly-values (walk/stringify-keys values)]
    (clojure.string/join
     (map
      (fn [token]
        (if (= \{ (first token))
          (handle-token (parse-token token) stringly-values)
          token))
      (tokenize template)))))
