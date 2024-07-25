package priam.data.priamdataservice.dto.consent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRequestDTO {
    private Long dataSubjectId;
    private Long processingId;
}
