package priam.data.priamdataservice.services;

import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.DataTypeRequestDTO;
import priam.data.priamdataservice.dto.DataTypeResponseDTO;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.DataType;
import priam.data.priamdataservice.mappers.DataTypeMapper;
import priam.data.priamdataservice.repositories.DataTypeRepository;

import javax.transaction.Transactional;

@Service
@Transactional
public class DataTypeService implements DataTypeServiceInterface {
    final DataTypeMapper dataTypeMapper;
    final DataTypeRepository dataTypeRepository;

    public DataTypeService(DataTypeMapper dataTypeMapper, DataTypeRepository dataTypeRepository) {
        this.dataTypeMapper = dataTypeMapper;
        this.dataTypeRepository = dataTypeRepository;
    }

    @Override
    public DataTypeResponseDTO save(DataTypeRequestDTO dataTypeRequestDTO) {
        DataType dataType = dataTypeMapper.DataTypeRequestDTOToDataType(dataTypeRequestDTO);
        DataType saveData = dataTypeRepository.save(dataType);
        return dataTypeMapper.DataTypeToDataTypeResponseDTO(saveData);
    }

    @Override
    public String getDataTypeNameByDataTypeId(int dataTypeId) {
        DataType dataType = dataTypeRepository.getById(dataTypeId);
        return dataType.getDataTypeName();
    }
}
