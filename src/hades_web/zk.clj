(ns hades-web.zk
  (:import [com.netflix.curator.retry RetryNTimes]
           [com.netflix.curator.framework CuratorFramework CuratorFrameworkFactory])
  (:require [noir.session :as session]
            [noir.request :as req])
  (:refer-clojure :exclude [set get])
  (:use hades-web.util))

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

(defn oper-log
  "Operation log when invoke CURD .etc"
  [msg]
  (let [header req/ring-request
        user (if-let [user-session (session/get :user)]
               user-session
               "Guest")
        prev-msg (str "op-log,user:" user)]
    (println (str prev-msg ",detail:" msg ",header:" header))))

(defn create
  "Create a node in zk with a client"
  ([cli path data]
     (-> cli
      (.create)
      (.creatingParentsIfNeeded)
      (.forPath path data)))
  ([cli path]
     (-> cli
         (.create)
         (.creatingParentsIfNeeded)
         (.forPath path))))

(defn rm
  "Delete a node in zk with a client"
  [cli path]
  (oper-log (str "rm data for path:" path))
  (-> cli (.delete) (.forPath path)))

(defn ls
  "List children of a node"
  [cli path]
  (oper-log (str "list children for path:" path))
  (-> cli (.getChildren) (.forPath path)))

(defn stat
  "Get stat of a node, return nil if no such node"
  [cli path]
  (-> cli (.checkExists) (.forPath path) bean (dissoc :class)))

(defn set
  "Set data to a node"
  [cli path data]
  (oper-log (str "set data for path:" path " data:" data))
  (-> cli (.setData) (.forPath path data)))

(defn get
  "Get data from a node"
  [cli path]
  (-> cli (.getData) (.forPath path)))

(defn rmr
  "Remove recursively"
  [cli path]
  (println "rmr " path)
  (doseq [child (ls cli path)]
    (rmr cli (child-path path child)))
  (rm cli path))


