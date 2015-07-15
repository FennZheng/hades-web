(ns hades-web.server
  (:gen-class)
  (:require [noir.server :as server]
            [hades-web.conf :as conf]
            [hades-web.task :as task]
            ))

(server/load-views-ns 'hades-web.pages)


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (:server-port (conf/load-conf))]
    (server/start port {:mode mode
                        :ns 'hades-web})
    (task/start-backup)
    (println "server started")))
