package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingPersonalDataDTO {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class PurposeListItem {
        private String purposeDescription;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DataListItem {
        private String dataName;
    }
    private String processingName;
    private List<PurposeListItem> purposes = new ArrayList<>();
    private List<DataListItem> data = new ArrayList<>();

    public void addPurposeDescription(String purposeDescription) {
        this.purposes.add(new PurposeListItem(purposeDescription));
    }
    public void addDataName(String dataName) {
        this.data.add(new DataListItem(dataName));
    }
}
