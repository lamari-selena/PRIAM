package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.data.priamdataservice.entities.Data;

import java.util.List;
import java.util.Optional;

public interface DataRepository extends JpaRepository<Data, Integer> {
    Optional<Data> findByDataId(int dataId);

    int getIdByDataName(String dataName);

    Data findByDataName(String dataName);

    List<Data> findAllByDataSubjectCategoryId(int dataSubjectCategoryId);

    List<Data> findAllByIsPersonal(boolean isPersonal);

//    DataType findDataTypeByDataId(int dataId);
}
