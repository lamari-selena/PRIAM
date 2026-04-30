package priam.actor.services;

import priam.actor.dto.DataSubjectCategoryRequestDTO;
import priam.actor.dto.DataSubjectCategoryResponseDTO;
import priam.actor.dto.DataSubjectRequestDTO;
import priam.actor.dto.DataSubjectResponseDTO;

import java.util.List;

public interface DataSubjectService {
    /**
     * Save a new data subject
     *
     * @param dataSubjectRequestDTO A DataSubjectRequestDTO object
     * @return A DataSubjectResponseDTO object
     */
    DataSubjectResponseDTO saveDataSubject(DataSubjectRequestDTO dataSubjectRequestDTO);

    /**
     * Retrieve a DataSubjectResponseDTO object based on a data subject ID
     *
     * @param dataSubjectId The data subject ID
     * @return A DataSubjectResponseDTO object
     */
    DataSubjectResponseDTO findDataSubject(int dataSubjectId);

    /**
     * Retrieve a data subject ID based on a reference ID
     *
     * @param idRef The reference ID
     * @return A data subject ID
     */
    int getDataSubjectIdByIdRef(String idRef);

    /**
     * Retrieve a DataSubjectResponseDTO based on a reference ID
     *
     * @param idRef The reference ID
     * @return A DataSubjectResponseDTO object
     */
    DataSubjectResponseDTO getDataSubjectByIdRef(String idRef);

    /**
     * Save a new data subject category
     *
     * @param dataSubjectCategoryRequestDTO A DataSubjectCategoryRequestDTO object
     * @return A DataSubjectCategoryResponseDTO object
     */
    DataSubjectCategoryResponseDTO saveDataSubjectCategory(DataSubjectCategoryRequestDTO dataSubjectCategoryRequestDTO);

    /**
     * Retrieve all the data subject category in a list of DataSubjectCategoryResponseDTO objects
     * @return A list of DataSubjectCategoryResponseDTO objects
     */
    List<DataSubjectCategoryResponseDTO> getAllDataSubjectCategories();

    /**
     * Retrieve a DataSubjectCategoryResponseDTO object based on a data subject category ID
     * @param dataSubjectCategoryId The data subject category ID
     * @return A DataSubjectCategoryResponseDTO object
     */
    DataSubjectCategoryResponseDTO getDataSubjectCategoryById(int dataSubjectCategoryId);
}
