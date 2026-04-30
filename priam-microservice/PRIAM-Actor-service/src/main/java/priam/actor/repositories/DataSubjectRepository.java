package priam.actor.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.actor.entities.DataSubject;

public interface DataSubjectRepository extends JpaRepository<DataSubject, Integer> {
    DataSubject findDataSubjectByIdRef(String idRef);
}
