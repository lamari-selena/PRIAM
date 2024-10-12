package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import priam.data.priamdataservice.entities.Purpose;

@Repository
public interface PurposeRepository extends JpaRepository<Purpose, Integer> {

}
