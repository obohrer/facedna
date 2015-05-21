# facedna
Simple image generation using 'genetic' algorithm principles.
We cheat a bit here because we know the 'solution' (aka target).
We 'blindly' reach it by painting random traces in our images (mutations) and mixing two entities together (cross)

# Playground

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
**Target**

![Target](https://raw.githubusercontent.com/obohrer/facedna/master/resources/target.jpg)

**Generation 1**

![Best entity for generation 1](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-1.png)

**Generation 10**

![Best entity for generation 10](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-10.png)

**Generation 20**

![Best entity for generation 20](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-20.png)

**Generation 30**

![Best entity for generation 30](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-30.png)

**Generation 40**

![Best entity for generation 40](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-40.png)

**Generation 50**

![Best entity for generation 50](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-50.png)

**Generation 60**

![Best entity for generation 60](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-60.png)

**Generation 100**

![Best entity for generation 100](https://raw.githubusercontent.com/obohrer/facedna/master/results/gen-100.png)
