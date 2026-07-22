package priam.actor.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class DataSubjectCategoryResponseDTO {
    private int dataSubjectCategoryId;
    private String dataSubjectCategoryName;
    private String locationId;
}