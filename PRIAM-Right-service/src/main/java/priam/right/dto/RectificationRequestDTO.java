package priam.right.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RectificationRequestDTO {
    String idRef;
    String dataName;
    String dataTypeName;
    String newValue;
    Map<String, String> primaryKeys = new HashMap<>();
}
