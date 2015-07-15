(defproject hades-web "0.1.1"
            :description "FIXME: write this!"
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [noir "1.3.0-beta3"]
                           [com.netflix.curator/curator-framework "1.1.16"]
                           [com.netflix.curator/curator-test "1.1.16"]
                           [com.alibaba/fastjson "1.2.6"]
                           [clojurewerkz/quartzite "2.0.0"]]
            :main hades-web.server)
