package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.right.entities.DataRequest;

import java.util.Date;
@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestAnswerResponseDTO {
    private int idAnswer;
    private boolean answer;
    private String claimAnswer;
    private Date claimDate;

    private DataRequest dataRequest;
}
