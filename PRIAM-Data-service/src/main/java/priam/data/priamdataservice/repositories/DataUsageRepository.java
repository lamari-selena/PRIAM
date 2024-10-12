package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import priam.data.priamdataservice.entities.DataUsage;

import java.util.Collection;

@Repository
public interface DataUsageRepository extends JpaRepository<DataUsage, Integer> {
    Collection<DataUsage> findAllByProcessing_ProcessingId(int processingId);
}
