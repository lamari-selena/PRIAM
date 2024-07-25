package priam.data.priamdataservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class DataSubjectResponseDTO {
    private int dataSubjectId;
    private int age;
    private String idRef;
    private String username;
    private String password;
    private int dataSubjectCategoryId;
    private String dataSubjectCategoryName;
}
