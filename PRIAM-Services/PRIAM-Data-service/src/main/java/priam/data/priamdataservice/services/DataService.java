package priam.data.priamdataservice.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import priam.data.priamdataservice.dto.DataRequestDTO;
import priam.data.priamdataservice.dto.DataResponseDTO;
import priam.data.priamdataservice.dto.DataSubjectResponseDTO;
import priam.data.priamdataservice.dto.DataTypeResponseDTO;
import priam.data.priamdataservice.dto.ProcessedIndirectAndProducedPersonalDataDTO;
import priam.data.priamdataservice.dto.ProcessedPersonalDataDTO;
import priam.data.priamdataservice.dto.transfer.DataListTransferDTO;
import priam.data.priamdataservice.dto.transfer.SecondaryActorCategoryDTO;
import priam.data.priamdataservice.entities.DataSubjectCategory;
import priam.data.priamdataservice.entities.Data;
import priam.data.priamdataservice.entities.model.SecondaryActor;
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
        DataSubjectCategory dataSubjectCategory;
        for (Data datum : dataList) {
            try{
                dataSubjectCategory = actorRestClient.getDataSubjectCategoryById(datum.getDataSubjectCategory().getDataSubjectCategoryId());
                datum.setDataSubjectCategory(dataSubjectCategory);
            }catch (Exception e) {
                System.out.println("Error : recup data subject category !");
            }
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
            try {
                DataSubjectCategory dataSubjectCategory = actorRestClient.getDataSubjectCategoryById(datum.getDataSubjectCategory().getDataSubjectCategoryId());
            }catch (Exception e) {
                System.out.println("Error : recup data subject category !");
            }

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

        // First, get all direct datas - grouped by dataType so every column of the
        // same dataType is requested in a single Provider call
        // (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §8.2.e). Calling once per column and
        // recombining by array position relied on the target app returning
        // identically-ordered results across separate calls, which isn't guaranteed
        // and could silently mix fields from different records.
        ArrayList<Data> directDatas = new ArrayList<>(dataList.stream().filter(d -> d.getSource().equals(Source.DIRECT)).collect(Collectors.toList()));
        Map<String, List<Data>> directByDataType = directDatas.stream()
                .collect(Collectors.groupingBy(d -> d.getDataType().getDataTypeName(), LinkedHashMap::new, Collectors.toList()));
        directByDataType.forEach((dataTypeName, datas) ->
                addDataValuesForDataType(response, idRef, dataTypeName, datas));

        // Then the same thing, with the accepted indirect and produced datas - the
        // acceptance check stays per-column (each data_id needs its own right
        // granted), but the Provider call is still batched per dataType afterward.
        ArrayList<Data> nondirectDatas = new ArrayList<>(dataList.stream().filter(d -> d.getSource().equals(Source.INDIRECT) || d.getSource().equals(Source.PRODUCED)).collect(Collectors.toList()));
        List<Data> acceptedNondirectDatas = nondirectDatas.stream()
                .filter(data -> rightRestClient.isDataRequestAcceptedForDataId(dataSubjectId, data.getDataId()))
                .collect(Collectors.toList());
        Map<String, List<Data>> nondirectByDataType = acceptedNondirectDatas.stream()
                .collect(Collectors.groupingBy(d -> d.getDataType().getDataTypeName(), LinkedHashMap::new, Collectors.toList()));
        nondirectByDataType.forEach((dataTypeName, datas) ->
                addDataValuesForDataType(response, idRef, dataTypeName, datas));

        return response;
    }

    /**
     * One Provider call per dataType, all requested attribute names joined -
     * Provider bridge contract (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2, verified
     * against PRIAM-Right-service and real target apps): each map is keyed
     * directly by attribute name (e.g. {"first_name": "Jane"}), not by a generic
     * {"attribute":..., "value":...} pair. The same response list is reused for
     * every column of this dataType, so field alignment across columns no longer
     * depends on the target app returning identically-ordered results between
     * separate calls (§8.2.e).
     */
    private void addDataValuesForDataType(ArrayList<ProcessedPersonalDataDTO> response, String idRef, String dataTypeName, List<Data> datas) {
        ProcessedPersonalDataDTO dataType = response.stream()
                .filter(p -> p.getDataTypeName().equals(dataTypeName))
                .findFirst()
                .orElseGet(() -> {
                    ProcessedPersonalDataDTO newDataType = new ProcessedPersonalDataDTO(dataTypeName);
                    response.add(newDataType);
                    return newDataType;
                });

        List<String> datasNames = datas.stream().map(Data::getDataName).collect(Collectors.toList());
        List<Map<String, String>> valuesResponse = providerRestClient.getPersonalDataValues(idRef, dataTypeName, String.join(",", datasNames));

        datas.forEach(data -> {
            ArrayList<String> values = new ArrayList<>();
            valuesResponse.forEach(valueMap -> {
                String value = valueMap.get(data.getDataName());
                if (value != null)
                    values.add(value);
            });
            dataType.addData(data.getDataId(), data.getDataName(), values, data.getDataConservationDuration(), data.getSource().name(), data.getSource().name(), data.getPersonalDataCategory().getPersonalDataCategoryName(), data.isPrimaryKey());

            if (data.isPrimaryKey()) {
                dataType.addPrimaryKey(data.getDataName());
            }
        });
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
                List<SecondaryActor> secondaryActors = actorRestClient.getSecondaryActorsByDataId(dataId);
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
