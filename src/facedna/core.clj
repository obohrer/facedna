(ns facedna.core
  (:require [clojure.java.io :as io])
  (:import [javax.swing JFrame JPanel]
           [java.awt Graphics Dimension Color]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

(defn ^BufferedImage load-target
  [target-filename]
  (ImageIO/read (io/resource target-filename)))

(defrecord ColorTuple [r g b])

(defn ^ColorTuple px->rgb
  [px]
  (let [a (bit-and (bit-shift-right px 24) 0xff)
        r (bit-and (bit-shift-right px 16) 0xff)
        g (bit-and (bit-shift-right px 8) 0xff)
        b (bit-and px 0xff)]
    (ColorTuple. r g b)))
(set! *warn-on-reflection* true)

(defn build-target-lookup
  [^BufferedImage target width height]
  (vec
   (for [y (range height)]
     (mapv #(px->rgb (.getRGB target (int %) y)) (range width)))))

(defn dist
  [target-lookup ^BufferedImage curr x y]
  (let [^ColorTuple target-color (nth (nth target-lookup y) x)
        ^ColorTuple curr-color (px->rgb (.getRGB curr x y))]
    (+ (Math/abs (int (- (.r target-color) (.r curr-color))))
       (Math/abs (int (- (.g target-color) (.g curr-color))))
       (Math/abs (int (- (.b target-color) (.b curr-color)))))))


(defn random-color
  []
  (rand-int (* 255 255 255)))

(defn init-state
  [^BufferedImage img width height]
  (doseq [x (range width)]
    (doseq [y (range height)]
      (.setRGB img x y (int 0)))))

(defn build-entity
  [width height]
  (let [img (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
    (init-state img width height)
    img))

(defn draw-entity
  [^BufferedImage img width height]
  (let [canvas (proxy [JPanel] []
                 (paint [g] (.drawImage g img (int 0) (int 0) this)))]
    (doto (JFrame.)
      (.add canvas)
      (.setSize (Dimension. width height))
      (.show))))

(defn dup-image
  [^BufferedImage img]
  (let [cm (.getColorModel img)
        raster (.copyData img nil)]
    (BufferedImage. cm raster false nil)))

(defn init
  [target-filename n]
  (let [target (load-target target-filename)
        width (.getWidth target)
        height (.getHeight target)
        entities (->> #(build-entity width height) repeatedly (take n))]

    {:entities entities
     :target target
     :target-lookup (build-target-lookup target width height)
     :width  width
     :height height}))

(def max-line-length 15)

(defn entity-mutations
  [^BufferedImage img width height mutations]
  (let [g (.createGraphics img)]
    (.setStroke g (java.awt.BasicStroke. 7))
  (dotimes [_ mutations]
    (.setColor g (java.awt.Color. (random-color)))
    (let [x  (rand-int width)
          y  (rand-int height)
          dx (- (rand 2.0) 1)
          dy (- (rand 2.0) 1)
          x2 (int (+ x (* dx max-line-length)))
          y2 (int (+ y (* dy max-line-length)))]
    (.drawLine g x y x2 y2))
    ; (.setRGB img (rand-int width) (rand-int height) )
    )))

(defn score
  [target-lookup width height entity]
  (reduce + ; skip one out of 4 px on each axis for speedup
          (for [x (filter #(zero? (mod % 4)) (range width)) y (filter #(zero? (mod % 4)) (range height))]
            (dist target-lookup entity x y))))

(defn cross-avg
  [width height [^BufferedImage ent1 ^BufferedImage ent2]]
  (let [child (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
    (doseq [x (range width)]
      (doseq [y (range height)]
        (let [p1 (px->rgb (.getRGB ent1 x y))
              p2 (px->rgb (.getRGB ent2 x y))]
          (.setRGB child x y (.getRGB (Color. (int (/ (+ (.r p1) (.r p2)) 2))
                                     (int (/ (+ (.g p1) (.g p2)) 2))
                                     (int (/ (+ (.b p1) (.b p2)) 2))))))))
    child))

(defn cross-dist
  [target-lookup width height [^BufferedImage ent1 ^BufferedImage ent2]]
  (let [child (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
    (doseq [x (range width)]
      (doseq [y (range height)]
        (let [p1 (.getRGB ent1 x y)
              p2 (.getRGB ent2 x y)]
          (if (< (dist target-lookup ent1 x y)
                 (dist target-lookup ent2 x y))
            (.setRGB child x y (.getRGB ent1 x y))
            (.setRGB child x y (.getRGB ent2 x y))))))
    child))

(defn cross
  [target-lookup width height entities]
  (if (zero? (rand-int 2))
    (cross-dist target-lookup width height entities)
    (cross-avg width height entities)))

(defn step
  [{:keys [entities target target-lookup width height] :as gen}]
  (doseq [e entities]
    (entity-mutations e width height (+ 10 (rand 20))))
  (let [n (count entities)
        scores (->> entities (pmap #(score target-lookup width height %)) (zipmap entities))
        top-entities (->> entities (sort-by scores <) (take (int (* 0.5 n))))
        _ (println "top score diff" (scores (first top-entities)))
        to-fill (- n (count top-entities))
        to-cross (/ to-fill 2)
        to-dupe (- to-fill to-cross)
        crosses (->> #(rand-nth (take 20 top-entities)) repeatedly (take (* 2 to-cross)) (partition 2) (pmap (partial cross target-lookup width height)))
        dupes (->> #(rand-nth (take 10 top-entities)) repeatedly (take to-dupe) (map dup-image) doall)]
    (assoc gen :entities (concat top-entities crosses dupes))))

(defn show-top-entities
  [{:keys [entities target target-lookup width height] :as gen} n]
  (let [scores (->> (pmap #(vector % (score target-lookup width height %)) entities) (into {}))
        top-entities (->> entities (sort-by scores <) (take n))]
    (doseq [e top-entities]
      (draw-entity e width height))))

(defn show-top-entity
  [{:keys [entities target target-lookup width height] :as gen}]
  (show-top-entities gen 1))

(defn save
  [{:keys [entities target target-lookup width height] :as gen} & [filename]]
  (let [n (count entities)
        scores (->> (pmap #(vector % (score target-lookup width height %)) entities) (into {}))
        top-entity (->> entities (sort-by scores <) first)]
    (ImageIO/write ^BufferedImage top-entity "png" (java.io.File. (or (str filename) "genetics.png")))))

(defn apply-n
  "Apply a function n times by feeding its output as input for the next invocation"
  [f a1 n]
  (if (zero? n)
    a1
    (recur f (f a1) (dec n))))
