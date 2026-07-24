package priam.data.priamdataservice.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "gateway", contextId = "providerClient")
//@FeignClient(name = "PROVIDER-SERVICE")
public interface ProviderRestClient {
    //{/provider} prefix
    // `attributes` is a single comma-joined query param (Docs/PRIAM-INTEGRATION-PLAYBOOK.md
    // §2/§7ter) — OpenFeign's default List<String> encoding sends repeated params
    // instead, which a target app parsing `attributes` as one value would silently
    // truncate to the last one. Callers must join with "," before passing it in.
    @GetMapping(path = "/provider/api/dataAccessRight")
    List<Map<String, String>> getPersonalDataValues(@RequestParam String idRef, @RequestParam String dataTypeName, @RequestParam String attributes);
}
