package priam.data.priamdataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.enums.Source;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedPersonalDataDTO {
    private String dataTypeName;
    private List<PrimaryKeysListItem> primaryKeys = new ArrayList<>();
    private List<DataListItem> data = new ArrayList<>();

    public ProcessedPersonalDataDTO(String dataTypeName) {
        this.setDataTypeName(dataTypeName);
        this.setPrimaryKeys(new ArrayList<>());
    }

    public void addPrimaryKey(String primaryKeyName) {
        this.getPrimaryKeys().add(new PrimaryKeysListItem(primaryKeyName));
    }

    public void addData(int dataId, String dataName, List<String> dataValues, int dataConservationDuration, String sourceName, String sourceDetails, String personalDataCategory, boolean isPrimaryKey) {
        DataListItem data = new DataListItem();
        data.setDataId(dataId);
        data.setDataName(dataName);
        data.setDataValue(dataValues);
        data.setSourceName(sourceName);
        data.setSourceDetails(sourceDetails);
        data.setDataConservationDuration(dataConservationDuration);
        data.setPersonalDataCategory(personalDataCategory);
        data.setPrimaryKey(isPrimaryKey);

        this.getData().add(data);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class PrimaryKeysListItem {
        private String primaryKeyName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataListItem {
        private int dataId;
        private String dataName;
        private List<String> dataValue;
        private int dataConservationDuration;
        private String sourceName;
        private String sourceDetails;
        private String personalDataCategory;
        @JsonProperty(value = "isPrimaryKey") // If not here, it removes the 'is' part
        private boolean isPrimaryKey;
    }
}
