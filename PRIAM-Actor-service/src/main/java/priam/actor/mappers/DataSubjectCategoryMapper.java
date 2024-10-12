package priam.actor.mappers;

import org.mapstruct.Mapper;
import priam.actor.dto.DataSubjectCategoryRequestDTO;
import priam.actor.dto.DataSubjectCategoryResponseDTO;
import priam.actor.entities.DataSubjectCategory;

@Mapper(componentModel = "spring")
public interface DataSubjectCategoryMapper {
    DataSubjectCategoryResponseDTO DataSubjectCategoryToDataSubjectCategoryResponseDTO(DataSubjectCategory dataSubjectCategory);
    DataSubjectCategory DataSubjectCategoryResponseDTOToDataSubjectCategory(DataSubjectCategoryRequestDTO dataSubjectCategoryRequestDTO);
}
