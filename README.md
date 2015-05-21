# facedna
Simple image generation using 'genetic' algorithm principles.
We cheat a bit here because we know the 'solution' (aka target).
We blindly reach it by painting random traces in our images (mutations) and diff'ing two entities pixel by pixel (cross)

# playground

```clojure
(use 'facedna.core)

; init a population of 100 (target.jpg is located in resources)
(def gen (init "target.jpg" 100))

; generate a new generation using step
(def gen-1 (step gen))

; show the best entity from the population
(show-top-entity gen-1)

; more generations ...
(def gen-50 (apply-n step gen-1 50))
(show-top-entity gen-50)

; save the result
(save gen-50 "gen-50.png")
```

# Example of results
