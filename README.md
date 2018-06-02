uritemplate-clj
===============

Clojure implementation of URI Templates as specified in RFC 6570 (http://tools.ietf.org/html/rfc6570), compliant to level 4

The binary version of this library is distributed via Clojars (https://clojars.org/uritemplate-clj)

uritemplate-clj exposes a single method, uritemplate, taking the URI template and a map of values as input and returning the filled in template:

```clojure
user=> (ns test
  #_=> (:require [uritemplate-clj.core :as templ]))
nil
test=> (templ/uritemplate "http://example.org/abc{/type}{/agent*}{/year}{/natural_identifier,version,language}" {"type" "dir", 
  #_=>                "agent"  ["ep" "consil"], 
  #_=>                "year"  "2003",
  #_=>                "natural_identifier" "98"})
"http://example.org/abc/dir/ep/consil/2003/98"
test=> 
```

## Usage

```clojure
[uritemplate-clj "1.1.1"]

;; In your ns statement:
(ns my.ns
  (:require [uritemplate-clj.core)
```
## On Clojars
[![Clojars Project](https://img.shields.io/clojars/v/uritemplate-clj.svg)](https://clojars.org/uritemplate-clj)
