(ns grub.view
  (:require [grub.async-utils 
             :refer [do-chan! do-chan event-chan map-chan fan-in filter-chan]]
            [grub.state :as state]
            [dommy.core :as dommy]
            [cljs.core.async :refer [<! >! chan]])
  (:require-macros [grub.macros :refer [log logs go-loop]]
                   [dommy.macros :refer [deftemplate sel1 node]]
                   [cljs.core.async.macros :refer [go]]))

(def outgoing-events (chan))

(def add-grub-text 
  (node [:input.form-control {:id "add-grub-input" :type "text" :placeholder "2 grubs"}]))

(def add-grub-btn 
  (node [:button.btn.btn-default {:id "add-grub-btn" :type "button"} "Add"]))

(deftemplate main-template []
  [:div.container
   [:div.row.show-grid
    [:div.col-lg-4]
    [:div.col-lg-4
     [:h3 "Grub List"]
     [:div.input-group 
      add-grub-text
      [:span.input-group-btn
       add-grub-btn]]
     [:ul#grub-list.list-group]]
     [:div.col-lg-4]
    [:div.col-lg-2]]])

(defn make-grub-node [grub]
  (if (:completed grub)
    (node [:li.list-group-item.completed.grub-item {:id (:_id grub)} (:grub grub)])
    (node [:li.list-group-item.grub-item {:id (:_id grub)} (:grub grub)])))

(defn render-body []
  (dommy/prepend! (sel1 :body) (main-template)))

(defn get-add-grub-text []
  (let [text (dommy/value add-grub-text)]
    (dommy/set-value! add-grub-text "")
    text))

(defn get-grubs-from-clicks []
  (let [out (chan)]
    (dommy/listen! add-grub-btn :click #(go (>! out (get-add-grub-text))))
    out))

(defn put-grubs-if-enter-pressed [out event]
  (when (= (.-keyIdentifier event) "Enter")
    (go (>! out (get-add-grub-text)))))

(defn get-grubs-from-enter []
  (let [out (chan)]
    (dommy/listen! add-grub-text 
                   :keyup 
                   (partial put-grubs-if-enter-pressed out))
    out))

(defn get-added-events []
  (let [grubs (fan-in [(get-grubs-from-clicks)
                       (get-grubs-from-enter)])]
    (->> grubs
         (filter-chan #(not (empty? %)))
         (map-chan (fn [g] {:event :create :grub g})))))

(defn get-completed-event [event]
  (let [target (.-target event)
        id (.-id target)
        completed (dommy/has-class? target "completed")
        event-type (if completed :uncomplete :complete)]
    {:_id id :event event-type}))
  
(defn get-deleted-events []
  (let [click-events (chan)]
    (dommy/listen! [(sel1 :#grub-list) ".close"] 
                   :click 
                   #(go (>! click-events %)))
    (let [ids (map-chan #(.-id (.-parentNode (.-parentNode (.-target %)))) click-events)
          grub-events (map-chan (fn [id] {:event :delete :_id id}) ids)]
      grub-events)))


(defn render-grub-list [grubs]
  (let [grub-list (sel1 :#grub-list)
        sorted-grubs (sort-by (juxt :completed :_id) grubs)]
    (aset grub-list "innerHTML" "")
    (doseq [grub sorted-grubs]
      (let [node (make-grub-node grub)]
        (dommy/listen! node :click #(go (>! outgoing-events (get-completed-event %))))
        (dommy/append! grub-list node)))))

(defn push-outgoing-events []
  (fan-in outgoing-events [(get-added-events)
                           (get-deleted-events)]))

(defn watch-for-state-changes []
  (add-watch state/grubs 
             :grub-add-watch
             (fn [key ref old new]
               (render-grub-list new))))

(defn init []
  (render-body)
  (watch-for-state-changes)
  (push-outgoing-events))