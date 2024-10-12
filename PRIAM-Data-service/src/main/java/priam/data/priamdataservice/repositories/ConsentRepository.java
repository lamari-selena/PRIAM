package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import priam.data.priamdataservice.entities.consent.Consent;

import java.util.List;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Integer> {

    @Query(value = "SELECT data_id FROM Consent WHERE processing_id = :processingId",
        nativeQuery = true)
    List<Integer> findDataIdByProcessingId(int processingId);
}
