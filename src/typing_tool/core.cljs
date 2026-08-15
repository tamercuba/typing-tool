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
   :errors       0
   :finished?    false})

(defn handle-char [s k correct-char last?]
  (if (= k correct-char)
    (swap! s #(-> %
                  (update :index  inc)
                  (update :result conj k)
                  (assoc :finished? last?)))
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
  (if (:finished? @s)
    nil
    (let [{:keys [base index result]} @s
          k                    (.-key event)
          k                    (if (= k "Enter") "\n" k)
          correct-key          (when (< index (count base)) (nth base index))
          last?                (= (count base) (inc (count result)))]
      (cond
        (contains? no-op-keys k) nil
        (= k "Backspace")       (backspace s)
        correct-key             (handle-char s k correct-key last?)))))

(defn errors [s]
  (let [errors-count (:errors @s)]
    (if (pos? errors-count)
      [:p (str "Voce cometeu " errors-count " erros :(")] nil)))

(defn finished [s input-ref]
  (let [{:keys [finished? errors base]} @s]
    (when finished?
      [:div
       [:p (str "PARABÉNS! Você terminou com apenas " errors " erros.")]
       [:button {:on-click (fn []
                             (reset! s (->state (str/join base)))
                             (some-> @input-ref .focus))}
        "Reiniciar"]])))

(defn placeholder [c]
  (cond
    (= c " ")  "·"
    (= c "\n") (str "↵" "\n")
    :else      c))

(defn placeholder? [c]
  (or (= c " ") (= c "\n")))

(defn remaining-char [c]
  (if (= c " ") "·" c))

(defn typing [text]
  (r/with-let [content   (r/atom (->state text))
               input-ref (r/atom nil)]
    (let [{:keys [base index result wrong-result]} @content
          current (nth base index "")]
      [:div
       [:p
        [:span.char.correct (str/join result)]
        [:span.char.wrong   (str/join wrong-result)]
        [:span {:class (str "char current" (when (placeholder? current) " placeholder"))}
         (placeholder current)]
        (map-indexed
         (fn [i c]
           [:span {:class (str "char" (when (= c " ") " placeholder"))
                   :key i}
            (remaining-char c)])
         (drop (inc index) base))]
       [:input {:ref #(reset! input-ref %)
                :type "text"
                :class "typing-input"
                :auto-focus true
                :on-key-down (partial handle-key-pressed content)}]
       [errors content]
       [finished content input-ref]])))

(r/defc app []
  [:div
   [typing "Eu te amo MUITO! \n aaa"]])

(defonce root (rdc/create-root (js/document.getElementById "app")))

(defn ^:dev/after-load mount-root []
  (rdc/render root [app]))

(defn init []
  (mount-root))
