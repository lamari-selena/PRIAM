package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequestDTO {
    private String dataSubjectEmail;
    private String appProviderEmail;
    private String message;
    private String requestType;
    private String status;
}
