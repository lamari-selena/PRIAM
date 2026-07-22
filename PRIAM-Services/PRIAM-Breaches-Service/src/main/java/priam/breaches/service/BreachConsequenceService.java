package priam.breaches.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import priam.breaches.model.BreachConsequence;
import priam.breaches.model.BreachConsequenceId;
import priam.breaches.repository.BreachConsequenceRepository;

import java.util.List;

@Service
public class BreachConsequenceService {

    @Autowired
    private BreachConsequenceRepository breachConsequenceRepository;

    public List<BreachConsequence> getAllBreachConsequences() {
        return breachConsequenceRepository.findAll();
    }

    public List<BreachConsequence> getBreachConsequencesByBreachId(int breachId) {
        return breachConsequenceRepository.findByBreachId(breachId);
    }

    public BreachConsequence getBreachConsequenceById(BreachConsequenceId breachConsequenceId) {
        return breachConsequenceRepository.findById(breachConsequenceId).orElse(null);
    }

    public BreachConsequence saveBreachConsequence(BreachConsequence breachConsequence) {
        return breachConsequenceRepository.save(breachConsequence);
    }

    public void deleteBreachConsequence(BreachConsequenceId breachConsequenceId) {
        breachConsequenceRepository.deleteById(breachConsequenceId);
    }
}
