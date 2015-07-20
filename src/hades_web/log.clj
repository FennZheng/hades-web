(ns hades-web.log
  (:require [noir.session :as session]
            [noir.request :as req])
  (:use hades-web.util))

 ;; log in memory
(defonce max-mem-log 100)
(defonce drop-size 20)
(def mem-log (ref '()))

(defn get-last-log []
  @mem-log)

(defn generate-log
  [msg]
  (let [user (get-user)
    prev-msg (str (now-string) ",user:" user)
    log-msg  (str prev-msg ",detail:" msg)]
  log-msg))

(defn log->memory
  [msg]
  (if (>= (count @mem-log) max-mem-log)
    (dosync (ref-set mem-log (drop-last drop-size @mem-log))))
  (dosync (alter mem-log conj (str msg "</br>")))
  msg)

(defn log->file
  [msg]
  (spit (str "operation-" (date-string) ".log") (str msg "\n") :append true)
  msg)

(defn log->mutil
  [msg]
  (-> (generate-log msg)
    (log->memory)
    (log->file)))

(defn oper-log
  "Operation log when invoke CURD .etc"
  [msg]
  (log->mutil msg))