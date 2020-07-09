(ns form-validation.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    [reagent.dom :as rdom]
    [reagent.session :as session]
    [reitit.frontend :as reitit]
    [clerk.core :as clerk]
    [accountant.core :as accountant]
    [clojure.edn :as edn]
    ;; [clj-time.core :as t]

    ))

;; -------------------------
;; Routes


(def my-name (reagent/atom nil))
(def my-number (reagent/atom nil))
(def my-email (reagent/atom nil))




(defn num-valid? [form-value]
  (if (re-find #"[^\d+$]" (get form-value :number))
    ["only numbers allowed"]
    (cond-> []
            (> 5 (edn/read-string (get form-value :number))) (conj :number "too short")
            (< 15 (edn/read-string (get form-value :number))) (conj :number "too long")
            (and (<= 5 (edn/read-string (get form-value :number)))
                 (>= 15 (edn/read-string (get form-value :number)))) (conj :number "valid"))
    )
  )




(defn email-valid? [form-value]
  (cond-> []
          (re-matches #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                      (get form-value :email)) (conj :email "Valid")
          (not (re-matches #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                           (get form-value :email))) (conj :email "Invalid")
          )
  )

(defn name-valid? [form-value]
  (if (not (re-find #"[^\d+$]" (get form-value :name)))
    ["only characters allowed"]
    (cond-> []
            (> 5 (count (get form-value :name))) (conj :name " too short")
            (< 15 (count (get form-value :name))) (conj :name " too long")
            (and (<= 5 (count (get form-value :name)))
                 (>= 15 (count (get form-value :name)))) (conj :name " valid"))
   )

  )



(defn form-valid? [form-value]
  {:number (num-valid? form-value)
   :name   (name-valid? form-value)
   :email  (email-valid? form-value)
   }
  )

(defn show-name-errors
  [error-message]
  [:div
    (for [item error-message]
       item
      )])


(defn show-num-errors
  [num-error]
  [:div
   (for [item num-error]
     ^{:key (str item)} item)
   ])

(defn show-email-error
  [email-error]
  [:div
   (for [item email-error]
     item )
   ]
)

(defn form-input []
  (let [form-value (reagent/atom {:name nil :number nil :email nil :date1 nil})
        error-message (reagent/atom {})]
    (fn []
      [:div
       [:p "Name: " [:input {:type      "text"
                             :value     (get @form-value :name)
                             :on-change #(swap! form-value assoc :name (-> % .-target .-value))
                             }]
        [show-name-errors (get @error-message :name)]
        ]


       [:p "Number: " [:input {:type      "text"
                               :value     (get @form-value :number)
                               :on-change #(swap! form-value assoc :number (-> % .-target .-value))}]
        [show-num-errors (get @error-message :number)]
        ]

       [:p "Email: " [:input {:type      "email"
                              :value     (get @form-value :email)
                              :on-change #(swap! form-value assoc :email (-> % .-target .-value))}]
        [show-email-error (get @error-message :email)]
        ]

       [:p "Date: " [:input {:type      "date"
                             :value     (get @form-value :date1)
                             :on-change #(swap! form-value assoc :date1 (-> % .-target .-value))
                             }]]

       [:input {:type     "submit",
                :value    "Submit"
                :on-click #(reset! error-message (form-valid? @form-value))
                }]
       [:p "ERROR MESSAGE:- " (str @error-message)]
       ])))


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
      [:p "changed email :-" @my-email]
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
             current-page (:name (:data match))
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
