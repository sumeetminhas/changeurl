(ns grub.dom
  (:require [dommy.core :as dommy]
            [cljs.core.async :as a :refer [<! >! chan]])
  (:require-macros [grub.macros :refer [log logs go-loop]]
                   [dommy.macros :refer [deftemplate sel1 node]]
                   [cljs.core.async.macros :refer [go]]))

(defn listen
  ([el type] (listen el type nil))
  ([el type f] (listen el type f (chan)))
  ([el type f out]
     (let [push-fn (fn [e] (when f (f e)) (go (>! out e)))
           unlisten #(do (dommy/unlisten! el type push-fn)
                         (a/close! out))]
         (dommy/listen! el type push-fn)
         {:chan out :unlisten unlisten})))

(defn listen-once
  ([el type] (listen el type nil))
  ([el type f] (listen el type f (chan)))
  ([el type f out]
     (let [push-fn (fn [e] (when f (f e)) (go (>! out e)))
           unlisten #(do (dommy/unlisten! el type push-fn)
                         (a/close! out))]
         (dommy/listen-once! el type push-fn)
         {:chan out :unlisten unlisten})))

(def add-grub-text 
  (node [:input.form-control {:id "add-grub-input" :type "text" :placeholder "2 grubs"}]))

(def add-grub-btn 
  (node [:button.btn.btn-primary {:id "add-grub-btn" :type "button"} "Add"]))

(def clear-all-btn
  (node [:button.btn.hidden.pull-right 
         {:id "clear-all-btn" :type "button"}
         "Clear all"]))

(defn make-grub-node [grub]
  (node [:li.list-group-item.grub-item 
         {:id (:_id grub)
          :class (when (:completed grub) "completed")} 
         [:span.grub-static
          (if (:completed grub)
            [:span.glyphicon.glyphicon-check]
            [:span.glyphicon.glyphicon-unchecked])
          [:span.grub-text (:grub grub)]]
         [:input.grub-input {:type "text" :value (:grub grub)}]]))

(defn grubs-selector []
  [(sel1 :#grub-list) :.grub-item])

(defn make-recipe-node [id name steps]
  (node [:div.panel.panel-default.recipe-panel {:id id}
          [:div.panel-heading.recipe-header
           [:input.form-control.recipe-header-input 
            {:id "recipe-name"
             :type "text" 
             :placeholder "Grub pie"
             :value name}]]
          [:div.panel-body.recipe-steps.hidden
           [:textarea.form-control.recipe-steps-input
            {:id "recipe-steps"
             :rows 3 
             :placeholder "2 grubs"}
            steps]
           [:button.btn.btn-primary.recipe-done-btn.hidden.pull-right 
            {:type "button"} "Done"]]]))

(defn add-new-recipe [id name steps]
  (log "add new recipe:" name "steps:" steps)
  (let [node (make-recipe-node id name steps)
        recipe-list (sel1 :#recipe-list)]
    (logs "node:" node)
    (logs "recipe-list:" recipe-list)
    (dommy/append! recipe-list node)
    node))

(def new-recipe (make-recipe-node "new-recipe" "" ""))

(defn recipes-selector []
  [(sel1 :#recipe-list) :.recipe-panel])

(defn recipe-done-btns-selector []
  [(sel1 :body) :.recipe-done-btn])

(deftemplate main-template []
  [:div.container
   [:div.row
    [:div.col-sm-6.leftmost-column
     [:h3 "Grub List"]
     [:div.input-group 
      add-grub-text
      [:span.input-group-btn
       add-grub-btn]]
     [:ul#grub-list.list-group]
     clear-all-btn]
    [:div.col-sm-6
     [:h3.recipes-title "Recipes"]
     new-recipe
     [:ul#recipe-list.list-group.recipe-list]]]])

(defn render-body []
  (dommy/prepend! (sel1 :body) (main-template)))

(defn render-grub-list [grubs]
  (let [grub-list (sel1 :#grub-list)
        sorted-grubs (sort-by (juxt :completed :_id) grubs)]
    (aset grub-list "innerHTML" "")
    (doseq [grub sorted-grubs]
      (let [node (make-grub-node grub)]
        (dommy/append! grub-list node)))))

(defn get-add-grub-text []
  (dommy/value add-grub-text))

(defn clear-add-grub-text []
  (dommy/set-value! add-grub-text ""))


(defprotocol IActivatable
  (-activate! [view])
  (-deactivate! [view]))

(defprotocol IHideable
  (-hide! [view])
  (-show! [view]))

(defprotocol IEditable
  (-set-editing! [view])
  (-unset-editing! [view]))

(defprotocol IExpandable
  (-expand! [view])
  (-unexpand! [view]))

(defprotocol IClearable
  (-clear! [view]))

(extend-type js/HTMLElement
  IActivatable
  (-activate! [view]
    (dommy/add-class! view :grub-active))
  (-deactivate! [view]
    (dommy/remove-class! view :grub-active)))

(extend-type js/HTMLElement
  IHideable
  (-hide! [view]
    (dommy/add-class! view :hidden))
  (-show! [view]
    (dommy/remove-class! view :hidden)))

(extend-type js/HTMLLIElement
  IEditable
  (-set-editing! [view]
    (-deactivate! view)
    (dommy/add-class! view :edit)
    (.focus (sel1 view :input)))
  (-unset-editing! [view]
    (dommy/remove-class! view :edit)))

(extend-type js/HTMLDivElement
  IExpandable
  (-expand! [view]
    (dommy/remove-class! (sel1 view ".recipe-steps") :hidden)
    (dommy/remove-class! (sel1 view ".recipe-done-btn") :hidden))
  (-unexpand! [view]
    (dommy/add-class! (sel1 view ".recipe-steps") :hidden)
    (dommy/add-class! (sel1 view ".recipe-done-btn") :hidden)))

(extend-type js/HTMLDivElement
  IClearable
  (-clear! [view]
    (dommy/set-value! (sel1 view "#recipe-name") "")
    (dommy/set-value! (sel1 view "#recipe-steps") "")))
    
