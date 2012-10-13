(ns uritemplate-clj.core
  (:require [ring.util.codec :as codec]
            [clojure.string :as cs]))

;Author: Marc Wilhelm Kuester
;Code releazed under the Eclipse Public License

(def ^String special-chars "/?#[]@!$&'()*+,;=")

(defn full-encode [^String s]
  (if s
    (codec/url-encode s)
    ""))

(defn partial-encode [^String s]
  (clojure.string/join 
   (map (fn[c] 
          (if (>= (.indexOf special-chars (int c)) 0) c (codec/url-encode c))) s)))

(defrecord Variable [text postfix])

(defn parse-variable ^Variable [variable]
  (let
      [parts (re-find #"([a-zA-Z0-9,_]+)(\*|:\d+)?" variable)]
    (->Variable (nth parts 1) (nth parts 2))))

(defrecord Token [variables prefix])

(defn parse-token ^Token [token]
  (let
      [parts (re-find #"\{([\.#+/\.;\?\&])?([a-zA-Z0-9,_\*:]+)\}" token)]
   
    (->Token (map parse-variable (clojure.string/split (nth parts 2) #",")) (nth parts 1))))



(defmulti handle-value 
  (fn [^Variable variable values separator encoding-fn]
    (class (values (:text variable)))))

(defmethod handle-value String [^Variable variable values separator encoding-fn]
  ;(println "Print string")
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
  ;(println "Print collection")
  (if (= (:postfix variable) "*")
    (map encoding-fn (values (:text variable)))
    (cs/join "," (map encoding-fn (values (:text variable))))))


(defmethod handle-value clojure.lang.IPersistentMap [^Variable variable values separator encoding-fn]
  ;(println "Print map")
  (if (= (:postfix variable) "*")
    (cs/join 
     (cond
      (= separator "/") "/"
      (= separator "&") "&"
      (= separator ";") ";"
      :else ",")
     (map #(str 
            (first %)
            "="
            (encoding-fn (second %)))  (values (:text variable))))
    (cs/join "," (map encoding-fn (mapcat identity (values (:text variable)))))))
  
(defmethod handle-value nil [variable values separator encoding-fn] nil)

(defmethod handle-value :default [variable values separator encoding-fn]
  (println "default handle value")
  (println variable)
  (println values)
  (println separator)
  "error-case")

(defn handle-variables 
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
  (fn [token values]
    (:prefix token)))

(defmethod handle-token "#" [token values]
  "Fragment expansion with multiple variables"
  (let
      [s
       (filter #(not (empty? %))
               (map 
                #(let
                     [res (handle-value % values "#" partial-encode)]         
                   (cs/join "," (if (string? res) (list res) res)))
                (:variables token)))]
    (if (not (empty? s))
      (str "#" (cs/join "," s)))))


(defmethod handle-token "/" [token values]
  "Path segments, slash-prefixed, cf. 3.2.6"
  (let
      [s
       (filter #(not (empty? %))
               (map 
                #(let
                     [res (handle-value % values "/" full-encode)]         
                   (cs/join "/" (if (string? res) (list res) res)))
                (:variables token)))]
    (if (not (empty? s))
      (str "/" (cs/join "/" s)))))


(defmethod handle-token "." [token values]
  "Label expansion, dot-prefixed, cf. 3.2.5"
  (let
      [s
       (filter #(not (empty? %))
               (map 
                #(let
                     [res (handle-value % values "." full-encode)]         
                   (cs/join "." (if (string? res) (list res) res)))
                (:variables token)))]
    (if (not (empty? s))
      (str "." (cs/join "." s)))))


(defmethod handle-token "+" [token values]
  "Reserved string expansion does not convert (cf. 1.5): "
  (let
      [s
       (filter #(not (empty? %))
               (map 
                #(let
                     [res (handle-value % values "." partial-encode)]         
                   (cs/join "," (if (string? res) (list res) res)))
                (:variables token)))]
    (if (not (empty? s))
      (cs/join "," s))))

(defn- build-= [variable values r] 
  (str (if (and (map? (values (:text variable))) (= (:postfix variable) "*")) "" (str (:text variable) "=")) r))

(defmethod handle-token "?" [token values]
  "Form-style query, ampersand-separated"
  (str
   "?"
   (cs/join "&"
    (map 
     #(let
          [res (handle-value % values "&" full-encode)]         
          (cs/join "&" (map (fn [r] (build-= % values r)) (if (string? res) (list res) res))))
   (:variables token)))))

(defmethod handle-token "&" [token values]
  "Form-style query continuation"
  (str
   "&"
   (cs/join "&"
    (map 
     #(let
          [res (handle-value % values "&" full-encode)]         
          (cs/join "&" (map (fn [r] (build-= % values r)) (if (string? res) (list res) res))))
   (:variables token)))))


(defmethod handle-token ";" [token values]
  "Path-style parameters, semicolon-prefixed"
  ;;Special rule in 3.2.7: if a variable is empty, no = should be appended. So ;x=1024;y=768;empty and not ;x=1024;y=768;empty=
  ;hack 
  (cs/replace
   (str
    ";"
    (cs/join ";"
             (map 
              #(let
                   [res (handle-value % values ";" full-encode)]         
                 (cs/join ";" (map (fn [r] (build-= % values r))  (if (string? res) (list res) res))))
              (:variables token)))) #"=$|=;" ""))

(defmethod handle-token :default [token values]
  "Variable has no special modifier, so just apply simple string expansion"
  (let
      [s
       (filter #(not (empty? %))
               (map 
                #(let
                     [res (handle-value % values "." full-encode)]         
                   (cs/join "," (if (string? res) (list res) res)))
                (:variables token)))]
    (if (not (empty? s))
      (cs/join "," s))))
;   (handle-variables token values "," full-encode))

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
