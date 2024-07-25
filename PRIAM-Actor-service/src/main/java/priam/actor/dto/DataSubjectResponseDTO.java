package priam.actor.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.actor.entities.DataSubjectCategory;

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
