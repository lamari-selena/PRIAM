package priam.right.mappers;

import org.mapstruct.Mapper;
import priam.right.dto.DataRequestRequestDTO;
import priam.right.dto.DataRequestResponseDTO;
import priam.right.entities.DataRequest;

@Mapper(componentModel = "spring")
public interface DataRequestMapper {
    DataRequest fromDataRequestRequestDTO(DataRequestRequestDTO dataRequestRequestDTO);

    DataRequestResponseDTO fromDataRequest(DataRequest dataRequest);
}
