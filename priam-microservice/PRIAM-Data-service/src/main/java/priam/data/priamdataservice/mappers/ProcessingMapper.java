package priam.data.priamdataservice.mappers;

import priam.data.priamdataservice.dto.*;
import org.mapstruct.Mapper;
import priam.data.priamdataservice.dto.ProcessingRequestDTO;
import priam.data.priamdataservice.dto.ProcessingResponseDTO;
import priam.data.priamdataservice.entities.Processing;

@Mapper(componentModel = "spring")
public interface ProcessingMapper {
    Processing fromProcessingDTO(ProcessingRequestDTO processingRequestDTO);

    ProcessingResponseDTO fromProcessing(Processing processing);
}

