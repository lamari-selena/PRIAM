package priam.consent.priamconsentservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Contract;
import priam.consent.priamconsentservice.entities.Processing;

public interface ContractRepository extends JpaRepository<Contract, Integer> {
    Contract findByDataSubjectId (int dataSubjectId);


}
