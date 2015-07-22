(ns hades-web.i18n
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:use hades-web.log)
  (:import [java.nio.charset Charset]))

;zh/en
(def language "en")

(def zh-properties {"export-alert" "是否导出该节点和所有的子节点? 导出文件自动备份至
                                   <a href='/list-backup'/>备份目录</a>"
                    "check-status-alert" "是否需要发起校验所有hades client的内存数据一致性？"
                    "rmr-alert" "你确认要删除该节点和所有子节点吗？"
                    "backup-tip" "<strong>文件名格式</strong>:<br>
                                  backup_ _ {node}_{node_list_recursively}_yyyyMMdd_HH_mm_ss_SSS.zip<br>
                                  <hr>
                                  (1)你可以通过export功能生成一个备份文件.<br>
                                  (2)每天都会自动备份 /hades/configs 节点.<br>
                                  <hr>
                                  "
                    })

(def en-properties {"export-alert" "Do you want export this node and its children as a zip file? see
                                  <a href='/list-backup'/>backup directory</a>"
                    "check-status-alert" "Do you want check all clients' memory datas are equal to zk data?"
                    "rmr-alert" "Do you really have to delete this node and its children??"
                    "backup-tip" "<strong>File Name Format</strong>:<br>
                                  backup_ _ {node}_{node_list_recursively}_yyyyMMdd_HH_mm_ss_SSS.zip<br>
                                  <hr>
                                  (1)You can backup a zip file using 'export'.<br>
                                  (2)Node('/hades/configs') backup run automatically everyday.<br>
                                  <hr>
                                  "
                    })

(def properties
  (if (.equals "zh" language)
    zh-properties
    en-properties))

(defn i18n->
  [key]
  (properties key))