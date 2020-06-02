(ns form-validation.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    [reagent.dom :as rdom]
    [reagent.session :as session]
    [reitit.frontend :as reitit]
    [clerk.core :as clerk]
    [accountant.core :as accountant]
    [clojure.edn :as edn]))

;; -------------------------
;; Routes

(def my-name (reagent/atom nil))
(def my-number (reagent/atom nil))
(def my-email (reagent/atom nil))


(defn email-valid? [form-value]
  (re-matches #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                   (get @form-value :email))
  )

(defn name-valid? [form-value]
  (and (< 5 (count (get @form-value :name)))
       (> 15 (count (get @form-value :name))))

  )


(defn num-valid? [form-value]

  (and (< 5  (edn/read-string (get @form-value :number) ))
       (> 15 (edn/read-string (get @form-value :number) )) ) )



(defn form-valid? [form-value]

    (if (and (name-valid? form-value))
      (if (num-valid? form-value)
        (if (email-valid? form-value) true false ) false  ) false )

  )


(defn form-input []
  (let [ form-value (reagent/atom {:name nil :number nil :email nil})
        error-message (reagent/atom nil)]
    (fn []
      [:div
       [:p "Name: "    [:input {:type "text"
                                :value (get @form-value :name)
                                :on-change #(swap! form-value assoc :name(-> % .-target .-value))
                               }]


        ]


       [:p "Number: "    [:input {:type "text"
                                :value (get @form-value :number)
                                :on-change #(swap! form-value assoc :number (-> % .-target .-value))}]
        ]

       [:p "Email: "    [:input {:type "email"
                                :value (get @form-value :email)
                                :on-change #(swap! form-value assoc :email (-> % .-target .-value))}]
        ]

       [:input {:type "submit" ,
                :value "Submit"
                :on-click  #(if  (form-valid? form-value) (do (reset!  my-name (get @form-value :name))
                                                              (reset!  my-number (get @form-value :number))
                                                              (reset!  my-email (get @form-value :email))
                                                              (reset! error-message "Your form is VERIFIED")
                                                              )(reset! error-message "Your form has error")  )
                }]
       [:p "ERROR MESSAGE:- " @error-message]
       ] )))


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
     [:div
      "changed name :-" @my-name
      [:p "changed number :-" (edn/read-string @my-number)]
      [:p "changed email :-"  @my-email]
      ]
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
