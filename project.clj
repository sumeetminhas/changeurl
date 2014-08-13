(defproject grub "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2277"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [http-kit "2.1.18"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.0" :exclusions [org.clojure/tools.reader]]
                 [ring/ring-devel "1.3.0" :exclusions [org.clojure/tools.reader]]
                 [hiccup "1.0.5"]
                 [prismatic/dommy "0.1.2"]
                 [com.novemberain/monger "2.0.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [clj-webdriver "0.6.1" :exclusions [org.clojure/core.cache]]
                 [om "0.7.0"]
                 [sablono "0.2.17"]
                 [cljs-uuid "0.0.4"]
                 [net.polyc0l0r/hasch "0.2.3"]]
  :profiles {:uberjar {:aot :all}}
  :min-lein-version "2.1.2"
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.6"]
            [com.keminglabs/cljx "0.4.0"]]
  :cljsbuild {:builds {:dev {:source-paths ["src/cljs" "target/generated/cljs"]
                             :compiler {:output-dir "public/js/out"
                                        :output-to "public/js/grub.js"
                                        :optimizations :none
                                        :source-map true}}
                       :prod {:source-paths ["src/cljs" "target/generated/cljs"]
                              :compiler {:output-to "public/js/grub.min.js"
                                         :optimizations :advanced
                                         :pretty-print false
                                         :preamble ["react/react.min.js"]
                                         :externs ["react/externs/react.js"]}}}}
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]}
  :hooks [cljx.hooks]
  :source-paths ["src/clj" "src/test"]
  :test-paths ["spec/clj"]
  :ring {:handler grub.core/app}
  :uberjar-name "grub-standalone.jar"
  :main grub.core)
