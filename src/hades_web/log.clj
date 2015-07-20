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

(defn log->memory
  [msg]
  (println msg)
  (if (>= (count @mem-log) max-mem-log)
    (dosync (ref-set mem-log (drop-last drop-size @mem-log))))
  (dosync (alter mem-log conj msg)))

(defn oper-log
  "Operation log when invoke CURD .etc"
  [msg]
  (let [user (get-user)
        prev-msg (str (now-string) ",user:" user)
        log-msg  (str prev-msg ",detail:" msg)]
    (log->memory (str log-msg "</br>"))
    (spit (str "operation-" (date-string) ".log") (str log-msg "\n") :append true)))