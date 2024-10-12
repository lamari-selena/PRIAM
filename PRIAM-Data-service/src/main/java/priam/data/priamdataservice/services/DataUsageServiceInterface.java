package priam.data.priamdataservice.services;


import priam.data.priamdataservice.dto.DataUsageResponseDTO;
import priam.data.priamdataservice.entities.DataUsage;

import java.util.Collection;

public interface DataUsageServiceInterface {
    /**
     * Save a new DataUsage
     * @param dataUsage Information of the DataUsage
     * @return The created DataUsage object
     */
    DataUsage createDataUsage(DataUsage dataUsage);
    /**
     * Update a DataUsage
     * @param dataUsage The new information of the DataUsage object
     * @return The updated DataUsage object
     */
    DataUsage updateDataUsage(DataUsage dataUsage);

    /**
     * Delete a DataUsage by its ID
     * @param dataUsageId ID of the DataUsage
     * @return True if deletion successful
     */
    boolean deleteDataUsage(Integer dataUsageId);
    /**
     * Retrieve a DataUsage object by its ID
     * @param dataUsageId ID of the DataUsage
     * @return A DataUsage object
     */
    DataUsageResponseDTO getDataUsage(Integer dataUsageId);
    /**
     * Retrieve all DataUsage of a processing by the processing ID
     * @param processingId ID of the processing
     * @return A DataUsage object List
     */
    Collection<DataUsage> getDataUsages(int processingId);
}
