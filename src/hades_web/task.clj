(ns hades-web.task
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
            [hades-web.export :as export]
            [hades-web.log :as log]))

(def backup-node "/hades/configs")

(defjob backup-job
  [ctx]
  (println "backup-job started!")
  (log/oper-log (str "backup-job:" backup-node))
  (try
    (export/backup backup-node)
    (catch Exception e (println (str "backup-job exception:" (.getMessage e)))))
  (println "backup-job finished!"))

(defn start-backup
  [& m]
  (let [s (-> (qs/initialize) qs/start)
    job (j/build
      (j/of-type backup-job)
      (j/with-identity (j/key "jobs.backup")))
    trigger (t/build
      (t/with-identity (t/key "triggers.backup"))
      (t/start-now)
      (t/with-schedule (schedule
         (cron-schedule "0 0 0 * * ?"))))]
    (qs/schedule s job trigger)))