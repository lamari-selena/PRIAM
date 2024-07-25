package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErasureRequestDTO {
    String idRef;
    String dataName;
    String dataTypeName;
    Map<String, String> primaryKeys = new HashMap<>();
}
