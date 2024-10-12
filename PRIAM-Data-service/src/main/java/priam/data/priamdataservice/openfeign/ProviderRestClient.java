package priam.data.priamdataservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "PROVIDER-SERVICE")
public interface ProviderRestClient {
    @GetMapping(path = "/api/dataAccessRight")
    List<Map<String, String>> getPersonalDataValues(@RequestParam String idRef, @RequestParam String dataTypeName, @RequestParam List<String> attributes);
}
