package priam.breaches.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import priam.breaches.model.BreachMeasure;
import priam.breaches.model.BreachMeasureId;
import priam.breaches.service.BreachMeasureService;

import java.util.List;

@RestController
@RequestMapping("/api/breach-measures")
public class BreachMeasureController {

    @Autowired
    private BreachMeasureService breachMeasureService;

    @GetMapping
    public List<BreachMeasure> getAllBreachMeasures() {
        return breachMeasureService.getAllBreachMeasures();
    }

    @GetMapping("/breach/{breachId}")
    public List<BreachMeasure> getBreachMeasuresByBreachId(@PathVariable int breachId) {
        return breachMeasureService.getBreachMeasuresByBreachId(breachId);
    }

    @GetMapping("/{measureId}/{breachId}")
    public BreachMeasure getBreachMeasureById(@PathVariable int measureId, @PathVariable int breachId) {
        return breachMeasureService.getBreachMeasureById(new BreachMeasureId(measureId, breachId));
    }

    @PostMapping
    public BreachMeasure createBreachMeasure(@RequestBody BreachMeasure breachMeasure) {
        return breachMeasureService.saveBreachMeasure(breachMeasure);
    }

    @PutMapping("/{measureId}/{breachId}")
    public BreachMeasure updateBreachMeasure(@PathVariable int measureId, @PathVariable int breachId, @RequestBody BreachMeasure breachMeasure) {
        breachMeasure.setMeasureId(measureId);
        breachMeasure.setBreachId(breachId);
        return breachMeasureService.saveBreachMeasure(breachMeasure);
    }

    @DeleteMapping("/{measureId}/{breachId}")
    public void deleteBreachMeasure(@PathVariable int measureId, @PathVariable int breachId) {
        breachMeasureService.deleteBreachMeasure(new BreachMeasureId(measureId, breachId));
    }
}
