package priam.breaches.repository;

import priam.breaches.model.BreachDataSubject;
import priam.breaches.model.BreachDataSubjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreachDataSubjectRepository extends JpaRepository<BreachDataSubject, BreachDataSubjectId> {

    // Custom query methods can be added here if needed
    List<BreachDataSubject> findByBreachId(int breachId);
}

