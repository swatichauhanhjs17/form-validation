(ns form-validation.prod
  (:require [form-validation.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
