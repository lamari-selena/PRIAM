package priam.breaches.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import priam.breaches.model.BreachDataSubject;
import priam.breaches.model.BreachDataSubjectId;
import priam.breaches.service.BreachDataSubjectService;

import java.util.List;

@RestController
@RequestMapping("/api/breach-data-subjects")
public class BreachDataSubjectController {

    @Autowired
    private BreachDataSubjectService breachDataSubjectService;

    @GetMapping
    public List<BreachDataSubject> getAllBreachDataSubjects() {
        return breachDataSubjectService.getAllBreachDataSubjects();
    }

    @GetMapping("/breach/{breachId}")
    public List<BreachDataSubject> getBreachDataSubjectsByBreachId(@PathVariable int breachId) {
        return breachDataSubjectService.getBreachDataSubjectsByBreachId(breachId);
    }

    @GetMapping("/{dataSubjectId}/{breachId}")
    public BreachDataSubject getBreachDataSubjectById(@PathVariable int dataSubjectId, @PathVariable int breachId) {
        return breachDataSubjectService.getBreachDataSubjectById(new BreachDataSubjectId(dataSubjectId, breachId));
    }

    @PostMapping
    public BreachDataSubject createBreachDataSubject(@RequestBody BreachDataSubject breachDataSubject) {
        return breachDataSubjectService.saveBreachDataSubject(breachDataSubject);
    }

    @PutMapping("/{dataSubjectId}/{breachId}")
    public BreachDataSubject updateBreachDataSubject(@PathVariable int dataSubjectId, @PathVariable int breachId, @RequestBody BreachDataSubject breachDataSubject) {
        breachDataSubject.setDataSubjectId(dataSubjectId);
        breachDataSubject.setBreachId(breachId);
        return breachDataSubjectService.saveBreachDataSubject(breachDataSubject);
    }

    @DeleteMapping("/{dataSubjectId}/{breachId}")
    public void deleteBreachDataSubject(@PathVariable int dataSubjectId, @PathVariable int breachId) {
        breachDataSubjectService.deleteBreachDataSubject(new BreachDataSubjectId(dataSubjectId, breachId));
    }
}
