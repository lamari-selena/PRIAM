package priam.actor.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import priam.actor.dto.DataSubjectRequestDTO;
import priam.actor.dto.DataSubjectResponseDTO;
import priam.actor.entities.DataSubject;

@Mapper(componentModel = "spring")
public interface DataSubjectMapper {
    @Mapping(target = "dataSubjectCategoryId", source = "dataSubject.dataSubjectCategory.dataSubjectCategoryId")
    @Mapping(target = "dataSubjectCategoryName", source = "dataSubject.dataSubjectCategory.dataSubjectCategoryName")
    DataSubjectResponseDTO DataSubjectToDataSubjectResponseDTO(DataSubject dataSubject);

    @Mapping(target = "dataSubjectCategory.dataSubjectCategoryId", source = "dataSubjectRequestDTO.dataSubjectCategoryId")
    DataSubject DataSubjectRequestDTOToDataSubject(DataSubjectRequestDTO dataSubjectRequestDTO);
}
