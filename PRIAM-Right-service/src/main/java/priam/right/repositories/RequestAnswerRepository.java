package priam.right.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.right.entities.DataRequestAnswer;

import java.util.Optional;

public interface RequestAnswerRepository extends JpaRepository<DataRequestAnswer, Integer> {
    Optional<DataRequestAnswer> findDataRequestAnswerByDataRequest_DataRequestId(int requestId);
}
