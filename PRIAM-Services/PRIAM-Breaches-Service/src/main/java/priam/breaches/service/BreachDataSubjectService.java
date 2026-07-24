package priam.breaches.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import priam.breaches.model.BreachDataSubject;
import priam.breaches.model.BreachDataSubjectId;
import priam.breaches.repository.BreachDataSubjectRepository;

import java.util.List;

@Service
public class BreachDataSubjectService {

    @Autowired
    private BreachDataSubjectRepository breachDataSubjectRepository;

    public List<BreachDataSubject> getAllBreachDataSubjects() {
        return breachDataSubjectRepository.findAll();
    }

    public List<BreachDataSubject> getBreachDataSubjectsByBreachId(int breachId) {
        return breachDataSubjectRepository.findByBreachId(breachId);
    }

    public BreachDataSubject getBreachDataSubjectById(BreachDataSubjectId breachDataSubjectId) {
        return breachDataSubjectRepository.findById(breachDataSubjectId).orElse(null);
    }

    public BreachDataSubject saveBreachDataSubject(BreachDataSubject breachDataSubject) {
        return breachDataSubjectRepository.save(breachDataSubject);
    }

    public void deleteBreachDataSubject(BreachDataSubjectId breachDataSubjectId) {
        breachDataSubjectRepository.deleteById(breachDataSubjectId);
    }
}
