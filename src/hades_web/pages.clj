(ns hades-web.pages
  (:require [hades-web.zk :as zk]
            [hades-web.conf :as conf]
            [hades-web.export :as export]
            [hades-web.statistics :as statistics]
            [noir.cookies :as cookies]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.request :as req]
            [clojure.string :as str])
  (:use [noir.core]
        [hades-web.util]
        [hiccup page form element core]
        [hades-web.log]
        [hades-web.i18n]))

;; util functions

(defn node-link
  "Return http link to node page"
  [node text]
  [:a {:href (str "/node?path=" node)} text])

(defn nodes-parents-and-link
  "Return name parents and there links"
  [path]
  (let [node-seq (rest (str/split path #"/"))
        link-seq (reduce #(conj %1 (str (last %1) %2 "/"))
                         ["/"] node-seq)
        node-seq (cons (session/get :addr) node-seq)]
    [node-seq link-seq]))

(defn referer
  "Get the referer from http header"
  []
  (let [header (:headers (req/ring-request) {"referer" "/"})
        referer (header "referer")]
    referer))

(defn add-ns
  [addr]
  (let [addr (str/trim addr)]
    (if (not (.endsWith addr "/hades"))
      (str addr "/hades")
      addr)))

(defn init-zk-client [addr]
  (let [addr (add-ns addr)
        cookie-str (cookies/get :history)
        cookie-str (if (nil? cookie-str) "[]" cookie-str)
        cookie (read-string cookie-str)
        cookie (filter #(not= addr %) cookie)]
    (session/put! :cli (zk/mk-zk-cli addr))
    (cookies/put! :history  (str (vec (take 3 (cons addr cookie)))))
    (session/put! :addr addr)))

(defonce conf (conf/load-conf))
(defonce all-users (:users conf))
(defonce default-node (:default-node conf))

;; layout

(defpartial header []
  [:div.span12.page-header
   [:div.row
    [:div.span9
     [:h1 (link-to "/" "Hades Web")
      [:small (space 4) "Web managerment for hades"]]]
    [:div.span3
     (if-let [user (session/get :user)]
       [:div
        [:span.badge.badge-info user]
        (link-to "/logout" [:span.badge.badge-error "Logout"])
        (link-to "/log" [:span.badge.badge-success "Log"])]
       [:div
        [:span.badge "Guest"]
        (link-to "/login" [:span.badge.badge-info "Login"])
        (link-to "/log" [:span.badge.badge-success "Log"])])]]
   ])

(defpartial footer []
  [:div])

(defpartial admin-tool [path]
  [:div.navbar.navbar-fixed-bottom
   [:div.navbar-inner
    [:div.container
     [:a.brand {:href "#"} "Admin Tools"]
     [:ul.nav
      (interleave
       [[:li [:a {:data-toggle "modal" :href "#checkStatusModal"} "Check Status"]]
        [:li [:a {:data-toggle "modal" :href "#exportModal"} "Export"]]
        [:li [:a {:data-toggle "modal" :href "#copyModal"} "Copy"]]
        [:li [:a {:data-toggle "modal" :href "#createModal"} "Create"]]
        [:li [:a {:data-toggle "modal" :href "#editModal"} "Edit"]]
        [:li [:a {:data-toggle "modal" :href "#deleteModal"} "Delete"]]
        [:li [:a {:data-toggle "modal" :href "#rmrModal"} "Delete -R"]]
        [:li [:a {:href "/list-backup"} "Backup"]]]
       (repeat [:li.divider-vertical])
       )]]
    ]])

(defpartial layout [& content]
  (html5
   [:head
    [:title "Hades Web | Web managerment for hades"]
    (include-js "/js/jquery.js")
    (include-js "/js/bootstrap.js")
    (include-css "/css/bootstrap.css")]
   [:body
    [:div.container {:style "padding:0px 0px 40px 0px;"}
     (header)
     content
     (footer)]]))

;; page elements

(defpartial nav-bar [path]
  (let [[node-seq link-seq] (nodes-parents-and-link path)]
    [:ul.breadcrumb.span12
     (interleave (repeat [:i.icon-chevron-right])
                 (map (fn [l n] [:li (node-link l n)]) link-seq  node-seq))]))

(defpartial node-children [parent children]
  (let [parent (if (.endsWith parent "/")
                 parent
                 (str parent "/"))]
    [:div.span4
     [:ul.nav.nav-tabs.nav-stacked
      [:div.row
       [:div.span3 [:h3 "Children"]]
       [:div.span1
        [:span.span1 (space 1)]
        [:span.label.label-info.pull-right (count children)]]]

      (if (empty? children)
        [:div.alert "No children"]
        (map (fn [s] [:li (node-link (str parent s) s)]) children))]]))

(defpartial node-stat [stat]
  [:div.span4
   [:table.table-striped.table-bordered.table
    [:tr [:h3 "Node Stat"]]
    (map (fn [kv]
           [:tr
            [:td (first kv)]
            [:td (last kv)]])
         stat)]])

(defpartial node-data [path data]
  [:div.span4
   [:div.row
    [:div.span3 [:h3 "Node Data" [:a {:href (str "/node-data-show?node-path=" path)} [:span {:class "icon-fullscreen"}]]]]
    [:div.span1
     [:span.span1 (space 1)]
     [:span.label.label-info (count data) " byte(s)"]]]

   (if (nil? data)
     [:div.alert.alert-error "God, zookeeper returns NULL!"]
     [:div.well
      [:p {:style "white-space: nowrap;"}
       (str/replace (bytes->str data) #"\n" "<br>")]])])

(defpartial check-status-modal []
  [:div#checkStatusModal.modal.hide.fade
   [:div.modal-header [:h4 "Check All Clients' Data Status"]]
   (form-to [:post "/check-status"]
     [:div.modal-body
      [:div.alert.alert-error [:h4 "Warning!!"] (i18n-> "check-status-alert")]
      [:p [:span.span3 "Group name（default:main）:"]
       [:input {:type "text" :name "group" :value nil} ]]
      [:p [:span.span3"Project name（default:ad）:"]
       [:input {:type "text" :name "project" :value nil}]]]
     [:div.modal-footer
      [:button.btn.btn-danger "Run checking.."]
      (space 1)
      [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]])
   ])

(defpartial export-modal [path]
  [:div#exportModal.modal.hide.fade
   [:div.modal-header [:h4 "Export path: " path " as a zip file?"]]
   (form-to [:post "/export"]
     [:input {:type "hidden" :name "path" :value path}]
     [:div.modal-body
      [:div.alert.alert-warn [:h4 "Warning!!"] (i18n-> "export-alert")]]
     [:div.modal-footer
      [:button.btn.btn-danger "I will perform Export"]
      (space 1)
      [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]])
   ])

(defpartial copy-modal [path]
  [:div#copyModal.modal.hide.fade
   [:div.modal-header [:h4 "Copy Node With Children"]]
   (form-to [:post "/copy"]
     [:div.modal-body
      [:div.alert.alert-info "Copy from node: " [:strong path]]
      [:span.span2 "To <strong><font color='red'>new path</font></strong>: "]
      [:input.span4 {:type "text" :name "to-path"}]
      [:input.span8 {:type "hidden" :name "path" :value path}]]
     [:div.modal-footer
      [:button.btn.btn-danger  "Run copy.."]
      (space 1)
      [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]
      ])])

(defpartial create-modal [path]
  [:div#createModal.modal.hide.fade
   [:div.modal-header [:h4 "Create A Child"]]
   (form-to [:post "/create"]
            [:div.modal-body
             [:div.alert.alert-info  "Create a child under: " [:strong path]]
             [:input {:type "text" :name "name" :placeholder "Name of new node"}]
             [:textarea.input.span7 {:name "data" :rows 50 :clos 20 :placeholder "Data of new node"}]
             [:input.span8 {:type "hidden" :name "parent" :value path}]]
            [:div.modal-footer
             [:button.btn.btn-danger  "Create"]
             (space 1)
             [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]])
   ])

(defpartial edit-modal [path data]
  [:div#editModal.modal.hide.fade
   [:div.modal-header [:h4 "Edit Node Data"]]
   (form-to [:post "/edit"]
            [:div.modal-body
             [:div.alert.alert-info "Editing node: " [:strong path]]
             [:textarea.input.span7 {:type "text" :name "data" :rows 50 :cols 20} (bytes->str data)]
             [:input.span8 {:type "hidden" :name "path" :value path}]]
            [:div.modal-footer
             [:button.btn.btn-danger  "Save"]
             (space 1)
             [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]
             ])])

(defpartial delete-modal [path children]
  [:div#deleteModal.modal.hide.fade
   [:div.modal-header [:h4 "Delete This Node"]]
   (form-to [:post "/delete"]
            [:input {:type "hidden" :name "path" :value path}]
            (let [child-num (count children)]
              (if (zero? child-num)
                [:div
                 [:div.modal-body
                  [:div.alert.alert-warn  "Confirm to delete node: " [:strong path]]]
                 [:div.modal-footer
                  [:button.btn.btn-danger  "Delete"]
                  [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]]]
                [:div
                 [:div.modal-body
                  [:div.alert.alert-error "You can't delete a node with children."]]
                 [:div.modal-footer
                  [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]]]
                )))])

(defpartial rmr-modal [path]
  [:div#rmrModal.modal.hide.fade
   [:div.modal-header [:h4 "Delete This Node And Its Children"]]
   (form-to [:post "/rmr"]
            [:input {:type "hidden" :name "path" :value path}]
            [:div.modal-body
             [:div.alert.alert-error [:h4 "Danger!!"] (i18n-> "rmr-alert") " node:" [:strong path]]]
            [:div.modal-footer
             [:button.btn.btn-danger "I will perform RMR"]
             (space 1)
             [:button.btn.btn-success {:data-dismiss "modal"} "Cancel"]])
   ])

;; pages

(defpage "/" []
  (when (and  (nil? (cookies/get :history)) (not-empty default-node))
    (init-zk-client default-node))
  (let [cookie (cookies/get :history)
        cookie (if (nil? cookie) "[]" cookie)]
    (layout
     (map #(link-to (str "init?addr=" %) [:div.well.span8 [:h4 %]])
          (read-string cookie))
     [:form.well.span8 {:action "/init" :method "get"}
      [:div.span8
       [:div.row
        [:div.span6
         [:input.span6 {:type "text" :name "addr" :placeholder "Connect String: host[:port]"}]]
        [:div.span2
         [:button.btn.btn-primary {:type "submit"} "Go"]]]]]
     )))

(defpage "/log" []
  (layout
    [:form.well.span8 {:action "/log" :method "get"}
     [:div {:class "pannel pannel-default"}
      [:div {:class "pannel-body"}
       (apply str (get-last-log))
       ]]]
    ))

(defpage "/list-backup" []
  (layout
    [:form.well.span8 {:action "/backup-list" :method "get"}
     [:div {:class "pannel pannel-default"}
      [:div {:class "pannel-body"}
       (i18n-> "backup-tip")
       (for [item (export/list-backup)]
         [:p [:a {:href (str "/download-backup/" item)} item]])
       ]]]
    ))

(defpage "/node" {:keys [path]}
  (let [path (normalize-path path)
        cli (session/get :cli)]
    (if (nil? cli)
      (resp/redirect "/")
      (layout
       (let [children (zk/ls cli path)
             stat (zk/stat cli path)
             data (zk/get cli path)]
         [:div
          (nav-bar path)
          [:div.row
           (node-children path children)
           (node-stat stat)
           (node-data path data)]
          (when-admin
           [:div#adminZone
            (admin-tool path)
            (check-status-modal)
            (export-modal path)
            (copy-modal path)
            (edit-modal path data)
            (create-modal path)
            (delete-modal path children)
            (rmr-modal path)
            ])])))))

(defpage [:get "/init"] {:keys [addr]}
  (init-zk-client addr)
  (resp/redirect "/node"))

(defpage [:get "/login"] {:keys [msg target]}
  (layout
   [:div.span3.offset3
    [:div.row
     (when-not (nil? msg) [:div.alert.alert-error [:h4 msg]])
     (form-to [:post "/login"]
              (label "user" "User Name")
              [:input.span3 {:type "text" :name "user"}]
              (label "pass" "Pass Word")
              [:input.span3 {:type "password" :name "pass"}]
              [:input.span3 {:type "hidden" :name "target" :value (if (nil? target) (referer) target)}]
              [:div.form-actions
               [:button.btn.btn-primary {:type "submit"} "Login"]])]]))

(defpage [:post "/login"] {:keys [user pass target]}
  (cond
   (= (all-users user) pass) (do
                               (session/put! :user user)
                               (resp/redirect target))
   :else (render [:get "/login"]
                 {:msg "Incorrect password." :target target})))

(defpage "/logout" []
  (do
    (session/put! :user nil)
    (resp/redirect (referer))))

(defpage [:post "/edit"] {:keys [path data]}
  (when-admin
   (zk/set (session/get :cli) path (.getBytes data)))
  (resp/redirect (str "/node?path=" path)))

(defpage [:post "/create"] {:keys [parent name data]}
  (let [child-path (child-path parent name)
        data (.getBytes data)
        cli (session/get :cli)]
    (when-admin
     (zk/create cli child-path data)
     (resp/redirect (str "/node?path=" child-path)))))

(defpage [:post "/delete"] {:keys [path]}
  (when-admin
   (zk/rm (session/get :cli) path)
   (resp/redirect (str "/node?path=" (parent path)))))

(defpage [:post "/rmr"]  {:keys [path]}
  (when-admin
   (zk/rmr (session/get :cli) path)
   (resp/redirect (str "/node?path=" (parent path)))))

(defpage [:post "/export"]  {:keys [path]}
  (when-admin
    (export/export-zip (session/get :cli) path)))

(defpage [:get "/download-backup/:zip-name"] {zip-name :zip-name}
  (export/download-backup zip-name))

(defpage [:get "/node-data-show"] {:keys [node-path]}
  (layout
    [:form.span8 {:action (str "/node-data-show" node-path) :method "get"}
     [:p [:h4 (str "Path Data: " node-path)]]
     [:div {:class "well pannel pannel-default"}
      [:div {:class "pannel-body"}
       [:p {:style "white-space: pre-wrap;"}
       (bytes->str (zk/get (session/get :cli) node-path))
       ]]]]
  ))

(defpage [:post "/copy"] {:keys [path to-path]}
  (when-admin
    (zk/copy-node (session/get :cli) path to-path)
  (resp/redirect (str "/node?path=" path))))


(defpage [:post "/check-status"] {:keys [group project]}
  (when-admin
    (layout
      [:form.well.span8 {:action "/check-status" :method "post"}
       [:div {:class "pannel pannel-default"}
        [:div {:class "pannel-body"}
         (let [group (if (str/blank? group) "main" group)
               project (if (str/blank? project) "ad" project)]
          (try
            (statistics/check-status (session/get :cli) group project)
           (catch Exception e (str "No node found for group:" group " project:" project))
             )
           )
         ]]]
      )))

;; data remote api
(defpage [:get "/data"] {:keys [node-path inner-level]}
  (try
    (zk/get-data-recursively (remove-end-slash node-path) (remove-head-slash inner-level))
    (catch Exception e (println (str "Get /data error:" e)) "")
    ))

(defpage [:get "/data-ls"] {:keys [node-path]}
  (try
    (zk/ls-data (remove-end-slash node-path))
    (catch Exception e (println (str "Get /data-ls error:" e)) "")
    ))