(defproject uritemplate-clj "0.5.0-SNAPSHOT"
  :description "Clojure implementation of URI Templates as specified in RFC 6570 (http://tools.ietf.org/html/rfc6570), currently level 3 compatible"
  :url "https://github.com/mwkuster/uritemplate-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "4.0.3"]
                 [ring "1.1.6" :exclusions [org.clojure/clojure]]])