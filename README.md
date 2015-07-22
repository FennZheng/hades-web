# hades-web

A smart web management for configuration-center.

(hades-node-client: https://github.com/vernonzheng/hades-node-client)

forked from zk-web as prototype.

Latest Version 1.0.0-beta.

##1. Dependencies

Jdk6+

Leiningen：http://leiningen.org/

##2. Run it

###2.1 Run in local

git clone https://github.com/vernonzheng/hades-web

cd hades-web

lein deps # run this if you're using lein 1.x

lein run


###2.2 Deploy and run
 
lein uberjar

cd target

jar -jar hades-web-x.x.x-standalone.jar


##3. Features

Default Admin Role/password: admin/hades

Language Change: en/zh (modify i18n.clj/language using zh/en)


###3.1 Show node data 


###3.2 Export


###3.3 Copy

You could use this feature to create a new group or project from a exists node.


###3.4 Create/Edit/Delete/Delete -R


###3.5 Check client status

Check all hades client status by comparing their memory data versions with zookeeper data versions.


###3.6 Remote API


####3.6.1 Get Node Data

For an example: 

http://127.0.0.1:9011/data?node-path=/hades/configs/main/ad/log&inner-level=root/loggers


**Parameters:**

+ node-path: node path in zookeeper

+ inner-level: inner level in node data(data must be a JSON string), levels split by '/'.


**Return:**

JSON data as string


####3.6.2 List Node's children

For an example: 

http://127.0.0.1:9011/data-ls?node-path=/hades/configs/main/ad


**Parameters:**

+ node-path: node path in zookeeper


**Return:**

JSON data as Array


###3.7 Backup

Use feature 'export' to download a zip file, and generate a backup file automatically.

Node backup for '/hades/configs/main/ad' runs as a task everyday. See task.clj.


###3.8 Operation Log

Operation will be record both in memory and operation-{date}-log file.
 
Memory log only store latest 100 records and support to be read online. See log.clj