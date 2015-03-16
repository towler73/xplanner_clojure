(ns util.wiki
  (:require [clojure.string :as str]
            [clojure.tools.html-utils :as html-utils])
  (:import (org.xwiki.component.embed EmbeddableComponentManager)
           (org.xwiki.rendering.converter Converter)
           (org.xwiki.rendering.renderer.printer DefaultWikiPrinter)
           (org.xwiki.rendering.syntax Syntax)
           (java.io StringReader)))

(def converter
  (let [manager (EmbeddableComponentManager.)]
    (.initialize manager (.getClassLoader (class manager)))
    (.getInstance manager Converter)
    ))



(defn wiki->html [value]
  (let [printer (DefaultWikiPrinter.)
        fixed-value (str/replace value #"   ([0-9]) " "   $1. ")
        string-reader (StringReader. fixed-value)]
    (.convert converter string-reader Syntax/TWIKI_1_0 Syntax/XHTML_1_0 printer)
    (.toString printer)))

