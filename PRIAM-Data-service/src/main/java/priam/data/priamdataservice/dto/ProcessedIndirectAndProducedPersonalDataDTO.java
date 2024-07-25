package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedIndirectAndProducedPersonalDataDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DataListItem {
        private int dataId;
        private String dataName;
    }
    private String dataTypeName;
    private List<DataListItem> data = new ArrayList<>();


    public ProcessedIndirectAndProducedPersonalDataDTO(String dataTypeName) {
        this.setDataTypeName(dataTypeName);
    }
    public void addData(int dataId, String dataName) {
        DataListItem data = new DataListItem();
        data.setDataId(dataId);
        data.setDataName(dataName);

        this.getData().add(data);
    }
}
