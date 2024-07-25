package priam.data.priamdataservice.web;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import priam.data.priamdataservice.entities.DataUsage;
import priam.data.priamdataservice.services.DataUsageServiceInterface;
import priam.data.priamdataservice.dto.DataUsageResponseDTO;

import java.util.Collection;

@RestController
@RequestMapping("processing/data-usage")
@AllArgsConstructor
public class DataUsageController {
    @Autowired
    DataUsageServiceInterface dataUsageService;

    /**
     * Save a new DataUsage
     * @param dataUsage Information of the DataUsage
     * @return The created DataUsage object
     */
    @PostMapping("/create")
    public DataUsage newDataUsage(DataUsage dataUsage) {
        return dataUsageService.createDataUsage(dataUsage);
    }

    /**
     * Update a DataUsage
     * @param dataUsage The new information of the DataUsage object
     * @return The updated DataUsage object
     */
    @PutMapping("/update")
    public DataUsage modifyDataUsage(DataUsage dataUsage) {
        return dataUsageService.updateDataUsage(dataUsage);
    }

    /**
     * Retrieve all DataUsage of a processing by the processing ID
     * @param processingId ID of the processing
     * @return A DataUsage object List
     */
    @GetMapping("/")
    public Collection<DataUsage> getDataUsages(int processingId){
        return dataUsageService.getDataUsages(processingId);
    }

    /**
     * Retrieve a DataUsage object by its ID
     * @param dataUsageId ID of the DataUsage
     * @return A DataUsage object
     */
    @GetMapping("/{dataUsageId}")
    public DataUsageResponseDTO getDataUsage(@PathVariable Integer dataUsageId) {
        return dataUsageService.getDataUsage(dataUsageId);
    }

}