package priam.consent.priamconsentservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.consent.priamconsentservice.entities.Consent;

public interface ConsentRepository extends JpaRepository<Consent, Integer> {
}
