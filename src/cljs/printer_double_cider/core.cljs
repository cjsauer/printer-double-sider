(ns printer-double-cider.core
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as s]))

(def app-title
  "Printer Double Cider")
(def app-tagline
  "Easy double-sided printing at home!")

(enable-console-print!)

(defn page-ranges
  [n slides-per-page]
  (let [ranges (map (juxt first last)
                    (partition-all slides-per-page (range 1 (inc n))))]
    {:side-one (take-nth 2 ranges)
     :side-two (take-nth 2 (drop 1 ranges))}))

(defn page-range->str
  [[lo hi]]
  (if (= lo hi)
    (str lo)
    (s/join "-" [lo hi])))

(defn page-range-coll->str
  [prs]
  (s/join "," (map page-range->str prs)))

(defonce app-state (atom {:num-pages 30
                          :slides-per-page 1}))

(defn num-input
  [{:keys [id label help-text value min max change-fn]}]
  [:div.form-group
   [:label {:for id} label]
   [:input.form-control.form-control-lg
    {:id id
     :type "number"
     :min min
     :max max
     :value (if (js/isNaN value) "" value)
     :on-change #(try (let [v (js/parseInt (.. % -target -value))]
                        (when (or (= "" (.. % -target -value))
                                  (and (>= v min) (<= v max)))
                          (change-fn v)))
                      (catch js/Error e
                        (js/console.warn "Something weird is happening...")))}]
   (when help-text
     [:small.form-text.text-muted help-text])])

;; Relies on clipboard.js https://clipboardjs.com/
(defn copy-to-clipboard-btn
  [{:keys [target-id]}]
  (let [id (str target-id "-copy")]
    (reagent/create-class
     {:component-did-mount #(js/Clipboard. (str "#" id))
      :reagent-render
      (fn [{:keys [target-id]}]
        [:button.btn.btn-link.clipboard-btn
         {:id id
          :data-clipboard-target (str "#" target-id)
          :on-click #(.preventDefault %)}
         "Copy to clipboard"])})))

(defn range-output
  [{:keys [id side-key label num-pages slides-per-page]}]
  (let [range-str (if (and (> num-pages 0) (> slides-per-page 0))
                    (-> (page-ranges num-pages slides-per-page)
                        side-key
                        page-range-coll->str)
                    "")]
    [:div.form-group.range-output
     [:label {:for (str side-key)} label]
     [copy-to-clipboard-btn {:target-id id}]
     [:textarea.form-control
      {:id id
       :value (str range-str)
       :readOnly true}]]))

(defn credits
  []
  [:div.credits
   [:a {:href "https://github.com/cjsauer/printer-double-cider"} "Created with ❤ for B"]])

(defn instructions
  []
  [:div.instructions
   [:h3 "Instructions"]
   [:ol
    [:li "Enter the number of pages you need to print"]
    [:li "Enter the number of 'slides' per page (optional)"]
    [:li "Copy the text from 'Side one' into your print tool's (e.g. Word, Powerpoint) 'page ranges' input"]
    [:li "Print! Be sure to take note of the orientation (e.g. upside-down) and
     side that your printer prints to (e.g. top/bottom)."]
    [:li "Insert the printed pages back into the printer to prepare for side two"]
    [:li "Copy the text from 'Side two' into your print tool's 'page ranges' input"]
    [:li "Print!!"]]])

(defn jumbotron
  []
  [:div.jumbotron
   [:h1.display-4 app-title]
   [:p.lead app-tagline]])

(defn print-panel [{:keys [num-pages slides-per-page]}]
  [:div.print-panel
   [:form
    [num-input {:id "num-pages-input"
                :label "Number of pages (or slides)"
                :min 1
                :max 10000
                :value num-pages
                :change-fn #(swap! app-state assoc :num-pages %)}]
    [num-input {:id "slides-per-page-input"
                :label "Slides per side of paper"
                :help-text "If not using Powerpoint or similar, leave this at 1"
                :min 1
                :max 10000
                :value slides-per-page
                :change-fn #(swap! app-state assoc :slides-per-page %)}]
    [range-output {:id "side-one-output"
                   :side-key :side-one
                   :label "Side one"
                   :num-pages num-pages
                   :slides-per-page slides-per-page}]
    [range-output {:id "side-two-output"
                   :side-key :side-two
                   :label "Side two"
                   :num-pages num-pages
                   :slides-per-page slides-per-page}]]])

(defn app
  []
  [:div.container
   [jumbotron]
   [print-panel @app-state]
   [instructions]
   [credits]])

(defn render []
  (reagent/render [app] (js/document.getElementById "app")))
