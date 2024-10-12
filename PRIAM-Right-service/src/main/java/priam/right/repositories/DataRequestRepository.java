package priam.right.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import priam.right.entities.DataRequest;
import priam.right.enums.DataRequestType;

import java.util.List;
//@Repository
public interface DataRequestRepository extends JpaRepository<DataRequest, Integer> {

    List<DataRequest> findByDataSubjectId(int dataSubjectId);

    List<DataRequest> findByDataRequestType(DataRequestType dataRequestType);

    @Query(value = "SELECT * FROM data_request dr WHERE dr.data_request_type IN :types",
            nativeQuery = true)
    List<DataRequest> findByTypes(List<Integer> types);
}
