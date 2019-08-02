uritemplate-clj
===============

Clojure implementation of URI Templates as specified in RFC 6570 (http://tools.ietf.org/html/rfc6570), compliant to level 4

## `uritemplate`
uritemplate-clj exposes first and foremost the function `uritemplate`, taking the URI template and a map of values as input and returning the filled in template:

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

## Additional functions
### `match-variables`
`match-variables` is the inverse to `uritemplate`. Given a level 1 or 2 URI template and a URI, it finds all the parses a given uri can have against this URI template. Return this as a set of maps (possibly empty).

```clojure
user=> (ns test (:require [uritemplate-clj.match :as match]))
nil
test=> (match/match-variables "http://example.org/abc{/type}{/agent}{/year}{/natural_identifier,version,language}" "http://example.org/abc/dir/ep/consil/2003/98")
#{{"type" "dir", "agent" "ep", "year" "consil", "natural_identifier" "2003", "version" "98"}}
test=> 
```


### `uritemplate-compare`
`uritemplate-compare`, when called with a URI template and a URI, returns whether it is matched or not. Returns 0 if the uri matches the template, -1 if the template give a
canonicial form is less than the URI in terms of string comparison, +1
if it is more

The function assumes that all values in the template are filled with ASCII %00 for comparision (canonical URI representation of the template).

```clojure
user=> (ns test (:require [uritemplate-clj.match :as match]))
nil
test=> (match/uritemplate-compare "http://example.org/abc{/type}{/agent}{/year}{/natural_identifier,version,language}" "http://example.org/abc/dir/ep/consil/2003/98")
0
test=> (match/uritemplate-compare "http://example.org/abc{/type}{/agent}{/year}{/natural_identifier,version,language}" "http://example.org/abcd/dir/ep/consil/2003/98")
1
test=> (match/uritemplate-compare "http://example.org/abc{/type}{/agent}{/year}{/natural_identifier,version,language}" "http://example.org/abb/dir/ep/consil/2003/98")
-1
```


## Usage

```clojure
[uritemplate-clj "1.2.2"]

;; In your ns statement:
(ns my.ns
  (:require [uritemplate-clj.core)
```
## On Clojars
The binary version of this library is distributed via Clojars (https://clojars.org/uritemplate-clj)

[![Clojars Project](https://img.shields.io/clojars/v/uritemplate-clj.svg)](https://clojars.org/uritemplate-clj)
