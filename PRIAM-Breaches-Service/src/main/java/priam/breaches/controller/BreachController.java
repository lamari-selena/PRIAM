// BreachController.java
package priam.breaches.controller;

import priam.breaches.model.Breach;
import priam.breaches.service.BreachService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/breaches")
public class BreachController {

    @Autowired
    private BreachService breachService;

    @GetMapping
    public List<Breach> getAllBreaches() {
        return breachService.getAllBreaches();
    }

    @GetMapping("/{breachId}")
    public Breach getBreachById(@PathVariable int breachId) {
        return breachService.getBreachById(breachId);
    }

    @PostMapping
    public Breach createBreach(@RequestBody Breach breach) {
        return breachService.saveBreach(breach);
    }

    @PutMapping("/{breachId}")
    public Breach updateBreach(@PathVariable int breachId, @RequestBody Breach breach) {
        breach.setBreachId(breachId);
        return breachService.saveBreach(breach);
    }

    @DeleteMapping("/{breachId}")
    public void deleteBreach(@PathVariable int breachId) {
        breachService.deleteBreach(breachId);
    }
}

// Add similar controller classes for Consequence, BreachMeasure, BreachConsequence, BreachDataSubject, and BreachData

