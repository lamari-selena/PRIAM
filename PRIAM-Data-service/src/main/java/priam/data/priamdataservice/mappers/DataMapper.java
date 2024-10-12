package priam.data.priamdataservice.mappers;

import org.mapstruct.Mapper;

import priam.data.priamdataservice.dto.DataRequestDTO;
import priam.data.priamdataservice.dto.DataResponseDTO;
import priam.data.priamdataservice.entities.Data;

@Mapper(componentModel = "spring")
public interface DataMapper {

    //@Mapping(target = "dataType"/*, ignore = true*/, source = "data.dataType")

    //@Mapping(target = "data.dataType.dataList", ignore = true)

    // @Mapping(target = "dataTypeName", source = "data.dataType.dataTypeName")
    // @Mapping(target = "dataType", source = "data.dataType.dataTypeId")
    // @Mapping(target = "dataSubjectCategory", source = "data.dataSubjectCategory")
    DataResponseDTO DataToDataResponseDTO(Data data); 
    // @Mapping(target = "locationId", source = "dataRequestDTO.locationId")
    // @Mapping(target = "dataType.dataTypeId", source = "dataRequestDTO.data_type_id")
    // @Mapping(target = "dataSubjectCategory.dataSubjectCategoryId", source = "dataRequestDTO.data_subject_category_id")
    Data DataRequestDTOToData(DataRequestDTO dataRequestDTO);
}
