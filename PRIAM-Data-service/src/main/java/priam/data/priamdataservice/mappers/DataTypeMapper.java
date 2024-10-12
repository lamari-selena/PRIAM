package priam.data.priamdataservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import priam.data.priamdataservice.dto.DataRequestDTO;
import priam.data.priamdataservice.dto.DataResponseDTO;
import priam.data.priamdataservice.dto.DataTypeRequestDTO;
import priam.data.priamdataservice.dto.DataTypeResponseDTO;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.DataType;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DataTypeMapper {

    DataType DataTypeRequestDTOToDataType(DataTypeRequestDTO dataTypeRequestDTO);

    DataTypeResponseDTO DataTypeToDataTypeResponseDTO(DataType dataType);

}
