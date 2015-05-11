(ns util.wiki
  (:require [clojure.string :as str])
  (:import (java.io StringReader ByteArrayOutputStream)
           (org.apache.maven.doxia DefaultConverter)
           (org.apache.maven.doxia.wrapper InputReaderWrapper OutputStreamWrapper)))

(def converter (DefaultConverter.))


(defn wiki->html [value]
  (let [fixed-value (str/replace value #"   ([0-9]) " "   $1. ")
        string-reader (StringReader. fixed-value)
        out (ByteArrayOutputStream.)
        input (InputReaderWrapper/valueOf string-reader "twiki" (.getInputFormats converter))
        output (OutputStreamWrapper/valueOf out "xhtml" "UTF-8" (.getOutputFormats converter))]
    (.convert converter input output)
    (.toString out)))

