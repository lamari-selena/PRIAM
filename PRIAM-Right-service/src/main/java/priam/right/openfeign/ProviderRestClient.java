package priam.right.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import priam.right.dto.ErasureRequestDTO;
import priam.right.dto.RectificationRequestDTO;

import java.util.List;
import java.util.Map;

@FeignClient(name = "gateway", contextId = "providerClient")//@FeignClient(name = "PROVIDER-SERVICE")
public interface ProviderRestClient {

    //{/provider}  prefixe
    @PostMapping(path = "/provider/api/rectification")
    void rectification(@RequestBody RectificationRequestDTO rectificationRequestDTO);

    @PostMapping(path = "/provider/api/erasure")
    void erasure(@RequestBody ErasureRequestDTO erasureRequestDTO);

    @GetMapping(path = "/provider/api/dataAccessRight")
    List<Map<String, String>> getPersonalDataValues(@RequestParam int idRef, @RequestParam String dataTypeName, @RequestParam List<String> attributes);
}
