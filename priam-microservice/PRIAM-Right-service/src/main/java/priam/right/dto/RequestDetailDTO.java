package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.right.enums.DataRequestType;

import java.util.*;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDetailDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DataListItem {
        private int dataId;
        private String dataName;
        private boolean answerByData;
        private Map<String, String> primaryKeys;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class DataTypeListItem {
        private String dataTypeName;
        private List<DataListItem> data = new ArrayList<>();
        private void addData(int dataId, String attributeName, boolean answerByData, Map<String, String> primaryKeys) {
            this.data.add(new DataListItem(dataId, attributeName, answerByData, primaryKeys));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataSubject {
        private int dataSubjectId;
        private String idRef;
        private String dataSubjectCategoryName;
    }

    private int dataRequestId;
    private DataRequestType dataRequestType;
    private String dataRequestClaim;
    private String newValue;
    private Date dataRequestIssuedAt;
    private boolean response;
    private boolean isIsolated;
    private DataSubject dataSubject;
    private List<DataTypeListItem> dataTypeList = new ArrayList<>();

    public void setDataSubject(int id, String idRef, String dataSubjectCategoryName) {
        this.dataSubject = new DataSubject(id, idRef, dataSubjectCategoryName);
    }
    public void addData(String dataTypeName, int dataId, String attributeName, boolean answerByData, Map<String, String> primaryKeys) {
        Optional<DataTypeListItem> dataType = this.dataTypeList.stream().filter(dataTypeListItem -> dataTypeListItem.dataTypeName.equals(dataTypeName)).findFirst();
        if(dataType.isPresent()) {
            dataType.get().addData(dataId, attributeName, answerByData, primaryKeys);
        }
        else {
            DataTypeListItem dt = new DataTypeListItem();
            dt.setDataTypeName(dataTypeName);
            dt.addData(dataId, attributeName, answerByData, primaryKeys);
            this.dataTypeList.add(dt);
        }
    }
}
