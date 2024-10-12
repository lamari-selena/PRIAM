package priam.data.priamdataservice.services;

import org.springframework.stereotype.Service;

import priam.data.priamdataservice.dto.*;
import priam.data.priamdataservice.entities.DataUsage;
import priam.data.priamdataservice.entities.Processing;
import priam.data.priamdataservice.mappers.ProcessingMapper;
import priam.data.priamdataservice.openfeign.ActorRestClient;
import priam.data.priamdataservice.repositories.DataUsageRepository;
import priam.data.priamdataservice.repositories.ProcessingRepository;

import javax.annotation.Generated;
import javax.transaction.Transactional;

import java.util.*;

import priam.data.priamdataservice.entities.Data;

@Generated(
        value = "org.mapstruct.ap.MappingProcessor",
        date = "2021-05-23T23:03:41+0530"
)
@Transactional
@Service
public class ProcessingService implements ProcessingServiceInterface {

    private ProcessingMapper processingMapper;
    private DataUsageService dataUsageService;
    private DataService dataService;
    private ProcessingRepository processingRepository;
    private DataUsageRepository dataUsageRepository;

    private ActorRestClient actorRestClient;

    public ProcessingService(ProcessingMapper processingMapper, DataUsageService dataUsageService, DataService dataService, ProcessingRepository processingRepository, DataUsageRepository dataUsageRepository, ActorRestClient actorRestClient) {
        this.processingMapper = processingMapper;
        this.dataUsageService = dataUsageService;
        this.dataService = dataService;
        this.processingRepository = processingRepository;
        this.dataUsageRepository = dataUsageRepository;
        this.actorRestClient = actorRestClient;
    }

    @Override
    public Processing createProcessing(ProcessingRequestDTO processingRequestDTO) {
        Processing processing = processingMapper.fromProcessingDTO(processingRequestDTO);
        Processing res = processingRepository.save(processing);
        res.getDataUsages().forEach(dataUsage -> {
            dataUsage.setProcessing(res);
            dataUsageRepository.save(dataUsage);
        });
        return res;
    }

    @Override
    public ProcessingResponseDTO updateProcessing(Integer processingId, ProcessingRequestDTO processingRequestDTO) {
        //log.info("UpdateProcessing start Process !");
        Processing processing = processingMapper.fromProcessingDTO(processingRequestDTO);
        Processing oldProcessing = processingRepository.findById(processingId).get();
        oldProcessing.setProcessingCategory(processing.getProcessingCategory());
        oldProcessing.setCreatedAt(processing.getCreatedAt());
        oldProcessing.setDataUsages(processing.getDataUsages());
        oldProcessing.setMeasures(processing.getMeasures());
        oldProcessing.setProcessingName(processing.getProcessingName());
        oldProcessing.setPurposes(processing.getPurposes());
        oldProcessing.setProcessingType(processing.getProcessingType());
        oldProcessing.setModifiedAt(new Date());
        processingRepository.save(oldProcessing);
        return processingMapper.fromProcessing(oldProcessing);
    }

    @Override
    public boolean deleteProcessing(Integer processingId) {
        processingRepository.deleteById(processingId);
        return true;
    }

    @Override
    public ProcessingResponseDTO getProcessing(Integer processingId) {
        Processing processing = processingRepository.findById(processingId).get();
        //processing.setDataUsages((List<DataUsage>)dataUsageService.getDataUsages(processingId));
        return processingMapper.fromProcessing(processing);
    }

    @Override
    public Collection<Processing> getProcessings() {
        Collection<Processing> processings = processingRepository.findAll();
        for (Processing processing : processings) {
            processing.setDataUsages((List<DataUsage>) dataUsageService.getDataUsages(processing.getProcessingId()));
            System.out.println(processing.getProcessingId());
            System.out.println(processing.getProcessingName());
        }
        return processings;
        //return processingRepository.findAll();
    }

    @Override
    public Collection<ProcessingResponseDTO> getProcessingsByDataSubjectCategoryId(int dataSubjectCategoryId) {
        Collection<Processing> processings = getProcessings();
        List<Integer> personalDataId = new LinkedList<>();
        Collection<ProcessingResponseDTO> processingsDsc = new LinkedList<>();

        ArrayList<DataResponseDTO> dataList = new ArrayList<>(dataService.findAllDataByDataSubjectCategory(dataSubjectCategoryId));
        for (DataResponseDTO data : dataList) {
            personalDataId.add(data.getDataId());
        }

        for (Processing processing : processings) {
            int cpt = 0;
            for (DataUsage dataUsage : processing.getDataUsages()) {
                if (personalDataId.contains(dataUsage.getDataId())) {
                    cpt++;
                    break;
                }
            }

            if (cpt != 0) {
                processingsDsc.add(processingMapper.fromProcessing(processing));
            }
        }
        return processingsDsc;
    }

    @Override
    public List<ProcessingPersonalDataDTO> getProcessingPersonalDataListPurposes(String idRef) {
        // Retrieve the DataSubject to have its category
        DataSubjectResponseDTO dataSubject = actorRestClient.getDataSubjectByRef(idRef);

        // Retrieve associated processings and datas
        Collection<ProcessingResponseDTO> processings = this.getProcessingsByDataSubjectCategoryId(dataSubject.getDataSubjectCategoryId());

        ArrayList<ProcessingPersonalDataDTO> response = new ArrayList<>();
        for (ProcessingResponseDTO processing : processings) {
            ProcessingPersonalDataDTO p = new ProcessingPersonalDataDTO();
            p.setProcessingName(processing.getProcessingName());
            // Purposes
            processing.getPurposes().forEach(purpose -> {
                p.addPurposeDescription(purpose.getPurposeDescription());
            });
            // Datas
            processing.getDataUsages().forEach(dataUsage -> {
                p.addDataName(dataService.getDataNameById(dataUsage.getDataId()));
            });

            response.add(p);
        }
        return response;
    }


}
