package priam.data.priamdataservice.services;

import priam.data.priamdataservice.dto.ProcessingPersonalDataDTO;
import priam.data.priamdataservice.dto.ProcessingRequestDTO;
import priam.data.priamdataservice.dto.ProcessingResponseDTO;
import priam.data.priamdataservice.entities.Processing;

import java.util.Collection;

public interface ProcessingServiceInterface {
    /**
     * Save a new Processing
     * @param processingRequestDTO Information of the Processing
     * @return The created Processing object
     */
    Processing createProcessing(ProcessingRequestDTO processingRequestDTO);
    /**
     * Update a Processing object
     * @param processingId ID of the Processing
     * @param processingRequestDTO The new information of the Processing
     * @return The updated Processing object
     */
    ProcessingResponseDTO updateProcessing(Integer processingId, ProcessingRequestDTO processingRequestDTO);
    /**
     * Delete a Processing by its ID
     * @param processingId ID of the Processing
     * @return True if deletion successful
     */
    boolean deleteProcessing(Integer processingId);
    /**
     * Retrieve a Processing object by its ID
     * @param processingId ID of the Processing
     * @return A Processing object
     */
    ProcessingResponseDTO getProcessing(Integer processingId);
    /**
     * Retrieve all Processing
     * @return A Processing object List
     */
    Collection<Processing> getProcessings();
    /**
     * Retrieve a list of Processing object by the DataSubjectCategoryID
     * @param dataSubjectCategoryId ID of the DataSubjectCategory
     * @return A ProcessingResponseDTO object List
     */

    Collection<ProcessingResponseDTO> getProcessingsByDataSubjectCategoryId(int dataSubjectCategoryId);
    /**
     * Retrieve processing purposes information about a user by its idRef
     * @param idRef Reference ID of the user
     * @return A ProcessingPersonalDataDTO object List
     */

    Collection<ProcessingPersonalDataDTO> getProcessingPersonalDataListPurposes(String idRef);

}
