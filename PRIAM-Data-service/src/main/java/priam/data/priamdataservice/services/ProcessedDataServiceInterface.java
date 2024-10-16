package priam.data.priamdataservice.services;

import java.util.List;

public interface ProcessedDataServiceInterface {
    void addProcessedData(List<Integer> dataIds, int datasubjectId);
    void removeProcessedData(List<Integer> dataIds, int datasubjectId);
}
