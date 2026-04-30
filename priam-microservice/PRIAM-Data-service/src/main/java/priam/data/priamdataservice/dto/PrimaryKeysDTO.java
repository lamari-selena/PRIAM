package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrimaryKeysDTO {
    private List<PrimaryKeysListItem> primaryKeys;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class PrimaryKeysListItem {
        private String primaryKeyName;
        private String primaryKeyValue;
    }
}
