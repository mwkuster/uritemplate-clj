(defproject uritemplate-clj "1.4.0"
  :description "Clojure implementation of URI Templates as specified in RFC 6570 (http://tools.ietf.org/html/rfc6570), level 4 compliant"
  :url "https://github.com/mwkuster/uritemplate-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :comment "Author: Marc Wilhelm Küster"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [cheshire "6.2.0"]
                 [ring/ring-codec "1.3.0" :exclusions [org.clojure/clojure]]])
