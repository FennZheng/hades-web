(ns hades-web.export
  (:require [noir.session :as session]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.response :as resp]
            [hades-web.zk :as zk])
  (:use hades-web.log)
  (:import [java.nio.charset Charset]))

(def backup-dir "backup/")

(defn get-zip-path
  [zip-name]
  (str backup-dir zip-name))

(defn generate-zip-name [node-path]
  ; replace "/" when node is root(/)
  (let [node-path (str/replace node-path "/" "_")]
    (str "backup_" node-path (.format (java.text.SimpleDateFormat. "_yyyyMMdd_HH_mm_ss_SSS") (java.util.Date.)))))

(defn zip-dir
  [zip-name]
  (shell/with-sh-dir "backup/"
    (let [zip-name-with-ext (str zip-name ".zip")]
      (shell/sh "zip" "-r" zip-name-with-ext zip-name)
      (shell/sh "rm" "-rf" zip-name)
    )))

(defn list-backup
  []
  (oper-log "list-backup")
  (shell/with-sh-dir backup-dir
    (str/split-lines(:out (shell/sh "ls" "-t")))))

(defn download-backup
  "Download a backup zip file"
  [zip-name]
  (oper-log (str "download-backup:" zip-name))
  (resp/file-response (get-zip-path zip-name)))

(defn node-to-file
  "Dump node data to a xx.json"
  [cli node-path args]
  (let
    [zip-root args
     file-full-path (zk/concat-path zip-root node-path)
     file-name (str file-full-path ".json")
     bytes (get cli node-path)]
    (io/make-parents file-full-path)
    (if (not bytes)
      (spit file-name "")
      (spit file-name (String. bytes)))))

(defn export-children-as-zip
  "Export/zip/download"
  [cli node-path]
  (let
    [zip-name (generate-zip-name node-path)
     zip-root (get-zip-path zip-name)]
    (zk/recur-child cli node-path node-to-file zip-root)
    (zip-dir zip-name)
    (resp/header
      (resp/file-response (str zip-root ".zip"))
      "Content-Disposition"
      (str "attachment; filename=" zip-name ".zip"))))

(defn export-zip
  "Export node data recursively as a zip file"
  [cli path]
  (oper-log (str "export:" path))
  (export-children-as-zip cli path))