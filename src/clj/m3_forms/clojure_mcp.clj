;; Copyright 2025 Julian Gosnell
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns m3-forms.clojure-mcp
  (:require
   [nrepl.server :refer [start-server] :rename {start-server start-nrepl-server}]))

;; integrate nrepl and clojure-mcp in-vm together so that we:
;; a) only have to start a single process
;; b) don't have to be explicit about a port
;; c) avoid an annoying race condition at startup
;;
;; clojure-mcp is resolved at runtime (not a deps.edn dependency);
;; it's added to the classpath by bin/mcp-clojure-tools.sh

(defn -main []
  (let [{p :port} (start-nrepl-server)]
    (println "connecting clojure-mcp to nrepl on port:" p)
    ((requiring-resolve 'clojure-mcp.main/start-mcp-server) {:port p})))
