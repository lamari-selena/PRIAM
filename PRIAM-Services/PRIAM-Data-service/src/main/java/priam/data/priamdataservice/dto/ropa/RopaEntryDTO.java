package priam.data.priamdataservice.dto.ropa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.enums.ProcessingCategory;
import priam.data.priamdataservice.enums.ProcessingType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * One row of the Record of Processing Activities (ROPA), as required by GDPR Art. 30.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RopaEntryDTO {

    private int processingId;
    private String processingName;
    private ProcessingType processingType;
    private ProcessingCategory processingCategory;
    private Date createdAt;
    private Date modifiedAt;

    private List<PurposeInfo> purposes = new ArrayList<>();
    private List<DataInfo> personalData = new ArrayList<>();
    private List<MeasureInfo> securityMeasures = new ArrayList<>();
    private List<TransferInfo> transfers = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurposeInfo {
        private String description;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataInfo {
        private String dataName;
        private String source;
        private int retentionDays;
        private boolean isPersonal;
        private boolean isPortable;
        private String personalDataCategory;
        private boolean crud_create;
        private boolean crud_read;
        private boolean crud_update;
        private boolean crud_delete;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeasureInfo {
        private String description;
        private String type;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferInfo {
        private int transferId;
        private List<String> dataNames;
    }
}
