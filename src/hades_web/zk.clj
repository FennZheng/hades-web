(ns hades-web.zk
  (:import [com.netflix.curator.retry RetryNTimes]
           [com.netflix.curator.framework CuratorFramework CuratorFrameworkFactory])
  (:refer-clojure :exclude [set get])
  (:use hades-web.util)
  (:use hades-web.log)
  (:use clojure.java.io)
  (:require [clojure.string :as str]
            [hades-web.export :as export]))

(defn- mk-zk-cli-inner
  "Create a zk client using addr as connecting string"
  [ addr ]
  (let [cli (-> (CuratorFrameworkFactory/builder)
                (.connectString addr)
                (.retryPolicy (RetryNTimes. (int 3) (int 1000)))
                (.build))
        _ (.start cli)]
    cli))

;; memorize this function to save net connection
(def mk-zk-cli (memoize mk-zk-cli-inner))

(defn create
  "Create a node in zk with a client"
  ([cli path data]
    (oper-log (str "create:" path " data:" (java.lang.String. data)))
    (-> cli
      (.create)
      (.creatingParentsIfNeeded)
      (.forPath path data)))
  ([cli path]
    (oper-log (str "create:" path))
    (-> cli
         (.create)
         (.creatingParentsIfNeeded)
         (.forPath path))))

(defn rm
  "Delete a node in zk with a client"
  [cli path]
  (oper-log (str "rm:" path))
  (-> cli (.delete) (.forPath path)))

(defn ls
  "List children of a node"
  [cli path]
  (oper-log (str "ls:" path))
  (-> cli (.getChildren) (.forPath path)))

(defn stat
  "Get stat of a node, return nil if no such node"
  [cli path]
  (-> cli (.checkExists) (.forPath path) bean (dissoc :class)))

(defn set
  "Set data to a node"
  [cli path data]
  (oper-log (str "set:" path " data:" (java.lang.String. data)))
  (-> cli (.setData) (.forPath path data)))

(defn get
  "Get data from a node"
  [cli path]
  (-> cli (.getData) (.forPath path)))

(defn rmr
  "Remove recursively"
  [cli path]
  (doseq [child (ls cli path)]
    (rmr cli (child-path path child)))
  (rm cli path))

(defn concat-path
  [path child]
    (str/replace (str path "/" child) "//" "/")
  )

(defn dump-to-file
  [file-root-path node-path bytes]
  (let
    [file-full-path (concat-path file-root-path node-path)]
    (make-parents file-full-path)
    (if (not bytes)
      (println bytes)
      (spit (str file-full-path ".json") (java.lang.String. bytes)))))

(defn recur-child
  [cli file-root-path node-path]
  ;广度优先
  (doseq [child (ls cli node-path)]
    (let [key (concat-path node-path child)]
    (dump-to-file file-root-path key (get cli key))))
  (if (> (count (ls cli node-path)) 0)
    (doseq [child (ls cli node-path)]
      (recur-child cli file-root-path (concat-path node-path child)))))

(defn export-children-to-dir
  [cli node-path]
  (let
    [zip-name (backup-file-name node-path)
     file-root-path (str "backup/" zip-name)]
    (recur-child cli file-root-path node-path)
    (export/zip-dir zip-name)
    ))

(defn export
  "Export node data recursively"
  [cli path]
  (oper-log (str "export:" path))
  (export-children-to-dir cli path))

