package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.right.enums.DataRequestType;
import java.util.Date;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestListDTO {
    private int dataRequestId;
    private DataRequestType dataRequestType;
    private Date dataRequestIssuedAt;
    private boolean response;
    private DataSubjectCategoryResponseDTO dataSubjectCategory;
}
