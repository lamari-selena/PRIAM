package priam.breaches.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import priam.breaches.model.BreachData;
import priam.breaches.model.BreachDataId;
import priam.breaches.service.BreachDataService;

import java.util.List;

@RestController
@RequestMapping("/api/breach-data")
public class BreachDataController {

    @Autowired
    private BreachDataService breachDataService;

    @GetMapping
    public List<BreachData> getAllBreachData() {
        return breachDataService.getAllBreachData();
    }

    @GetMapping("/breach/{breachId}")
    public List<BreachData> getBreachDataByBreachId(@PathVariable int breachId) {
        return breachDataService.getBreachDataByBreachId(breachId);
    }

    @GetMapping("/{dataId}/{breachId}")
    public BreachData getBreachDataById(@PathVariable int dataId, @PathVariable int breachId) {
        return breachDataService.getBreachDataById(new BreachDataId(dataId, breachId));
    }

    @PostMapping
    public BreachData createBreachData(@RequestBody BreachData breachData) {
        return breachDataService.saveBreachData(breachData);
    }

    @PutMapping("/{dataId}/{breachId}")
    public BreachData updateBreachData(@PathVariable int dataId, @PathVariable int breachId, @RequestBody BreachData breachData) {
        breachData.setDataId(dataId);
        breachData.setBreachId(breachId);
        return breachDataService.saveBreachData(breachData);
    }

    @DeleteMapping("/{dataId}/{breachId}")
    public void deleteBreachData(@PathVariable int dataId, @PathVariable int breachId) {
        breachDataService.deleteBreachData(new BreachDataId(dataId, breachId));
    }
}
