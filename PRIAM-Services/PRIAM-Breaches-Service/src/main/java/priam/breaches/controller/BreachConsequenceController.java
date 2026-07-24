package priam.breaches.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import priam.breaches.model.BreachConsequence;
import priam.breaches.model.BreachConsequenceId;
import priam.breaches.service.BreachConsequenceService;

import java.util.List;

@RestController
@RequestMapping("/api/breach-consequences")
public class BreachConsequenceController {

    @Autowired
    private BreachConsequenceService breachConsequenceService;

    @GetMapping
    public List<BreachConsequence> getAllBreachConsequences() {
        return breachConsequenceService.getAllBreachConsequences();
    }

    @GetMapping("/breach/{breachId}")
    public List<BreachConsequence> getBreachConsequencesByBreachId(@PathVariable int breachId) {
        return breachConsequenceService.getBreachConsequencesByBreachId(breachId);
    }

    @GetMapping("/{consequenceId}/{breachId}")
    public BreachConsequence getBreachConsequenceById(@PathVariable int consequenceId, @PathVariable int breachId) {
        return breachConsequenceService.getBreachConsequenceById(new BreachConsequenceId(consequenceId, breachId));
    }

    @PostMapping
    public BreachConsequence createBreachConsequence(@RequestBody BreachConsequence breachConsequence) {
        return breachConsequenceService.saveBreachConsequence(breachConsequence);
    }

    @PutMapping("/{consequenceId}/{breachId}")
    public BreachConsequence updateBreachConsequence(@PathVariable int consequenceId, @PathVariable int breachId, @RequestBody BreachConsequence breachConsequence) {
        breachConsequence.setConsequenceId(consequenceId);
        breachConsequence.setBreachId(breachId);
        return breachConsequenceService.saveBreachConsequence(breachConsequence);
    }

    @DeleteMapping("/{consequenceId}/{breachId}")
    public void deleteBreachConsequence(@PathVariable int consequenceId, @PathVariable int breachId) {
        breachConsequenceService.deleteBreachConsequence(new BreachConsequenceId(consequenceId, breachId));
    }
}
