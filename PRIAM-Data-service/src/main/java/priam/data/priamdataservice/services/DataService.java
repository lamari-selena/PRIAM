package priam.data.priamdataservice.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.*;
import priam.data.priamdataservice.dto.transfer.DataListTransferDTO;
import priam.data.priamdataservice.dto.transfer.SecondaryActorCategoryDTO;
import priam.data.priamdataservice.entities.DataSubjectCategory;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.SecondaryActor;
import priam.data.priamdataservice.enums.Source;
import priam.data.priamdataservice.mappers.DataMapper;
import priam.data.priamdataservice.mappers.DataTypeMapper;
import priam.data.priamdataservice.mappers.PersonalDataTransferMapper;
import priam.data.priamdataservice.openfeign.ActorRestClient;
import priam.data.priamdataservice.openfeign.ProviderRestClient;
import priam.data.priamdataservice.openfeign.RightRestClient;
import priam.data.priamdataservice.repositories.DataRepository;
import priam.data.priamdataservice.repositories.DataTypeRepository;
import priam.data.priamdataservice.repositories.ProcessedDataRepository;
import priam.data.priamdataservice.repositories.transfer.PersonalDataTransferRepository;
import priam.data.priamdataservice.repositories.transfer.SecondaryActorRepository;

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
public class DataService implements DataServiceInterface {
    final DataRepository dataRepository;
    final DataMapper dataMapper;

    final DataTypeMapper dataTypeMapper;
    final PersonalDataTransferMapper transferMapper;
    final ActorRestClient actorRestClient;
    final RightRestClient rightRestClient;
    final ProviderRestClient providerRestClient;

    final DataTypeRepository dataTypeRepository;
    final ProcessedDataRepository processedDataRepository;
    final SecondaryActorRepository secondaryActorRepository;
    private final PersonalDataTransferRepository personalDataTransferRepository;

    @Override
    public DataResponseDTO save(DataRequestDTO dataRequestDTO) {
        Data data = dataMapper.DataRequestDTOToData(dataRequestDTO);
        Data saveData = dataRepository.save(data);
        return dataMapper.DataToDataResponseDTO(saveData);
    }

    @Override
    public DataResponseDTO update(DataRequestDTO dataRequestDTO) {
        Data data = dataMapper.DataRequestDTOToData(dataRequestDTO);
        Data updatedData = dataRepository.save(data);
        return dataMapper.DataToDataResponseDTO(updatedData);
    }

    @Override
    public DataResponseDTO getData(int dataId) {
        Data data = dataRepository.findByDataId(dataId).get();
        System.out.println("Data: " + data);
        System.out.println("DataSubjectCategory: " + data.getDataSubjectCategory());
        DataSubjectCategory dataSubjectCategory = actorRestClient.getDataSubjectCategoryById(data.getDataSubjectCategoryId());
        data.setDataSubjectCategory(dataSubjectCategory);
        DataResponseDTO dataResponseDTO = dataMapper.DataToDataResponseDTO(data);

        return dataResponseDTO;
    }

    @Override
    public List<DataResponseDTO> findAllPersonalData() {
        List<Data> dataList = dataRepository.findAllByIsPersonal(true);
        for (Data datum : dataList) {
            DataSubjectCategory dataSubjectCategory = actorRestClient.getDataSubjectCategoryById(datum.getDataSubjectCategory().getDataSubjectCategoryId());
            datum.setDataSubjectCategory(dataSubjectCategory);
        }
        List<DataResponseDTO> dataResponseDTOS = dataList
                .stream().map(datum -> dataMapper.DataToDataResponseDTO(datum))
                .collect(Collectors.toList());
        return dataResponseDTOS;
    }

    @Override
    public List<DataResponseDTO> findAllData() {
        List<Data> dataList = dataRepository.findAll();
        for (Data datum : dataList) {
            DataSubjectCategory dataSubjectCategory = actorRestClient.getDataSubjectCategoryById(datum.getDataSubjectCategory().getDataSubjectCategoryId());
            datum.setDataSubjectCategory(dataSubjectCategory);
        }
        List<DataResponseDTO> dataResponseDTOS = dataList
                .stream().map(datum -> dataMapper.DataToDataResponseDTO(datum))
                .collect(Collectors.toList());
        return dataResponseDTOS;
    }

    @Override
    public int getIdByDataName(String dataName) {
        Data d = dataRepository.findByDataName(dataName);
        return d.getDataId();
    }

    @Override
    public String getDataNameById(int dataId) {
        Data d = dataRepository.findByDataId(dataId).get();
        return d.getDataName();
    }

    @Override
    public List<DataResponseDTO> findAllDataByDataSubjectCategory(int dataSubjectCategoryId) {
        List<Data> dataList = (List<Data>) dataRepository.findAllByDataSubjectCategoryId(dataSubjectCategoryId);
        List<DataResponseDTO> personalData = dataList.stream()
                .filter(dto -> dto.isPersonal())
                .map(dto -> dataMapper.DataToDataResponseDTO(dto))
                .collect(Collectors.toList());
        return personalData;
    }

    @Override
    public List<Data> findAllProcessedDataByDataSubjectCategoryAndId(int dataSubjectCategoryId, int dataSubjectId) {
        List<Data> dataList = (List<Data>) dataRepository.findAllByDataSubjectCategoryId(dataSubjectCategoryId);
        List<Integer> processedDataIds = processedDataRepository.findDataIdByDataSubjectId(dataSubjectId);
        List<Data> personalData = dataList.stream()
                .filter(dto -> dto.isPersonal() && processedDataIds.contains(dto.getDataId()))
                .collect(Collectors.toList());
        return personalData;
    }

    @Override
    public List<DataResponseDTO> getPersonalDataByDataTypeName(String dataTypeName) {
        List<DataResponseDTO> dataListByDataType = new LinkedList<>();
        List<DataResponseDTO> dataList = findAllPersonalData();
        for (DataResponseDTO datum : dataList) {
            if (datum.getDataTypeName().equals(dataTypeName))
                dataListByDataType.add(datum);
            System.out.println(datum.getDataTypeName());
        }
        for (DataResponseDTO datum : dataListByDataType) {
            System.out.println(datum.getDataTypeName());
        }
        return dataListByDataType;
    }

    @Override
    public List<ProcessedPersonalDataDTO> getProcessedPersonalDataList(String idRef) {
        ArrayList<ProcessedPersonalDataDTO> response = new ArrayList<>();
        DataSubjectResponseDTO dataSubject = actorRestClient.getDataSubjectByRef(idRef);
        int dSCategory = dataSubject.getDataSubjectCategoryId();
        int dataSubjectId = dataSubject.getDataSubjectId();

        ArrayList<Data> dataList = new ArrayList<>(this.findAllProcessedDataByDataSubjectCategoryAndId(dSCategory, dataSubjectId));

        // First, get all direct datas
        ArrayList<Data> directDatas = new ArrayList<>(dataList.stream().filter(d -> d.getSource().equals(Source.DIRECT)).collect(Collectors.toList()));

        directDatas.forEach(data -> {
            // Construct each dataType
            Optional<ProcessedPersonalDataDTO> processedPersonalDataDTO = response.stream().filter(p -> p.getDataTypeName().equals(data.getDataType().getDataTypeName())).findFirst();
            ProcessedPersonalDataDTO dataType = null;
            if (processedPersonalDataDTO.isPresent()) {
                dataType = processedPersonalDataDTO.get();
            } else {
                dataType = new ProcessedPersonalDataDTO(data.getDataType().getDataTypeName());
                response.add(dataType);
            }
            // Get data values
            ArrayList<String> datasNames = new ArrayList<>();
            datasNames.add(data.getDataName());
            ArrayList<Map<String, String>> valuesResponse = new ArrayList<>(providerRestClient.getPersonalDataValues(idRef, dataType.getDataTypeName(), datasNames));
            ArrayList<String> values = new ArrayList<>();
            valuesResponse.forEach(valueMap -> {
                if (valueMap.get("attribute").equals(data.getDataName()))
                    values.add(valueMap.get("value"));
            });
            dataType.addData(data.getDataId(), data.getDataName(), values, data.getDataConservationDuration(), data.getSource().name(), data.getSource().name(), data.getPersonalDataCategory().getPersonalDataCategoryName(), data.isPrimaryKey());

            // If the data was a primaryKey of the DataType, we add it to the primaryKey list
            if (data.isPrimaryKey()) {
                dataType.addPrimaryKey(data.getDataName());
            }
        });

        // Then the same thing, with the accepted undirect and produced datas
        ArrayList<Data> nondirectDatas = new ArrayList<>(dataList.stream().filter(d -> d.getSource().equals(Source.INDIRECT) || d.getSource().equals(Source.PRODUCED)).collect(Collectors.toList()));
        nondirectDatas.forEach(data -> {
            // We have to verify if provider accepted to give this data
            boolean isAccepted = rightRestClient.getIfDataAccessAccepted(dataSubjectId, data.getDataId());
            if(isAccepted) {
                // Construct each dataType
                Optional<ProcessedPersonalDataDTO> processedPersonalDataDTO = response.stream().filter(p -> p.getDataTypeName().equals(data.getDataType().getDataTypeName())).findFirst();
                ProcessedPersonalDataDTO dataType = null;
                if (processedPersonalDataDTO.isPresent()) {
                    dataType = processedPersonalDataDTO.get();
                } else {
                    dataType = new ProcessedPersonalDataDTO(data.getDataType().getDataTypeName());
                    response.add(dataType);
                }
                // Get data values
                ArrayList<String> datasNames = new ArrayList<>();
                datasNames.add(data.getDataName());
                ArrayList<Map<String, String>> valuesResponse = new ArrayList<>(providerRestClient.getPersonalDataValues(idRef, dataType.getDataTypeName(), datasNames));
                ArrayList<String> values = new ArrayList<>();
                valuesResponse.forEach(valueMap -> {
                    if (valueMap.get("attribute").equals(data.getDataName()))
                        values.add(valueMap.get("value"));
                });
                dataType.addData(data.getDataId(), data.getDataName(), values, data.getDataConservationDuration(), data.getSource().name(), data.getSource().name(), data.getPersonalDataCategory().getPersonalDataCategoryName(), data.isPrimaryKey());

                // If the data was a primaryKey of the DataType, we add it to the primaryKey list
                if (data.isPrimaryKey()) {
                    dataType.addPrimaryKey(data.getDataName());
                }
            }
        });
        return response;
    }

    @Override
    public List<ProcessedIndirectAndProducedPersonalDataDTO> getProcessedIndirectAndProducedPersonalDataList(String idRef) {
        ArrayList<ProcessedIndirectAndProducedPersonalDataDTO> response = new ArrayList<>();
        DataSubjectResponseDTO dataSubject = actorRestClient.getDataSubjectByRef(idRef);
        int dSCategory = dataSubject.getDataSubjectCategoryId();
        int dataSubjectId = dataSubject.getDataSubjectId();
        ArrayList<Data> dataList = new ArrayList<>(this.findAllProcessedDataByDataSubjectCategoryAndId(dSCategory, dataSubjectId));

        // Get indirect and produced datas
        ArrayList<Data> nondirectDatas = new ArrayList<>(dataList.stream().filter(d -> d.getSource().equals(Source.INDIRECT) || d.getSource().equals(Source.PRODUCED)).collect(Collectors.toList()));

        nondirectDatas.forEach(data -> {
            // Construct each dataType
            Optional<ProcessedIndirectAndProducedPersonalDataDTO> processedIndirectAndProducedPersonalDataDTO = response.stream().filter(p -> p.getDataTypeName().equals(data.getDataType().getDataTypeName())).findFirst();
            ProcessedIndirectAndProducedPersonalDataDTO dataType = null;
            if (processedIndirectAndProducedPersonalDataDTO.isPresent()) {
                dataType = processedIndirectAndProducedPersonalDataDTO.get();
            } else {
                dataType = new ProcessedIndirectAndProducedPersonalDataDTO(data.getDataType().getDataTypeName());
                response.add(dataType);
            }

            dataType.addData(data.getDataId(), data.getDataName());
        });

        return response;
    }

    @Override
    public List<DataListTransferDTO> getProcessedPersonalDataListTransfer(String idRef) {
        // Get the list of processed personal data
        List<ProcessedPersonalDataDTO> processedPersonalDataDTOList = getProcessedPersonalDataList(idRef);

        List<DataListTransferDTO> dataListTransferDTOList = new ArrayList<>();
        Map<Integer, List<SecondaryActor>> secondaryActorMap = new HashMap<>();
        // Iterate through each processed data
        for (ProcessedPersonalDataDTO processedPersonalDataDTO : processedPersonalDataDTOList) {
            // Fetch secondary actors for each data item in processed data
            for (ProcessedPersonalDataDTO.DataListItem dataListItem : processedPersonalDataDTO.getData()) {
                int dataId = dataListItem.getDataId();
                List<SecondaryActor> secondaryActors = secondaryActorRepository.findSecondaryActorsByDataId(dataId);
                secondaryActorMap.put(dataId, secondaryActors);
            }
        }

        for (Integer dataId : secondaryActorMap.keySet()) {
            List<SecondaryActor> secondaryActors = secondaryActorMap.get(dataId);
            for (SecondaryActor secondaryActor : secondaryActors) {
                if (!dataListTransferDTOList.contains(secondaryActor)) {
                    // Construct each dataListTransferDTO and add it to the list
                    DataListTransferDTO dataListTransferDTO = new DataListTransferDTO(
                            secondaryActor.getSecondaryActorId(),
                            secondaryActor.getSecondaryActorType(),
                            secondaryActor.getSecondaryActorName(),
                            secondaryActor.getSecondaryActorEmail(),
                            secondaryActor.getSecondaryActorPhone(),
                            secondaryActor.getSecondaryActorAddress(),
                            secondaryActor.getCountry(),
                            secondaryActor.getSafeguard(),
                            secondaryActor.getSafeguardType(),
                            new SecondaryActorCategoryDTO(
                                    secondaryActor.getSecondaryActorCategory().getSecondaryActorCategoryId(),
                                    secondaryActor.getSecondaryActorCategory().getSecondaryActorCategoryName()),
                            new ProcessedPersonalDataDTO() //TODO: check to be filled
                    );
                    // Retrieve the ProcessedPersonalDataDTO of the processedPersonalDataDTOList with the dataId
                    ProcessedPersonalDataDTO matchingProcessedData = processedPersonalDataDTOList.stream()
                            .filter(data -> data.getData().stream().anyMatch(item -> item.getDataId() == dataId))
                            .findFirst()
                            .orElse(null);

                    if (matchingProcessedData != null) {
                        dataListTransferDTO.setDataTransfers(matchingProcessedData);
                        dataListTransferDTOList.add(dataListTransferDTO);
                    }
                }
            }
        }
        return dataListTransferDTOList;
    }
}
