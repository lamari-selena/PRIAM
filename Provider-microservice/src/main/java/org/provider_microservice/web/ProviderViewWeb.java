package org.provider_microservice.web;

import org.provider_microservice.dto.ErasureRequestDTO;
import org.provider_microservice.dto.GetDataValueDTO;
import org.provider_microservice.dto.RectificationRequestDTO;
import org.provider_microservice.services.ProviderViewService;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = "/api")
public class ProviderViewWeb {
    private final ProviderViewService providerViewService;

    public ProviderViewWeb(ProviderViewService providerViewService) {
        this.providerViewService = providerViewService;
    }

    @GetMapping(path = "/dataAccessRight")
    public List<Map<String, String>> getPersonalDataValues(@RequestParam String idRef, @RequestParam String dataTypeName,
                                                        @RequestParam List<String> attributes) throws SQLException {
        return providerViewService.getPersonalDataValues(idRef, dataTypeName, attributes, null); // TODO: Pouvoir récupérer un map dans les paramètres de la requête
    }
    @PostMapping(path = "/dataValue")
    public Map<String, String> getPersonalDataValue(@RequestBody GetDataValueDTO getDataValueDTO) throws SQLException {
        return providerViewService.getDataValue(getDataValueDTO.getIdRef(), getDataValueDTO.getDataName(), getDataValueDTO.getPrimaryKeys());
    }

    @PostMapping(path = "/rectification")
    public void rectification(@RequestBody RectificationRequestDTO rectificationRequestDTO) throws SQLException {
        String dataName = rectificationRequestDTO.getDataName();
        String newValue = rectificationRequestDTO.getNewValue();
        String idRef = rectificationRequestDTO.getIdRef();
        String dataTypeName = rectificationRequestDTO.getDataTypeName();
        Map<String, String> primaryKeys = rectificationRequestDTO.getPrimaryKeys();
        providerViewService.Rectification(dataName, newValue, idRef, dataTypeName, primaryKeys);
    }

    @PostMapping(path = "/erasure")
    public void erasure(@RequestBody ErasureRequestDTO erasureRequestDTO) throws SQLException {
        String dataName = erasureRequestDTO.getDataName();
        String idRef = erasureRequestDTO.getIdRef();
        String dataTypeName = erasureRequestDTO.getDataTypeName();
        Map<String, String> primaryKeys = erasureRequestDTO.getPrimaryKeys();
        providerViewService.Erasure(dataName, idRef, dataTypeName, primaryKeys);

    }
}

