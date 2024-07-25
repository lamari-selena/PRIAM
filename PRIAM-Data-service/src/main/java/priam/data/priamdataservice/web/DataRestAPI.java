/*
package priam.data.priamdataservice.web;

import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import priam.data.priamdataservice.dto.*;
import priam.data.priamdataservice.dto.transfer.DataListTransferDTO;
import priam.data.priamdataservice.services.DataServiceInterface;
import priam.data.priamdataservice.services.DataTypeServiceInterface;
import priam.data.priamdataservice.services.ProcessingServiceInterface;

import java.util.Collection;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api")
public class DataRestAPI {

    private final DataServiceInterface dataService;
    private final DataTypeServiceInterface dataTypeService;
    private final ProcessingServiceInterface processingService;

    @Autowired
    private KeycloakRestTemplate keycloakRestTemplate;

    public DataRestAPI(DataServiceInterface dataService, DataTypeServiceInterface dataTypeService, ProcessingServiceInterface processingService) {
        this.dataService = dataService;
        this.dataTypeService = dataTypeService;
        this.processingService = processingService;
    }

    @PostMapping(path = "/data")
    public DataResponseDTO save(@RequestBody DataRequestDTO dataRequestDTO) {
        HttpEntity<DataRequestDTO> requestEntity = new HttpEntity<>(dataRequestDTO);
        ResponseEntity<DataResponseDTO> response = keycloakRestTemplate.exchange("/api/data", HttpMethod.POST, requestEntity, DataResponseDTO.class);
        return response.getBody();
    }

    @PostMapping(path = "/datatype")
    public DataTypeResponseDTO save(@RequestBody DataTypeRequestDTO dataTypeRequestDTO) {
        HttpEntity<DataTypeRequestDTO> requestEntity = new HttpEntity<>(dataTypeRequestDTO);
        ResponseEntity<DataTypeResponseDTO> response = keycloakRestTemplate.exchange("/api/datatype", HttpMethod.POST, requestEntity, DataTypeResponseDTO.class);
        return response.getBody();
    }

    @GetMapping(path = "/datatype/data/{dataTypeId}")
    public String getDataTypeNameByDataTypeId(@PathVariable int dataTypeId) {
        ResponseEntity<String> response = keycloakRestTemplate.exchange("/api/datatype/data/{dataTypeId}", HttpMethod.GET, null, String.class, dataTypeId);
        return response.getBody();
    }

    @GetMapping(path = "/dataId/{dataName}")
    public int getIdByName(@PathVariable String dataName) {
        ResponseEntity<Integer> response = keycloakRestTemplate.exchange("/api/dataId/{dataName}", HttpMethod.GET, null, Integer.class, dataName);
        return response.getBody();
    }

    @GetMapping(path = "/getDataName/{dataId}")
    public String getDataName(@PathVariable int dataId) {
        ResponseEntity<String> response = keycloakRestTemplate.exchange("/api/getDataName/{dataId}", HttpMethod.GET, null, String.class, dataId);
        return response.getBody();
    }

    @GetMapping(path = "/personalDataList")
    public List<DataResponseDTO> personalDataList() {
        ResponseEntity<List<DataResponseDTO>> response = keycloakRestTemplate.exchange("/api/personalDataList", HttpMethod.GET, null, new ParameterizedTypeReference<List<DataResponseDTO>>() {});
        return response.getBody();
    }

    @GetMapping(path = "/personalData/{dataId}")
    public DataResponseDTO getPersonalData(@PathVariable int dataId) {
        ResponseEntity<DataResponseDTO> response = keycloakRestTemplate.exchange("/api/personalData/{dataId}", HttpMethod.GET, null, DataResponseDTO.class, dataId);
        return response.getBody();
    }

    @GetMapping(path = "/dataList")
    public List<DataResponseDTO> dataList() {
        ResponseEntity<List<DataResponseDTO>> response = keycloakRestTemplate.exchange("/api/dataList", HttpMethod.GET, null, new ParameterizedTypeReference<List<DataResponseDTO>>() {});
        return response.getBody();
    }

    @GetMapping(path = "/data/{dataSubjectCategory}")
    public List<DataResponseDTO> getPersonalDataByDataSubjectCategory(@PathVariable int dataSubjectCategory) {
        ResponseEntity<List<DataResponseDTO>> response = keycloakRestTemplate.exchange("/api/data/{dataSubjectCategory}", HttpMethod.GET, null, new ParameterizedTypeReference<List<DataResponseDTO>>() {}, dataSubjectCategory);
        return response.getBody();
    }

    @GetMapping(path = "/data/dataType:{dataTypeName}")
    public List<DataResponseDTO> getPersonalDataByDataType(@PathVariable String dataTypeName) {
        ResponseEntity<List<DataResponseDTO>> response = keycloakRestTemplate.exchange("/api/data/dataType:{dataTypeName}", HttpMethod.GET, null, new ParameterizedTypeReference<List<DataResponseDTO>>() {}, dataTypeName);
        return response.getBody();
    }

    @GetMapping(path = "/data/processedPersonalDataList/{idRef}")
    public List<ProcessedPersonalDataDTO> getProcessedPersonalDataList(@PathVariable String idRef) {
        ResponseEntity<List<ProcessedPersonalDataDTO>> response = keycloakRestTemplate.exchange("/api/data/processedPersonalDataList/{idRef}", HttpMethod.GET, null, new ParameterizedTypeReference<List<ProcessedPersonalDataDTO>>() {}, idRef);
        return response.getBody();
    }

    @GetMapping(path = "/data/processedPersonalDataList/purposes/{idRef}")
    public Collection<ProcessingPersonalDataDTO> getProcessingPersonalDataListPurposes(@PathVariable String idRef) {
        ResponseEntity<Collection<ProcessingPersonalDataDTO>> response = keycloakRestTemplate.exchange("/api/data/processedPersonalDataList/purposes/{idRef}", HttpMethod.GET, null, new ParameterizedTypeReference<Collection<ProcessingPersonalDataDTO>>() {}, idRef);
        return response.getBody();
    }

    @GetMapping(path = "/data/processedIndirectAndProducedPersonalDataList/{idRef}")
    public List<ProcessedIndirectAndProducedPersonalDataDTO> getProcessedIndirectAndProducedPersonalDataList(@PathVariable String idRef) {
        ResponseEntity<List<ProcessedIndirectAndProducedPersonalDataDTO>> response = keycloakRestTemplate.exchange("/api/data/processedIndirectAndProducedPersonalDataList/{idRef}", HttpMethod.GET, null, new ParameterizedTypeReference<List<ProcessedIndirectAndProducedPersonalDataDTO>>() {}, idRef);
        return response.getBody();
    }

    @GetMapping(path = "/data/processedPersonalDataList/transfer/{idRef}")
    public List<DataListTransferDTO> getProcessedPersonalDataListTransfer(@PathVariable String idRef) {
        ResponseEntity<List<DataListTransferDTO>> response = keycloakRestTemplate.exchange("/api/data/processedPersonalDataList/transfer/{idRef}", HttpMethod.GET, null, new ParameterizedTypeReference<List<DataListTransferDTO>>() {}, idRef);
        return response.getBody();
    }
}
*/

package priam.data.priamdataservice.web;

import org.springframework.web.bind.annotation.*;

import priam.data.priamdataservice.dto.*;
import priam.data.priamdataservice.dto.transfer.DataListTransferDTO;
import priam.data.priamdataservice.services.DataServiceInterface;
import priam.data.priamdataservice.services.DataTypeServiceInterface;
import priam.data.priamdataservice.services.ProcessingServiceInterface;

import java.util.Collection;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api")
public class DataRestAPI {

    private final DataServiceInterface dataService;
    private final DataTypeServiceInterface dataTypeService;
    private final ProcessingServiceInterface processingService;


    public DataRestAPI(DataServiceInterface dataService, DataTypeServiceInterface dataTypeService, ProcessingServiceInterface processingService) {
        this.dataService = dataService;
        this.dataTypeService = dataTypeService;
        this.processingService = processingService;
    }


/**
     * Save a new Data
     * @param dataRequestDTO Information of the Data
     * @return The created Data object
     */

    @PostMapping(path = "/data")
    public DataResponseDTO save(@RequestBody DataRequestDTO dataRequestDTO) { return this.dataService.save(dataRequestDTO); }

/**
     * Save a new DataType
     * @param dataTypeRequestDTO Information of the DataType
     * @return The created DataType object
     */

    @PostMapping(path = "/datatype")
    public DataTypeResponseDTO save(@RequestBody DataTypeRequestDTO dataTypeRequestDTO) { return this.dataTypeService.save(dataTypeRequestDTO); }

/**
     * Retrieve a DataType name from its ID
     * @param dataTypeId ID of the DataType
     * @return The name of the DataType
     */

    @GetMapping(path = "/datatype/data/{dataTypeId}")
    public String getDataTypeNameByDataTypeId(@PathVariable int dataTypeId) { return this.dataTypeService.getDataTypeNameByDataTypeId(dataTypeId); }


/**
     * Retrieve a Data ID from its name
     * @param dataName Name of the Data
     * @return ID of the Data
     */

    @GetMapping(path = "/dataId/{dataName}")
    public int getIdByName(@PathVariable String dataName) {
        return dataService.getIdByDataName(dataName);
    }


/**
     * Retrieve a Data name from its ID
     * @param dataId ID of the Data
     * @return Name of the Data
     */

    @GetMapping(path = "/getDataName/{dataId}")
    public String getDataName(@PathVariable int dataId) {
        return dataService.getDataNameById(dataId);
    }


/**
     * Retrieve the list of all personal data
     * @return A DataResponseDTO object List
     */

    @GetMapping(path = "/personalDataList")
    public List<DataResponseDTO> personalDataList() {
        return dataService.findAllPersonalData();
    }


/**
     * Retrieve a Data from its ID
     * @param dataId ID of the Data
     * @return The Data object
     */

    @GetMapping(path = "/personalData/{dataId}")
    public DataResponseDTO getPersonalData(@PathVariable int dataId) {
        return dataService.getData(dataId);
    }


/**
     * Retrieve all Data
     * @return A DataResponseDTO object List
     */

    @GetMapping(path = "/dataList")
    public List<DataResponseDTO> dataList() {
        return dataService.findAllData();
    }


/**
     * Retrieve all Data from the DataSubjectCategory ID
     * @param dataSubjectCategory ID of the DataSubjectCategory
     * @return A DataResponseDTO object List
     */

    @GetMapping(path = "/data/{dataSubjectCategory}")
    public List<DataResponseDTO> getPersonalDataByDataSubjectCategory(@PathVariable int dataSubjectCategory) {
        return dataService.findAllDataByDataSubjectCategory(dataSubjectCategory);
    }


/**
     * Retrieve all Data from the DataType name
     * @param dataTypeName Name of the DataType
     * @return A DataResponseDTO object List
     */

    @GetMapping(path = "/data/dataType:{dataTypeName}")
    public List<DataResponseDTO> getPersonalDataByDataType(@PathVariable String dataTypeName) {
        return dataService.getPersonalDataByDataTypeName(dataTypeName);
    }


/**
     * Retrieve all personal Data obout a user by its idRef
     * @param idRef Reference ID of the user
     * @return A ProcessedPersonalDataDTO object List
     */

    @GetMapping(path = "/data/processedPersonalDataList/{idRef}")
    public List<ProcessedPersonalDataDTO> getProcessedPersonalDataList(@PathVariable String idRef) {
        return dataService.getProcessedPersonalDataList(idRef);
    }


/**
     * Retrieve processing purposes information about a user by its idRef
     * @param idRef Reference ID of the user
     * @return A ProcessingPersonalDataDTO object List
     */

    @GetMapping(path = "/data/processedPersonalDataList/purposes/{idRef}")
    public Collection<ProcessingPersonalDataDTO> getProcessingPersonalDataListPurposes(@PathVariable String idRef) {
        return processingService.getProcessingPersonalDataListPurposes(idRef);
    }


/**
     * Retrieve the indirect and produced personal data abour a user by its idRef
     * @param idRef Reference ID of the user
     * @return A ProcessedIndirectAndProducedPersonalDataDTO object List
     */

    @GetMapping(path = "/data/processedIndirectAndProducedPersonalDataList/{idRef}")
    public List<ProcessedIndirectAndProducedPersonalDataDTO> getProcessedIndirectAndProducedPersonalDataList(@PathVariable String idRef) {
        return dataService.getProcessedIndirectAndProducedPersonalDataList(idRef);
    }


/**
     * Retrieve the list of secondary actors for whom the processed data is transferred,
     * and the list of processed data transferred to them.
     * @param idRef the ID of the user
     * @return the list of secondary actors
     **/

    @GetMapping(path = "/data/processedPersonalDataList/transfer/{idRef}")
    public List<DataListTransferDTO> getProcessedPersonalDataListTransfer(@PathVariable String idRef) {
        return dataService.getProcessedPersonalDataListTransfer(idRef);
    }

}

