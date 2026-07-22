package priam.breaches.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import priam.breaches.model.BreachData;
import priam.breaches.model.BreachDataId;
import priam.breaches.repository.BreachDataRepository;

import java.util.List;

@Service
public class BreachDataService {

    @Autowired
    private BreachDataRepository breachDataRepository;

    public List<BreachData> getAllBreachData() {
        return breachDataRepository.findAll();
    }

    public List<BreachData> getBreachDataByBreachId(int breachId) {
        return breachDataRepository.findByBreachId(breachId);
    }

    public BreachData getBreachDataById(BreachDataId breachDataId) {
        return breachDataRepository.findById(breachDataId).orElse(null);
    }

    public BreachData saveBreachData(BreachData breachData) {
        return breachDataRepository.save(breachData);
    }

    public void deleteBreachData(BreachDataId breachDataId) {
        breachDataRepository.deleteById(breachDataId);
    }
}
