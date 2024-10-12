package priam.right.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import priam.right.entities.DataRequestData;

import java.util.List;
import java.util.Optional;

//@Repository
public interface DataRequestDataRepository extends JpaRepository<DataRequestData, Integer> {

    @Query(value = "SELECT data_id FROM data_request_data WHERE data_request_id = :dataRequestId",
            nativeQuery = true)
    List<Integer> findDataIdsByDataRequestId(int dataRequestId);

    List<DataRequestData> findDataRequestDataByDataRequestId(int dataRequestId);

    @Query(value= "SELECT answer_by_data FROM data_request_data WHERE data_id =:dataId AND answer_by_data = true AND data_request_id IN " +
            "(SELECT data_request_id FROM Data_Request WHERE data_subject_id = :dataSubjectId) " +// AND data_request_issued_at > DATE_SUB(DATE(NOW()), INTERVAL 2 DAY))" +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Boolean> isDataAcceptedByDataSubjectIdAndDataId(int dataSubjectId, int dataId);
}
