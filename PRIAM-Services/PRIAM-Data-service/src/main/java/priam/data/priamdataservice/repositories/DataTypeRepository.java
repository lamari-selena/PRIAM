package priam.data.priamdataservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import priam.data.priamdataservice.dto.DataTypeResponseDTO;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.DataType;

public interface DataTypeRepository extends JpaRepository<DataType, Integer> {
}
