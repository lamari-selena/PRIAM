package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class DataRequestRequestDTO {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DataItem {
        private int dataId;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class PrimaryKeyItems {
        private int primaryKeyId; // DataId
        private String primaryKeyValue;
    }

    private int dataSubjectId;
    private String dataTypeName;
    private DataItem data;
    private String newValue;
    private String claim;

    private List<PrimaryKeyItems> primaryKeys = new ArrayList<>();

    public HashMap<Integer, String> getPrimaryKeys() {
        HashMap<Integer, String> map = new HashMap<>();
        primaryKeys.forEach(p -> {
            map.put(p.primaryKeyId, p.primaryKeyValue);
        });
        return map;
    }
    public int getDataId() {
        return data.dataId;
    }
}
