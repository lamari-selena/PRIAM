package priam.right.repositories;

import org.hibernate.mapping.PrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import priam.right.entities.DataRequestPrimaryKey;
import priam.right.entities.DataRequestPrimaryKeyKey;

import java.util.List;

//@Repository
public interface DataRequestPrimaryKeyRepository extends JpaRepository<DataRequestPrimaryKey, DataRequestPrimaryKeyKey> {
    List<DataRequestPrimaryKey> findByDataRequestId(int dataRequestId);
}
