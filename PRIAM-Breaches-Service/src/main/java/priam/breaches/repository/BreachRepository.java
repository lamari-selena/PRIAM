package priam.breaches.repository;

import priam.breaches.model.Breach;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreachRepository extends JpaRepository<Breach, Integer> {
}