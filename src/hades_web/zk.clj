(ns hades-web.zk
  (:import [com.netflix.curator.retry RetryNTimes]
           [com.netflix.curator.framework CuratorFramework CuratorFrameworkFactory])
  (:refer-clojure :exclude [set get])
  (:use hades-web.util)
  (:use hades-web.log)
  (:require [clojure.string :as str]
            [ring.util.response :as resp]))

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

(defn exists
  "Check node exists"
  [cli path]
  (-> cli (.checkExists) (.forPath path)))

(defn rmr
  "Remove recursively"
  [cli path]
  (doseq [child (ls cli path)]
    (rmr cli (child-path path child)))
  (rm cli path))

(defn concat-path
  "Return zk children node path"
  [path child-key]
    (str/replace (str path "/" child-key) "//" "/")
  )

(defn recur-child-partial
  [f args]
  (defn r [cli parent-node]
    (doseq [child-key (ls cli parent-node)]
      (let [child-node (concat-path parent-node child-key)]
        (f cli child-node args)
        (r cli child-node)))) r)

(defn node->node
  [cli node path-map]
  (let [to-node (str/replace node (:from path-map) (:to path-map))
        node-data (get cli node)]
    (if node-data
      (create cli to-node node-data)
      (create cli to-node))))

(defn is-sub-path
  [from-path to-path]
  (.start-with from-path to-path)
  )

(defn copy-node
  "copy node with children"
  [cli from-path to-path]
  ; to-path must not exists, to-path must not be a child of from-path
  (if
    (not
      (and
        (exists cli to-path)
        (.startsWith to-path from-path)))
    (do
      (create cli to-path)
      (let [path-map {:from from-path :to to-path}]
        ((recur-child-partial node->node path-map) cli from-path)))))