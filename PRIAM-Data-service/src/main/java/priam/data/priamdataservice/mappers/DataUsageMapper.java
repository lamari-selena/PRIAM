package priam.data.priamdataservice.mappers;

import org.mapstruct.Mapper;
import priam.data.priamdataservice.dto.DataUsageRequestDTO;
import priam.data.priamdataservice.dto.DataUsageResponseDTO;
import priam.data.priamdataservice.entities.DataUsage;

@Mapper(componentModel = "spring")
public interface DataUsageMapper {
    DataUsage fromDataUsageDTO(DataUsageRequestDTO dataUsageDTO);

    DataUsageResponseDTO fromDataUsage(DataUsage dataUsage);
}

