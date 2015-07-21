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

(defn generate->dumpfile-root-path
  [zip-name]
  (str backup-dir zip-name))

(defn generate->zip-name [node-path]
  ; replace "/" when node is root(/)
  (let [node-path (str/replace node-path "/" "_")]
    (str "backup_" node-path (.format (java.text.SimpleDateFormat. "_yyyyMMdd_HH_mm_ss_SSS") (java.util.Date.)))))

(defn generate->zip-file-path
  [zip-name]
  ; zip-name with/without ext name
  (str/replace (str backup-dir zip-name ".zip") ".zip.zip" ".zip"))

(defn zip-dir
  [zip-name]
  (shell/with-sh-dir "backup/"
    (let [zip-name-with-ext (str zip-name ".zip")]
      (shell/sh "zip" "-r" zip-name-with-ext zip-name)
      (shell/sh "rm" "-rf" zip-name)
    )))

(defn list-backup
  []
  #_(oper-log "list-backup")
  (shell/with-sh-dir backup-dir
    (str/split-lines(:out (shell/sh "ls" "-t")))))

(defn download-backup
  "Download a backup zip file"
  [zip-name]
  (oper-log (str "download-backup:" zip-name))
  (resp/file-response (generate->zip-file-path zip-name)))

(defn generate->node-file-path
  [dumpfile-root-path node-path]
  (zk/concat-path dumpfile-root-path node-path))

(defn node-to-file
  "Dump node data to a xx.json"
  [cli node-path dumpfile-root-path]
  (let
    [node-file-path (generate->node-file-path dumpfile-root-path node-path)
     node-file-name (str node-file-path ".json")
     bytes (zk/get cli node-path)]
    (io/make-parents node-file-path)
    (if (not bytes)
      (spit node-file-name "")
      (spit node-file-name (String. bytes)))))

(defn export-children-as-zip
  "Export/zip/download"
  [cli node-path zip-name dumpfile-root-path]
  ((zk/recur-child-partial node-to-file dumpfile-root-path) cli node-path)
  (zip-dir zip-name))

(defn backup
  "Do backup node-path with its children ,and return {zip-name/zip-file-path} map"
  ([node-path]
  (backup (zk/get-default-zk-cli) node-path))
  ([cli node-path]
  (let
    [zip-name (generate->zip-name node-path)
     dumpfile-root-path (generate->dumpfile-root-path zip-name)
     zip-file-path (generate->zip-file-path zip-name)]
    (export-children-as-zip cli node-path zip-name dumpfile-root-path)
    {"zip-name" zip-name
     "zip-file-path" zip-file-path
     })))

(defn backup-finished->download
  [zip-info-map]
  (let [zip-name (zip-info-map "zip-name")
        zip-file-path (zip-info-map "zip-file-path")]
    (resp/header
      (resp/file-response zip-file-path)
      "Content-Disposition"
      (str "attachment; filename=" zip-name ".zip"))))

(defn export-zip
  "Export node data recursively as a zip file"
  [cli node-path]
  (oper-log (str "export:" node-path))
  (-> (backup cli node-path)
    (backup-finished->download)
    ))