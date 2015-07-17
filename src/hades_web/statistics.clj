(ns hades-web.statistics
  (:require [hades-web.zk :as zk]
            [clojure.string :as str])
  (:use [hades-web.util])
  (:import [com.alibaba.fastjson JSON]))

;namespace is ip:port/hades/
(def zk-config-root "/configs/")
(def zk-status-root "/status/")

(defn status-root-path<-
  [group project]
  (str zk-status-root group "/" project))

(defn config-root-path<-
  [group project]
  (str zk-config-root group "/" project))

(defn status-key->path
  [group project key]
  (str (status-root-path<- group project) "/" key))

(defn node->md5
  [cli root-path child]
  (bytes->md5 (zk/get cli (str root-path "/" child))))

(defn get-zk-data
  [cli group project]
  (let [root-path (config-root-path<- group project)]
    (loop [status-map (hash-map)
           children (zk/ls cli root-path)]
      (if (> (count children) 0)
        (let [child (first children)]
          (recur (assoc status-map child (node->md5 cli root-path child)) (rest children)))
        status-map))
    ))

(defn get-status-data
  [cli group project]
  (let [root-path (status-root-path<- group project)]
    (loop [status-map (hash-map)
           ids (zk/ls cli root-path)]
      (if (> (count ids) 0)
        (let [id (first ids)]
          (recur
            (assoc status-map id (bytes->str (zk/get cli (status-key->path group project id))))
            (rest ids)))
        status-map)
      )
    ))

(defn compare->server-data&zk-data
  [server-id src-data-str zk-data-map]
  (let [src-data-map (JSON/parseObject src-data-str)]
    (loop [server-data src-data-map
           msg ""]
      (if (> (count server-data) 0)
        (let [[key value] (first server-data)]
          (if (not (.equals value (zk-data-map key)))
            (recur
              (rest server-data)
              (str msg "node-name: " key "<br>"))
            msg))
        msg))))

(defn generate-server-check-msg
  [server-id error-msg]
  (if (.equals "" error-msg)
    (str "<strong>server-id: " server-id " is the latest version.</strong><br>")
    (str "<strong>server-id: " server-id " expired data:</strong><br><blockquote>" error-msg "</blockquote>")
  ))

(defn check-status
  [cli group project]
  (let [zk-data-map (get-zk-data cli group project)]
    (loop [status-data-map (get-status-data cli group project)
           msg ""]
      (if (> (count status-data-map) 0)
        (let [[server-id value] (first status-data-map)]
          (recur
            (rest status-data-map)
            (str msg (generate-server-check-msg
                       server-id
                       (compare->server-data&zk-data server-id value zk-data-map)))))
       msg))))

