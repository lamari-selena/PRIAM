package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import priam.data.priamdataservice.entities.Processing;

@Repository
public interface ProcessingRepository extends JpaRepository<Processing, Integer> {

}