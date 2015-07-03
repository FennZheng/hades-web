(ns hades-web.conf
  (:require [clojure.java.io :as io]
            [hades-web.util :as u])
  (:import [java.io File PushbackReader]))

(defn- valid-conf-file?
  "Check if a file exists and is a normal file"
  [path]
  (let [file (File. path)]
    (and (.exists file)
         (.isFile file))))

(defn- load-conf-file [path]
  (when (valid-conf-file? path)
    (read-string (slurp path :encoding "utf-8"))))

(defn load-conf []
  "load the config from ~/.hades-web-conf.clj or conf/hades-web-conf.clj"
  (let [home-conf (str (System/getenv "HOME") File/separator ".hades-web-conf.clj")
        pwd-conf "conf/hades-web-conf.clj"
        env-port (u/str->int (System/getenv "PORT"))
        conf     (or (load-conf-file home-conf) (load-conf-file pwd-conf)
                  {
                   :server-port 9011
                   :users {"admin" "hermes_hades"}
                   :default-node "192.168.16.235:2181/hades"
                  })]
        (if env-port
          (assoc conf :server-port env-port)
          conf)))

