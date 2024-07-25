package priam.right.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import priam.right.dto.*;
import priam.right.entities.*;
import priam.right.enums.AnswerType;
import priam.right.enums.StatusDataRequestType;
import priam.right.enums.DataRequestType;
import priam.right.mappers.DataRequestMapper;
import priam.right.openfeign.DataRestClient;
import priam.right.openfeign.ActorRestClient;

import priam.right.openfeign.ProviderRestClient;
import priam.right.repositories.DataRequestDataRepository;
import priam.right.repositories.DataRequestPrimaryKeyRepository;
import priam.right.repositories.DataRequestRepository;
import priam.right.repositories.RequestAnswerRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)

@Service
@Transactional
@AllArgsConstructor
public class DataRequestServiceImpl implements DataRequestService {
    private DataRequestRepository dataRequestRepository;
    private DataRequestMapper dataRequestMapper;
    private DataRestClient dataRestClient;
    private ActorRestClient actorRestClient;
    private ProviderRestClient providerRestClient;
    private RequestAnswerRepository requestAnswerRepository;
    private DataRequestDataRepository dataRequestDataRepository;
    private DataRequestPrimaryKeyRepository dataRequestPrimaryKeyRepository;

    /**
     * Retrieve the values of attributes of a dataType by a DataSubject ID
     * @param dataSubjectId ID of the DataSubject
     * @param dataTypeName Name of the DataType of the attributes
     * @param attributes List of attribute name
     * @return List of Map with the form <value, the value>
     */
    public List<Map<String, String>> DataAccess(int dataSubjectId, String dataTypeName, List<String> attributes){
        return providerRestClient.getPersonalDataValues(dataSubjectId, dataTypeName, attributes);
    }

    /**
     * Save a new erasure OR rectification DataRequest
     * @param dataRequestDTO A dataRequestDTO object
     * @return A DataRequestResponseDTO object
     */
    @Override
    public DataRequestResponseDTO saveDataRequest(DataRequestRequestDTO dataRequestDTO, DataRequestType dataRequestType) {
        DataRequest dataRequest = new DataRequest();
        dataRequest.setDataSubjectId(dataRequestDTO.getDataSubjectId());
        dataRequest.setNewValue(dataRequestDTO.getNewValue());
        dataRequest.setDataRequestClaim(dataRequestDTO.getClaim());

        dataRequest.setDataRequestType(dataRequestType);
        dataRequest.setDataRequestIssuedAt(new Date());
        dataRequest.setResponse(false);
        dataRequest.setIsolated(true);

        HashMap<Integer, String> primaryKeys = dataRequestDTO.getPrimaryKeys();

        Data data = dataRestClient.getDataById(dataRequestDTO.getDataId());

        DataSubject dataSubject = actorRestClient.getDataSubject(dataRequestDTO.getDataSubjectId());
        dataRequest.setDataSubject(dataSubject);

        DataRequest result = dataRequestRepository.save(dataRequest);

        // DataRequestData
        DataRequestData drd = new DataRequestData(result.getDataRequestId(), data.getDataId(), false);
        dataRequestDataRepository.save(drd);

        ArrayList<Data> datas = new ArrayList<>();
        datas.add(data);

        // PrimaryKeys
        primaryKeys.forEach((id, value)-> {
            DataRequestPrimaryKey dataRequestPrimaryKey = new DataRequestPrimaryKey(result.getDataRequestId(), id, value);
            dataRequestPrimaryKeyRepository.save(dataRequestPrimaryKey);
        });

        // Response DTO
        DataRequestResponseDTO response = new DataRequestResponseDTO(result, datas, primaryKeys);
        return response;
    }

    /**
     * Save a new access DataRequest
     * @param accessRequestRequestDTO A AccessRequestRequestDTO object
     * @return A DataRequestResponseDTO object
     */
    @Override
    public DataRequestResponseDTO saveAccessRequest(AccessRequestRequestDTO accessRequestRequestDTO) {
        DataRequest dataRequest = new DataRequest();

        dataRequest.setDataSubjectId(accessRequestRequestDTO.getDataSubjectId());
        dataRequest.setDataRequestIssuedAt(new Date());
        dataRequest.setDataRequestClaim(accessRequestRequestDTO.getDataRequestClaim());
        dataRequest.setDataRequestType(DataRequestType.ACCESS);

        dataRequest.setResponse(false);
        dataRequest.setIsolated(true);
        dataRequest.setNewValue(null);

        DataRequest result = dataRequestRepository.save(dataRequest);

        ArrayList<Data> datas = new ArrayList<>();


        // Save all DataRequestData
        accessRequestRequestDTO.getData().forEach(dataId -> {
            DataRequestData drd = new DataRequestData(result.getDataRequestId(), dataId.getDataId(), false);
            dataRequestDataRepository.save(drd);
            Data data = dataRestClient.getDataById(dataId.getDataId());
            datas.add(data);
        });

        // Response DTO
        DataRequestResponseDTO response = new DataRequestResponseDTO(result, datas, null);
        return response;
    }

    /**
     * Retrieve a DataRequest object based on its ID
     * @param dataRequestId ID of the DataRequest
     * @return The DataRequest object
     */
    @Override
    public DataRequestResponseDTO getDataRequest(int dataRequestId) {
        DataRequest dataRequest = dataRequestRepository.getById(dataRequestId);
        ArrayList<Data> datas = new ArrayList<>();

        List<Integer> dataIds = new ArrayList<>();
        dataIds = dataRequestDataRepository.findDataIdsByDataRequestId(dataRequestId);
        dataIds.forEach(dataId -> {
            Data data = dataRestClient.getDataById(dataId);
            datas.add(data);
        });

        DataSubject dataSubject = actorRestClient.getDataSubject(dataRequest.getDataSubjectId());
        dataRequest.setDataSubject(dataSubject);

        // Response DTO
        DataRequestResponseDTO response = new DataRequestResponseDTO(dataRequest, datas, null);
        return response;
    }

    /**
     * Retrieve the list of DataRequest of a DataSubject by its ID
     * @param dataSubjectId ID of the DataSubject
     * @return A DataRequestResponseDTO object List
     */
    @Override
    public List<DataRequestResponseDTO> getListDataRequestByDataSubjectId(int dataSubjectId) {
        List<DataRequestResponseDTO> response = new ArrayList<>();
        List<DataRequest> dataRequestList = dataRequestRepository.findByDataSubjectId(dataSubjectId);

        for (DataRequest dataRequest : dataRequestList)
        {
            // Get all Data object
            ArrayList<Data> datas = new ArrayList<>();
            List<Integer> dataIds = new ArrayList<>();
            dataIds = dataRequestDataRepository.findDataIdsByDataRequestId(dataRequest.getDataRequestId());
            dataIds.forEach(dataId -> {
                Data data = dataRestClient.getDataById(dataId);
                datas.add(data);
            });

            DataSubject dataSubject = actorRestClient.getDataSubject(dataRequest.getDataSubjectId());
            dataRequest.setDataSubject(dataSubject);
            response.add(new DataRequestResponseDTO(dataRequest, datas, null));
        }

        return response;
    }

    /**
     * Save a new DataRequestAnswer
     * @param requestAnswer A RequestAnswerRequestDTO object
     * @return The created DataRequestAnswer object
     */
    @Override
    public DataRequestAnswer saveRequestAnswer(RequestAnswerRequestDTO requestAnswerRequestDTO) {
        DataRequest dataRequest = dataRequestRepository.getById(requestAnswerRequestDTO.getDataRequestId());

        DataRequestAnswer requestAnswer = new DataRequestAnswer();
        requestAnswer.setDataRequest(dataRequest);
        requestAnswer.setDataRequestClaim(requestAnswerRequestDTO.getProviderClaim());

        ArrayList<DataRequestData> drdList = new ArrayList<>(dataRequestDataRepository.findDataRequestDataByDataRequestId(requestAnswerRequestDTO.getDataRequestId()));

        // Set answer boolean for each data request data
        // For rectification and erasure, only one data is involved
        if(dataRequest.getDataRequestType().equals(DataRequestType.RECTIFICATION) || dataRequest.getDataRequestType().equals(DataRequestType.ERASURE)) {
            DataRequestData drd = drdList.get(0);
            drd.setAnswerByData(requestAnswerRequestDTO.isAnswer());
            dataRequestDataRepository.save(drd);

            requestAnswer.setAnswer(requestAnswerRequestDTO.isAnswer() ? AnswerType.FULL : AnswerType.REFUSED);

            // Apply the rectification or the erasure in the provider application :
            //  - Get Data
            Data data = dataRestClient.getDataById(drd.getDataId());
            String dataTypeName= data.getDataType().getDataTypeName();
            String dataName = data.getDataName();
            String newValue= dataRequest.getNewValue();

            DataSubject dataSubject = actorRestClient.getDataSubject(dataRequest.getDataSubjectId());
            String idRef = dataSubject.getIdRef();

            // - Get Primary Key
            Map<String, String> primaryKeys = new HashMap<>();
            List<DataRequestPrimaryKey> listPrimaryKeys = dataRequestPrimaryKeyRepository.findByDataRequestId(dataRequest.getDataRequestId());
            listPrimaryKeys.forEach(pk -> {
                Data pkData = dataRestClient.getDataById(drd.getDataId());
                primaryKeys.put(pkData.getDataName(), pk.getPrimaryKeyValue());
            });
            // - Apply

            if(!requestAnswer.getAnswer().equals(AnswerType.REFUSED)) {
                if(dataRequest.getDataRequestType().equals(DataRequestType.RECTIFICATION)) {
                    RectificationRequestDTO rectificationRequestDTO = new RectificationRequestDTO(idRef, dataName, dataTypeName, newValue, primaryKeys);
                    providerRestClient.rectification(rectificationRequestDTO);
                }
                else if(dataRequest.getDataRequestType().equals(DataRequestType.ERASURE)) {
                    ErasureRequestDTO erasureRequestDTO = new ErasureRequestDTO(idRef, dataName, dataTypeName, primaryKeys);
                    providerRestClient.erasure(erasureRequestDTO);
                }
            }
        }
        else if (dataRequest.getDataRequestType().equals(DataRequestType.ACCESS)) {
            List<Integer> dataIdList = requestAnswerRequestDTO.getData().stream().map(d -> d.getDataId()).collect(Collectors.toList());
            // We look if the dataRequestData is present in the datas send in the request
            drdList.forEach(drd -> {
                if(dataIdList.contains(drd.getDataId())) {
                    drd.setAnswerByData(true);
                }
                else {
                    drd.setAnswerByData(false);
                }
            });
            if(drdList.size() == requestAnswerRequestDTO.getData().size()) {
                requestAnswer.setAnswer(AnswerType.FULL);
            }
            else if(requestAnswerRequestDTO.getData().size() > 0) {
                requestAnswer.setAnswer(AnswerType.PARTIAL);
            }
            else {
                requestAnswer.setAnswer(AnswerType.REFUSED);
            }
        }
        // Save request answer
        return requestAnswerRepository.save(requestAnswer);
    }

    /**
     * Retrieve the status of a data, if it had been recently accepted in a Access Request or not for a specific DataSubject
     * @param dataSubjectId ID of the DataSubject
     * @param dataId ID of the Data
     * @return A boolean
     */
    @Override
    public boolean isAccepted(int dataSubjectId, int dataId) {
        Optional<Boolean> isAccepted = dataRequestDataRepository.isDataAcceptedByDataSubjectIdAndDataId(dataSubjectId, dataId);
        return isAccepted.orElse(false);
    }

    /**
     * Retrieve a list of DataRequest based on filters
     * @param listOfSelectedTypeDataRequests List of String
     * @param listOfSelectedStatus List of String
     * @param listOfSelectedDataSubjectCategories List of String
     * @return A RequestListDTO object List
     */
    @Override
    public List<RequestListDTO> getDataRequestByFilters(List<String> listOfSelectedTypeDataRequests, List<String> listOfSelectedStatus, List<String> listOfSelectedDataSubjectCategories) {
        List<RequestListDTO> response = new ArrayList<>();
        // Filter with Type of DataRequest
        List<DataRequest> filteredTypeList;
        if(listOfSelectedTypeDataRequests.isEmpty()) {
            filteredTypeList = dataRequestRepository.findAll();
        }
        else {
            filteredTypeList = new ArrayList<>();
            ArrayList<Integer> indexOfEnumType = new ArrayList<>(listOfSelectedTypeDataRequests.stream().map(type ->DataRequestType.valueOf(type).ordinal()).collect(Collectors.toList()));
            filteredTypeList = dataRequestRepository.findByTypes(indexOfEnumType);
        }

        // Filter with status of DataRequest
        List<DataRequest> filteredStatusList;
        if(listOfSelectedStatus.isEmpty()) {
            filteredStatusList = filteredTypeList;
        }
        else {
            filteredStatusList = new ArrayList<>();
            filteredTypeList.forEach(dataRequest -> {
                Optional<DataRequestAnswer> answer = requestAnswerRepository.findDataRequestAnswerByDataRequest_DataRequestId(dataRequest.getDataRequestId());
                // First case : If looking for validated or refused requests
                if(answer.isPresent()) {
                    AnswerType answerType = answer.get().getAnswer();
                    if(listOfSelectedStatus.contains(answerType.toString())) {
                        filteredStatusList.add(dataRequest);
                    } // If we just want completed answer (so no difference between full or partial answer)
                    else if(listOfSelectedStatus.contains(StatusDataRequestType.COMPLETED.toString())) {
                        if(answerType.equals(AnswerType.FULL) || answerType.equals(AnswerType.PARTIAL)) {
                            filteredStatusList.add(dataRequest);
                        }
                    }
                }
                // Second case : looking for in progess requests (so no answer yet)
                else if(listOfSelectedStatus.contains("In Progress")) {
                    filteredStatusList.add(dataRequest);
                }
            });
        }

        // Filter with DataSubjectCategory
        filteredStatusList.forEach(dataRequest -> {
            DataSubjectCategoryResponseDTO category = actorRestClient.getDataSubjectCategoryById(dataRequest.getDataSubjectId());
            RequestListDTO r = new RequestListDTO();
            r.setDataRequestId(dataRequest.getDataRequestId());
            r.setResponse(dataRequest.isResponse());
            r.setDataRequestType(dataRequest.getDataRequestType());
            r.setDataRequestIssuedAt(dataRequest.getDataRequestIssuedAt());
            r.setDataSubjectCategory(category);
            
            if(listOfSelectedDataSubjectCategories.isEmpty()) {
                response.add(r);
            }
            else if(listOfSelectedDataSubjectCategories.contains(category.getDataSubjectCategoryName())) {
                response.add(r);
            }
        });

        return response;
    }

    /**
     * Retrieve information on a DataRequest based on its ID
     * @param dataRequestId ID of the DataRequest
     * @return A RequestDetailDTO object
     */
    @Override
    public RequestDetailDTO getRequestDataDetail(int dataRequestId) {
        RequestDetailDTO response = new RequestDetailDTO();

        // DataRequest information
        DataRequest dataRequest = dataRequestRepository.getById(dataRequestId);
        response.setDataRequestId(dataRequest.getDataRequestId());
        response.setDataRequestClaim(dataRequest.getDataRequestClaim());
        response.setResponse(dataRequest.isResponse());
        response.setIsolated(dataRequest.isIsolated());
        response.setDataRequestIssuedAt(dataRequest.getDataRequestIssuedAt());
        response.setNewValue(dataRequest.getNewValue());
        response.setDataRequestType(dataRequest.getDataRequestType());

        // DataSubject information
        DataSubject dataSubject = actorRestClient.getDataSubject(dataRequest.getDataSubjectId());
        DataSubjectCategoryResponseDTO category = actorRestClient.getDataSubjectCategoryById(dataRequest.getDataSubjectId());
        response.setDataSubject(dataSubject.getDataSubjectId(), dataSubject.getIdRef(), category.getDataSubjectCategoryName());

        // Data information
        List<DataRequestData> dataRequestDatas = dataRequestDataRepository.findDataRequestDataByDataRequestId(dataRequestId);
        dataRequestDatas.forEach(drd -> {
            Data data = dataRestClient.getDataById(drd.getDataId());
            String dataTypeName = dataRestClient.getDataTypeNameByDataTypeId(data.getDataType().getDataTypeId());
            // Primary keys
            Map<String, String> primaryKeys = new HashMap<>();
            if(dataRequest.getDataRequestType() == DataRequestType.RECTIFICATION || dataRequest.getDataRequestType() == DataRequestType.ERASURE) {
                ArrayList<DataRequestPrimaryKey> list = new ArrayList<>(this.dataRequestPrimaryKeyRepository.findByDataRequestId(dataRequestId));
                list.forEach(primaryKey -> {
                    Data d = dataRestClient.getDataById(primaryKey.getPrimaryKeyId());
                    primaryKeys.put(d.getDataName(), primaryKey.getPrimaryKeyValue());
                });
            }

            response.addData(dataTypeName, data.getDataId(), data.getDataName(), drd.isAnswerByData(), primaryKeys);
        });

        return response;
    }

    /**
     * Retrieve a DataRequestAnswer object based on its ID
     * @param dataRequestId The DataRequest ID
     * @return The DataRequestAnswer object
     */
    @Override
    public DataRequestAnswer getRequestAnswerByDataRequestId(int requestId) {
        Optional<DataRequestAnswer> res = requestAnswerRepository.findDataRequestAnswerByDataRequest_DataRequestId(requestId);
        return res.orElse(null);
    }
}

