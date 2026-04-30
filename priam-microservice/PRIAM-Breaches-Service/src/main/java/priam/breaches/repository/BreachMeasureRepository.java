package priam.breaches.repository;

import priam.breaches.model.BreachMeasure;
import priam.breaches.model.BreachMeasureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreachMeasureRepository extends JpaRepository<BreachMeasure, BreachMeasureId> {

    // Custom query methods can be added here if needed
    List<BreachMeasure> findByBreachId(int breachId);
}
