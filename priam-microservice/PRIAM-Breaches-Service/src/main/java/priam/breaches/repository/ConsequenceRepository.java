package priam.breaches.repository;

import priam.breaches.model.Consequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsequenceRepository extends JpaRepository<Consequence, Integer> {
}
// Add similar repository interfaces for BreachMeasure, BreachConsequence, BreachDataSubject, and BreachData