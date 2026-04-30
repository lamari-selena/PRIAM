package org.provider_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetDataValueDTO {
    private String idRef;
    private String dataName;
    private Map<String, String> primaryKeys = new HashMap<>();
}
