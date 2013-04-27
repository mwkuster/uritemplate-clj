(ns uritemplate-clj.match
  (:require [ring.util.codec :as codec]
            [clojure.string :as cs]))

(defn find-parses [^String template ^String uri]
  "Find all the parses a given uri can have against a URI template. Return this as a set of maps (possibly empty)"
  #{}
  )