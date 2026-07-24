package priam.breaches.repository;

import priam.breaches.model.BreachConsequence;
import priam.breaches.model.BreachConsequenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreachConsequenceRepository extends JpaRepository<BreachConsequence, BreachConsequenceId> {

    // Custom query methods can be added here if needed
    List<BreachConsequence> findByBreachId(int breachId);
}

