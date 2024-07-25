package priam.right.web;

import org.bouncycastle.cert.ocsp.Req;
import org.springframework.web.bind.annotation.*;
import priam.right.dto.*;
import priam.right.entities.DataRequestAnswer;
import priam.right.enums.DataRequestType;
import priam.right.services.DataRequestService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api", produces = "application/json")
public class DataRequestRestController {
    private final DataRequestService dataRequestService;

    public DataRequestRestController(DataRequestService dataRequestService) {
        this.dataRequestService = dataRequestService;
    }

    /**
     * Retrieve a DataRequest object based on its ID
     * @param dataRequestId ID of the DataRequest
     * @return The DataRequest object
     */
    @GetMapping(path = "right/dataRequest/{dataRequestId}")
    public DataRequestResponseDTO getDataRequest(@PathVariable int dataRequestId) {
        return dataRequestService.getDataRequest(dataRequestId);
    }
    /**
     * Save a new access DataRequest
     * @param accessRequestRequestDTO A AccessRequestRequestDTO object
     * @return A DataRequestResponseDTO object
     */
    @PostMapping(path = "/right/accessRequest")
    public DataRequestResponseDTO saveAccessRequest(@RequestBody AccessRequestRequestDTO accessRequestRequestDTO) {
        return dataRequestService.saveAccessRequest(accessRequestRequestDTO);
    }
    /**
     * Save a new rectification DataRequest
     * @param dataRequestDTO A dataRequestDTO object
     * @return A DataRequestResponseDTO object
     */
    @PostMapping(path = "/right/rectificationRequest")
    public DataRequestResponseDTO saveRectificationRequest(@RequestBody DataRequestRequestDTO dataRequestDTO) {
        return dataRequestService.saveDataRequest(dataRequestDTO, DataRequestType.RECTIFICATION);
    }
    /**
     * Save a new erasure DataRequest
     * @param dataRequestDTO A dataRequestDTO object
     * @return A DataRequestResponseDTO object
     */
    @PostMapping(path = "/right/erasureRequest")
    public DataRequestResponseDTO saveErasureRequest(@RequestBody DataRequestRequestDTO dataRequestDTO) {
        return dataRequestService.saveDataRequest(dataRequestDTO, DataRequestType.ERASURE);
    }
    /**
     * Save a new DataRequestAnswer
     * @param requestAnswer A RequestAnswerRequestDTO object
     * @return The created DataRequestAnswer object
     */
    @PostMapping(path = "/right/answer")
    public DataRequestAnswer saveRequestAnswer(@RequestBody RequestAnswerRequestDTO requestAnswer) {
        return dataRequestService.saveRequestAnswer(requestAnswer);
    }
    /**
     * Retrieve a DataRequestAnswer object based on its ID
     * @param dataRequestId The DataRequest ID
     * @return The DataRequestAnswer object
     */
    @GetMapping(path = "right/answer/{dataRequestId}")
    public DataRequestAnswer getRequestAnswer(@PathVariable int dataRequestId) {
        return dataRequestService.getRequestAnswerByDataRequestId(dataRequestId);
    }

    /**
     * Retrieve the list of DataRequest of a DataSubject by its ID
     * @param dataSubjectId ID of the DataSubject
     * @return A DataRequestResponseDTO object List
     */
    @GetMapping(path = "/requestsRectification/{dataSubjectId}")
    public List<DataRequestResponseDTO> getListDataRequestByDataSubject(@PathVariable int dataSubjectId) {
        return dataRequestService.getListDataRequestByDataSubjectId(dataSubjectId);
    }

    /**
     * Retrieve the values of attributes of a dataType by a DataSubject ID
     * @param dataSubjectId ID of the DataSubject
     * @param dataTypeName Name of the DataType of the attributes
     * @param attributes List of attribute name
     * @return List of Map with the form <value, the value>
     */
    @GetMapping(path = "/personalDataValues/accessRight")
    public List<Map<String, String>> DataAccess(@RequestParam int dataSubjectId, @RequestParam String dataTypeName, @RequestParam List<String> attributes) {
        System.out.println("recupération c bn " + dataSubjectId + dataTypeName + attributes);
        return dataRequestService.DataAccess(dataSubjectId, dataTypeName, attributes);
    }

    /**
     * Retrieve the status of a data, if it had been recently accepted in a Access Request or not for a specific DataSubject
     * @param dataSubjectId ID of the DataSubject
     * @param dataId ID of the Data
     * @return A boolean
     */
    @GetMapping(path = "/isAccepted")
    public boolean isDataRequestAcceptedForDataId(@RequestParam int dataSubjectId, @RequestParam int dataId) {
        return dataRequestService.isAccepted(dataSubjectId, dataId);
    }

    /**
     * Retrieve a list of DataRequest based on filters
     * @param listOfSelectedTypeDataRequests List of String
     * @param listOfSelectedStatus List of String
     * @param listOfSelectedDataSubjectCategories List of String
     * @return A RequestListDTO object List
     */
    @GetMapping(path = "right/requestList")
    public List<RequestListDTO> getRequestListByFilters(@RequestParam Optional<List<String>> listOfSelectedTypeDataRequests, @RequestParam Optional<List<String>> listOfSelectedStatus, @RequestParam Optional<List<String>> listOfSelectedDataSubjectCategories) {
        return dataRequestService.getDataRequestByFilters(listOfSelectedTypeDataRequests.orElse(new ArrayList<>()), listOfSelectedStatus.orElse(new ArrayList<>()), listOfSelectedDataSubjectCategories.orElse(new ArrayList<>()));
    }

    /**
     * Retrieve information on a DataRequest based on its ID
     * @param dataRequestId ID of the DataRequest
     * @return A RequestDetailDTO object
     */
    @GetMapping(path = "right/requestDetail/{dataRequestId}")
    public RequestDetailDTO getRequestDetail(@PathVariable int dataRequestId) {
        return dataRequestService.getRequestDataDetail(dataRequestId);
    }
}
