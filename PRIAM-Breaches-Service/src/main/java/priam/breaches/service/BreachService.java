package priam.breaches.service;

import priam.breaches.model.Breach;
import priam.breaches.repository.BreachRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BreachService {

    @Autowired
    private BreachRepository breachRepository;

    public List<Breach> getAllBreaches() {
        return breachRepository.findAll();
    }

    public Breach getBreachById(int breachId) {
        return breachRepository.findById(breachId).orElse(null);
    }

    public Breach saveBreach(Breach breach) {
        return breachRepository.save(breach);
    }

    public void deleteBreach(int breachId) {
        breachRepository.deleteById(breachId);
    }
}

// Add similar service classes for Consequence, BreachMeasure, BreachConsequence, BreachDataSubject, and BreachData
