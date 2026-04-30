package priam.consent.priamconsentservice.repositories;

import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import priam.consent.priamconsentservice.entities.Consent;
import priam.consent.priamconsentservice.entities.Contract;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Integer> {
    @Query(value = "SELECT * FROM consent WHERE processing_id = :processingId ORDER BY start_date DESC LIMIT 1", nativeQuery = true)
    Consent findByProcessingId(@Param("processingId") int processingId);

    @Query(value = "SELECT * FROM consent WHERE processing_id = :processingId and contract_id = :contract_id ORDER BY start_date DESC LIMIT 1", nativeQuery = true)
    Consent findByProcessingIdAndContract(@Param("processingId") String processingId, @Param("contract_id") int contract_id);
}



