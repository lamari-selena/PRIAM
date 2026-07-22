package priam.breaches.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import priam.breaches.model.BreachMeasure;
import priam.breaches.model.BreachMeasureId;
import priam.breaches.repository.BreachMeasureRepository;

import java.util.List;

@Service
public class BreachMeasureService {

    @Autowired
    private BreachMeasureRepository breachMeasureRepository;

    public List<BreachMeasure> getAllBreachMeasures() {
        return breachMeasureRepository.findAll();
    }

    public List<BreachMeasure> getBreachMeasuresByBreachId(int breachId) {
        return breachMeasureRepository.findByBreachId(breachId);
    }

    public BreachMeasure getBreachMeasureById(BreachMeasureId breachMeasureId) {
        return breachMeasureRepository.findById(breachMeasureId).orElse(null);
    }

    public BreachMeasure saveBreachMeasure(BreachMeasure breachMeasure) {
        return breachMeasureRepository.save(breachMeasure);
    }

    public void deleteBreachMeasure(BreachMeasureId breachMeasureId) {
        breachMeasureRepository.deleteById(breachMeasureId);
    }
}
