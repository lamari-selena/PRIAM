package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.right.entities.Data;

import java.util.ArrayList;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessRequestRequestDTO {
    private int dataSubjectId;
    private String dataRequestClaim;
    private ArrayList<Data> data;
}
