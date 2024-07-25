package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSubjectCategory {
    private int dataSubjectCategoryId;
    private String dataSubjectCategoryName;
    private String locationId;
}
