(ns hades-web.log
  (:require [noir.session :as session]
            [noir.request :as req])
  (:use hades-web.util))

  ;; log in memory
(def ^:dynamic max-mem-log 100)
(def ^:dynamic mem-log '())
(def ^:dynamic old-mem-log '())

(defn get-last-log []
  (println (concat old-mem-log mem-log)))

(defn reset-mem-log []
  (def old-mem-log mem-log)
  (def mem-log '()))

(defn add-mem-log [msg]
  (if (> (count mem-log) max-mem-log)
    (reset-mem-log))
    (def mem-log (conj mem-log msg)))

(defn oper-log
  "Operation log when invoke CURD .etc"
  [msg]
  (let [user (if-let [user-session (session/get :user)]
               user-session
               "Guest")
        prev-msg (str (now-string) ",user:" user)
        log-msg  (str prev-msg ",detail:" msg "\n")]
    (add-mem-log log-msg)
    (spit (str "operation-" (date-string) ".log") log-msg :append true)))