package priam.data.priamdataservice.services;

import org.springframework.stereotype.Service;
import priam.data.priamdataservice.entities.ProcessedData;
import priam.data.priamdataservice.openfeign.ActorRestClient;
import priam.data.priamdataservice.repositories.ProcessedDataRepository;

import priam.data.priamdataservice.dto.*;
import javax.transaction.Transactional;
import java.util.List;

import priam.data.priamdataservice.dto.transfer.*;
@Service
@Transactional
public class ProcessedDataService implements ProcessedDataServiceInterface{

    private final ProcessedDataRepository processedDataRepository;
    private final ActorRestClient actorRestClient;

    public ProcessedDataService(ProcessedDataRepository processedDataRepository, ActorRestClient actorRestClient) {
        this.processedDataRepository = processedDataRepository;
        this.actorRestClient = actorRestClient;
    }

    @Override
    public void addProcessedData(List<Integer> dataIds, int datasubjectId) {
        ProcessedData processedData;
        try {
            // Appel au microservice pour vérifier l'existence du subject
            actorRestClient.getDataSubjectId(datasubjectId);


            for (int dataId : dataIds) {
                boolean exists = processedDataRepository.existsByDataIdAndDataSubjectId(dataId, datasubjectId);
                if (!exists)
                {
                     processedData = new ProcessedData(dataId, datasubjectId,1);
                } else {
                    processedData = processedDataRepository.findByDataIdAndDataSubjectId(dataId, datasubjectId);
                    processedData.setNbOccurrences(processedData.getNbOccurrences() + 1);
                }
                processedDataRepository.save(processedData);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error adding data to processed data table");
        }
    }

    @Override
    public void removeProcessedData(List<Integer> dataIds, int dataSubjectId) {
        ProcessedData processedData = null;
        try {
            // Vérification que le subject existe via Feign
            actorRestClient.getDataSubjectId(dataSubjectId);
            for (int dataId : dataIds) {
                if (processedDataRepository.existsByDataIdAndDataSubjectId(dataId, dataSubjectId)) {
                    processedData = processedDataRepository.findByDataIdAndDataSubjectId(dataId, dataSubjectId);
                }
                assert processedData != null;
                if (processedData.getNbOccurrences() == 1)
                    processedDataRepository.deleteByDataIdAndDataSubjectId(dataId, dataSubjectId);
                else
                    processedData.setNbOccurrences(processedData.getNbOccurrences() - 1);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Subject not found with ID: " + dataSubjectId);
        }
    }
}
