package priam.data.priamdataservice.services;

import priam.data.priamdataservice.dto.*;
import priam.data.priamdataservice.dto.transfer.DataListTransferDTO;
import priam.data.priamdataservice.entities.Data;

import java.util.List;

public interface DataServiceInterface {

    /**
     * Retrieve information of a data based on the data ID
     *
     * @param dataId The data ID used to retrieve the data
     * @return A DataResponseDTO object
     */
    DataResponseDTO getData(int dataId);

    /**
     * Retrieve all personal data in a list of DataResponseDTO objects
     * @return A list of DataResponseDTO objects
     */
    List<DataResponseDTO> findAllPersonalData();

    /**
     * Retrieve all data in a list of DataResponseDTO objects
     * @return A list of DataResponseDTO objects
     */
    List<DataResponseDTO> findAllData();

    /**
     * Retrieve a dataID based on its dataName
     * @param dataName The name of the data
     * @return A dataID
     */
    int getIdByDataName(String dataName);

    /**
     * Retrieve a dataName based onn the data ID
     * @param dataId The ID of the data
     * @return A dataName
     */
    String getDataNameById(int dataId);

    /**
     * Retrieve a list of DataResponseDTO objects based on the data subject category ID
     *
     * @param dataSubjectCategoryId ID of the data subject category
     * @return A list of DataResponseDTO objects
     */
    List<DataResponseDTO> findAllDataByDataSubjectCategory(int dataSubjectCategoryId);

    /**
     * Retrieve a list of Data objects based of the data subject category ID and data subject ID
     *
     * @param dataSubjectCategoryId ID of the data subject category
     * @param dataSubjectId ID of the data subject
     * @return A list of Data objects
     */
    List<Data> findAllProcessedDataByDataSubjectCategoryAndId(int dataSubjectCategoryId, int dataSubjectId);

    /**
     * Save a new data
     * @param dataRequestDTO Information of the data
     * @return A DataResponseDTO object
     */
    DataResponseDTO save(DataRequestDTO dataRequestDTO);

    /**
     * Update a data
     *
     * @param dataRequestDTO Information of the data
     * @return A DataResponseDTO object
     */
    DataResponseDTO update(DataRequestDTO dataRequestDTO);

    /**
     * Retrieve a list of DataResponseDTO objects based on the data type name
     *
     * @param dataTypeName
     * @return A list of DataResponseDTO objects
     */
    List<DataResponseDTO> getPersonalDataByDataTypeName(String dataTypeName);

    /**
     * Retrieve a list of ProcessedPersonalDataDTO objects based on the reference ID
     *
     * @param idRef The reference ID used to retrieve processed personal data.
     * @return A list of ProcessedPersonalDataDTO objects containing each a dataType with its list of data and primary keys
     */
    List<ProcessedPersonalDataDTO> getProcessedPersonalDataList(String idRef);

    /**
     * Retrieve a list of ProcessedIndirectAndProducedPersonalDataDTO objects based on the reference ID
     *
     * @param idRef The reference ID used to retrieve processed personal data.
     * @return A list of ProcessedIndirectAndProducedPersonalDataDTO objects containing each a dataType with its list of data
     */
    List<ProcessedIndirectAndProducedPersonalDataDTO> getProcessedIndirectAndProducedPersonalDataList(String idRef);

    /**
     * Retrieves a list of DataListTransferDTO objects based on the specified reference ID.
     * For each processed personal data associated with the reference ID, fetches secondary actors
     * and constructs DataListTransferDTO objects containing the secondary actors and their
     * corresponding processed personal data.
     *
     * @param idRef The reference ID used to retrieve processed personal data.
     * @return A list of DataListTransferDTO objects containing secondary actors and their
     *         associated processed personal data.
     */
    List<DataListTransferDTO> getProcessedPersonalDataListTransfer(String idRef);
}
