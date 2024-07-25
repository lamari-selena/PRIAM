package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import priam.right.entities.Data;

import java.util.ArrayList;
import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestAnswerRequestDTO {
    private int requestAnswerId;
    @Getter
    private boolean answer;
    private String providerClaim;

    private int dataRequestId;

    private List<Data> data = new ArrayList<>();
}
