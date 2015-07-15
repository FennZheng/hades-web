(ns hades-web.log
  (:require [noir.session :as session]
            [noir.request :as req])
  (:use hades-web.util))

  ;; log in memory
(def ^:dynamic max-mem-log 100)
(def ^:dynamic mem-log '())
(def ^:dynamic old-mem-log '())

(defn get-last-log []
  (concat old-mem-log mem-log))

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
  (let [user (get-user)
        prev-msg (str (now-string) ",user:" user)
        log-msg  (str prev-msg ",detail:" msg)]
    (add-mem-log (str log-msg "</br>"))
    (spit (str "operation-" (date-string) ".log") (str log-msg "\n") :append true)))