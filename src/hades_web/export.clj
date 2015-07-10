(ns hades-web.export
  (:require [noir.session :as session]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.response :as resp])
  (:import [java.nio.charset Charset]))

(defn zip-dir
  [dir-name]
  (shell/with-sh-dir "backup/"
    (let [zip-name (str dir-name ".zip")]
      (shell/sh "zip" "-r" zip-name dir-name)
      (shell/sh "rm" "-rf" dir-name)
    )))

(defn list-backup
  []
  (shell/with-sh-dir "backup/"
    (str/split-lines(:out (shell/sh "ls" "-t")))))

(defn download-backup
  [zip-name]
  (println zip-name)
  (resp/file-response (str "backup/" zip-name)))
