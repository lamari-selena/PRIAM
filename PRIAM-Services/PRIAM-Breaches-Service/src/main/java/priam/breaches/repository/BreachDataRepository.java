package priam.breaches.repository;

import priam.breaches.model.BreachData;
import priam.breaches.model.BreachDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreachDataRepository extends JpaRepository<BreachData, BreachDataId> {

    // Custom query methods can be added here if needed
    List<BreachData> findByBreachId(int breachId);
}
