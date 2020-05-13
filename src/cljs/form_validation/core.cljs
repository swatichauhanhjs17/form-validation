(ns form-validation.core
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]))

;; -------------------------
;; Routes

(def my-name (reagent/atom nil))
(def my-number (reagent/atom nil))

(defn form-input []
  (let [ string-value (reagent/atom {:name "name" :number "12345"})]
  [:div
   [:p "Name: "    [:input {:type "text"
                           :value (get @string-value :name)
                           :on-change #(swap! string-value assoc :name(-> % .-target .-value))}]]

    [:p "Number"    [:input {:type "text"
                        :value (get @string-value :number)
                        :on-change #(swap! string-value assoc :number (-> % .-target .-value))}]]
                     [:input {:type "submit" ,
                            :value "Submit"
                            :on-click  #(do (reset!  my-name   (get @string-value :name))
                                            (reset!  my-number  (get @string-value :number)) )}]] ))

(defn validation [my-name my-number]
    (fn []
      (if (< 15 (count my-name) )  "valid"  "not-valid")
      (if (< 11 (count my-number)) "valid" "not-valid")))


(def router
  (reitit/router
    ["/" :index]))


(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to form-validation"]
    [form-input]
]
    ))








;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div

       [page]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
