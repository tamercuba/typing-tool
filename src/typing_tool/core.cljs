;; typing-tool - a typing practice tool
;; Copyright (C) 2026 Tamer (str "tamer" "cuba" "@" "gmail" "." "com")
;;
;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 2 of the License, or
;; (at your option) any later version.
(ns typing-tool.core
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [reagent.dom.client :as rdc]))

(def ^:private no-op-keys
  #{"Shift" "Alt" "Control" "Meta" "AltGraph"})

;; modelo do estado:
;;   :base         -> vetor de strings (texto original, nunca muda)
;;   :result       -> vetor de chars corretos já digitados (em ordem)
;;   :wrong-result -> vetor de chars errados digitados
;;   :index        -> quantos chars corretos foram digitados

(defn ->state [text]
  {:base         (vec (map str text))
   :result       []
   :wrong-result []
   :index        0
   :errors       0})

(defn handle-char [s k correct-char]
  (if (= k correct-char)
    (swap! s #(-> %
                  (update :index  inc)
                  (update :result conj k)))
    (swap! s #(-> %
                  (update :wrong-result conj k)
                  (update :errors inc)))))

(defn backspace [s]
  (swap! s
         (fn [{:keys [index wrong-result] :as v}]
           (cond
             (seq wrong-result) (update v :wrong-result pop)
             (pos? index)       (-> v
                                    (update :index  dec)
                                    (update :result pop))
             :else              v))))

(defn handle-key-pressed [s event]
  (let [{:keys [base index]} @s
        k           (.-key event)
        correct-key (when (< index (count base)) (nth base index))]
    (cond
      (contains? no-op-keys k) nil
      (= k "Backspace")       (backspace s)
      correct-key             (handle-char s k correct-key))))

(defn errors [s]
  (let [errors-count (:errors @s)]
    (println errors-count)
    (if (pos? errors-count)
      [:p (str "Voce cometeu " errors-count " erros :(")] nil)))

(defn typing [text]
  (r/with-let [content (r/atom (->state text))]
    (let [{:keys [base index result wrong-result]} @content]
      [:div
       [:p
        [:span {:style {:background-color "grey" :color "green"}} (str/join result)]
        [:span {:style {:background-color "grey" :color "red"}}   (str/join wrong-result)]
        [:span {:style {:background-color "grey" :color "black" :text-decoration "underline"}} (nth base index "")]
        [:span {:style {:background-color "grey" :color "black"}} (str/join (drop (inc index) base))]]
       [:input {:type        "text"
                :on-key-down (partial handle-key-pressed content)}]
       [errors content]])))

(r/defc app []
  [:div
   [:h1 "Olá, mundo!"]
   [typing "Eu te amo MUITO!"]])

(defonce root (rdc/create-root (js/document.getElementById "app")))

(defn ^:dev/after-load mount-root []
  (rdc/render root [app]))

(defn init []
  (mount-root))
