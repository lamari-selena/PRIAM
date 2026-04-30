package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import priam.data.priamdataservice.entities.Measure;

@Repository
public interface MesureRepository extends JpaRepository<Measure, Integer> {

}
