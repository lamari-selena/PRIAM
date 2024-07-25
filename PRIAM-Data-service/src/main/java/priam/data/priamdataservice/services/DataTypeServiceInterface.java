package priam.data.priamdataservice.services;

import priam.data.priamdataservice.dto.DataTypeRequestDTO;
import priam.data.priamdataservice.dto.DataTypeResponseDTO;

public interface DataTypeServiceInterface {
    /**
     * Save a new DataType
     * @param dataRequestDTO Information of the DataType
     * @return A DataTypeResponseDTO object
     */
    DataTypeResponseDTO save(DataTypeRequestDTO dataTypeRequestDTO);

    /**
     * Retrieve a DataType name from its ID
     * @param dataTypeId ID of the DataType
     * @return The name of the DataType
     */
    String getDataTypeNameByDataTypeId(int dataTypeId);
}
