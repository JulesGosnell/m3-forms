#!/bin/sh

cd "$(dirname "$0")/.."

# clojure-mcp is a git dep (not on Maven/Clojars), so we compute its
# classpath via Clojure CLI and merge it with the project classpath.

MCP_CP=$(clojure -Sdeps '{:deps {io.github.bhauman/clojure-mcp {:git/tag "v0.2.3" :git/sha "bbefc7a"}}}' -Spath)
PROJECT_CP=$(clojure -Spath -Sdeps '{:paths ["src/clj" "src/cljc"] :deps {nrepl/nrepl {:mvn/version "1.3.0"}}}')

java -cp "$PROJECT_CP:$MCP_CP" clojure.main -m m3-forms.clojure-mcp
